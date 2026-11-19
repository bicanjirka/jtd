package td.effect;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatAccumulator;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The effects active on one enemy, at most one per {@link EffectKind}. Reapplying a kind keeps the
 * stronger one and the longer remaining duration, so a weak top-up never cuts short a strong
 * effect, and of two slows of one kind the stronger holds while it runs. The pools, the shared counts and the kinds in
 * shares combine differently: see {@link #applyPool}, {@link #applyCount} and {@link Effect#joinedBy}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ActiveEffects {

    /** A pool deals what it has burned in a pulse this often: four times a second. */
    private static final int POOL_PULSE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 4f);
    /** A pool of this much fuel, in points, burns out in {@link #REFERENCE_BURN_SECONDS}. */
    static final float REFERENCE_FUEL_POINTS = 100f;
    static final float REFERENCE_BURN_SECONDS = 1.75f;
    /**
     * A pool burns this times the square root of its fuel a tick, in damage units: the root falls by half of it every
     * tick, so the pool empties in a time that grows with the root of what it held.
     */
    private static final float BURN_RATE = (float) (2 * Math.sqrt(DamageUnits.ofPoints(REFERENCE_FUEL_POINTS))
            / (REFERENCE_BURN_SECONDS * TickRate.TICKS_PER_SECOND));
    /** A pool that would burn longer than this reads as full. */
    private static final float FULL_BURN_TICKS = 2 * REFERENCE_BURN_SECONDS * TickRate.TICKS_PER_SECOND;
    /** Less fuel than this is burned at once, so a pool never ends on a crumb it would take ticks to burn. */
    private static final float LAST_UNIT = 1f;

    /** Speed comes back over this last stretch of a slow's own time. */
    public static final int SLOW_FADE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    private static final List<EffectKind> POOLS = Arrays.stream(EffectKind.values()).filter(EffectKind::isFuelPool).toList();

    private final Map<EffectKind, Effect> active = new EnumMap<>(EffectKind.class);
    /** Per kind, progress that is not yet a whole step: towards a shared count's next lost stack, or a bleed's next unit. */
    private final float[] progress = new float[EffectKind.values().length];
    /**
     * Per kind, the hold given since the last tick: the ticks a stack takes, 0 for none, the stacks it builds to, and the
     * fade step it leaves the count with.
     */
    private final int[] holdTicks = new int[EffectKind.values().length];
    private final int[] holdCap = new int[EffectKind.values().length];
    private final int[] holdFade = new int[EffectKind.values().length];
    /** Per kind, whether a holder held it on the last tick, so it loses nothing. */
    private final boolean[] heldNow = new boolean[EffectKind.values().length];
    /** Per kind, progress toward the next stack a hold builds. */
    private final float[] holdProgress = new float[EffectKind.values().length];
    private boolean held;
    private boolean heldBefore;

    private static Effect strongerOf(Effect a, Effect b) {
        Effect stronger = magnitude(a) >= magnitude(b) ? a : b;
        return stronger.withRemainingTicks(Math.max(a.remainingTicks(), b.remainingTicks()));
    }

    /** Of two slows of one kind, the stronger with its own time, so a weak long one can't stretch it; of equals, the longer. */
    private static Effect strongerSlowOf(Effect held, Effect incoming) {
        if (held.slowStrength() != incoming.slowStrength()) {
            return held.slowStrength() > incoming.slowStrength() ? held : incoming;
        }
        return held.remainingTicks() >= incoming.remainingTicks() ? held : incoming;
    }

    /** How hard an effect bites, in a unit specific to its family; only compared within one kind. */
    private static float magnitude(Effect effect) {
        return switch (effect.dynamics()) {
            // On/off, not gradated - any reapplication is at least as strong as what's already active.
            case EffectDynamics.Flag _, EffectDynamics.Encasing _ -> 1f;
            case EffectDynamics.Stacks stacks -> stacks.stacks();
            case EffectDynamics.StackShares shares -> shares.stacks();
            case EffectDynamics.Slow slow -> slow.strength();
            case EffectDynamics.FuelPool _ -> effect.fuelLevel();
            case EffectDynamics.HealOverTime heal -> heal.perTick();
            case EffectDynamics.ShieldPool shield -> shield.remaining();
            case EffectDynamics.Distance distance -> distance.perCell().amount();
            case EffectDynamics.GrowingSlow _ -> slowNow(effect);
        };
    }

    /**
     * Applies {@code effect} unless an active effect keeps its kind out (see {@link EffectInteractions}).
     */
    public void apply(Effect effect) {
        if (EffectInteractions.blocks(this.active.keySet(), effect.kind())) {
            return;
        }
        Effect incoming = effect;
        switch (incoming.kind().rule().stacking()) {
            case POOL -> this.applyPool(incoming);
            case COUNT -> this.applyCount(incoming);
            case SHARES -> {
                Effect existing = this.active.get(incoming.kind());
                this.active.put(incoming.kind(), existing == null ? incoming : existing.joinedBy(incoming));
            }
            case TIMED -> {
                Effect existing = this.active.get(incoming.kind());
                boolean holding = existing != null && incoming.kind().isNotRenewed();
                this.active.put(incoming.kind(), existing == null ? incoming
                        : holding ? existing
                        : incoming.slowStrength() > 0f ? strongerSlowOf(existing, incoming)
                        : strongerOf(existing, incoming));
            }
        }
    }

    /** The share of speed {@code effect} takes away now: its strength, fading over the end of its own time. */
    private static float slowNow(Effect effect) {
        return switch (effect.dynamics()) {
            case EffectDynamics.GrowingSlow growing -> effect.grownSlow();
            default -> fadingSlowNow(effect);
        };
    }

    private static float fadingSlowNow(Effect effect) {
        float strength = effect.slowStrength();
        if (strength <= 0f) {
            return 0f;
        }
        int fadeTicks = Math.clamp(effect.authoredDurationTicks(), 1, SLOW_FADE_TICKS);
        return strength * Math.min(1f, (float) effect.remainingTicks() / fadeTicks);
    }

    /**
     * A pool (burn, poison or Soulfire, each its own) takes all the fuel it is given, whoever gives it, with no cap.
     * Fuel from a source already feeding it joins that source's share, so every source is credited for its own and a
     * stream fed every tick stays one share. A pool that starts earns its first stack at once, unless another pool
     * already holds that mark, and holds it while it lasts, building a stack every {@code STACK_INTERVAL_TICKS}.
     * Feeding it earns none, and the fade step of the marks it holds is its last application's.
     */
    private void applyPool(Effect incoming) {
        Effect existing = this.active.get(incoming.kind());
        if (existing == null) {
            this.active.put(incoming.kind(), incoming);
            for (EffectKind debuff : EffectInteractions.earnedBy(incoming.kind())) {
                if (!this.isHeldByAnotherPool(debuff, incoming.kind())) {
                    this.addHeldStack(debuff, incoming.fadeTicks());
                }
            }
            return;
        }
        List<FuelContribution> fuel = new ArrayList<>(existing.fuel());
        for (FuelContribution more : incoming.fuel()) {
            int own = more.origin() == AttackOrigin.none() ? -1 : indexOfOrigin(fuel, more.origin());
            if (own >= 0) {
                fuel.set(own, fuel.get(own).fedBy(more.fuel()));
            } else {
                fuel.add(more);
            }
        }
        this.active.put(incoming.kind(), existing.withFuel(fuel).withFuelFade(incoming.fadeTicks()));
    }

    private static int indexOfOrigin(List<FuelContribution> fuel, AttackOrigin origin) {
        for (int i = 0; i < fuel.size(); i++) {
            if (fuel.get(i).origin().equals(origin)) {
                return i;
            }
        }
        return -1;
    }

    /** The ticks a pool of {@code fuel} damage units takes to burn out, if nothing feeds it. */
    public static float burnTicksOf(float fuel) {
        return fuel <= 0f ? 0f : (float) (2 * Math.sqrt(fuel) / BURN_RATE);
    }

    private boolean isHeldByAnotherPool(EffectKind debuff, EffectKind pool) {
        for (EffectKind other : POOLS) {
            if (other != pool && this.active.containsKey(other) && EffectInteractions.earnedBy(other).contains(debuff)) {
                return true;
            }
        }
        return false;
    }

    /** One more stack of the shared count {@code kind}, with no hold of its own, losing a stack every {@code fadeTicks}. */
    private void addHeldStack(EffectKind kind, int fadeTicks) {
        Effect existing = this.active.get(kind);
        int stacks = Math.min(kind.maxStacks(), (existing == null ? 0 : existing.stacks()) + 1);
        Effect count = existing == null ? Effect.counting(kind, stacks).withStacks(stacks, 0)
                : existing.withStacks(stacks, existing.remainingTicks());
        this.active.put(kind, count.withFadeTicks(fadeTicks));
    }

    /**
     * A shared count: an application adds its stacks up to the kind's cap, whichever tower applied them, and holds the
     * count for its own time, never shortening a hold already running; the fade step is the last application's. A
     * holder adds nothing: it raises the count to its own level, never past the cap, and holds it the same way.
     */
    private void applyCount(Effect incoming) {
        EffectKind kind = incoming.kind();
        Effect existing = this.active.get(kind);
        int before = existing == null ? 0 : existing.stacks();
        int stacks = incoming.isHolder() ? Math.max(before, Math.min(kind.maxStacks(), incoming.stacks()))
                : Math.min(kind.maxStacks(), before + incoming.stacks());
        if (stacks <= 0) {
            return;
        }
        int remaining = Math.max(existing == null ? 0 : existing.remainingTicks(), incoming.remainingTicks());
        this.progress[kind.ordinal()] = 0f;
        this.active.put(kind, incoming.withStacks(stacks, remaining));
    }

    /** Takes {@code fraction} of what every shield has left; a shield that is gone stays gone. */
    public void weakenShield(float fraction) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield != null) {
            this.putShield(shield, shield.shieldPool().weakened(fraction));
        }
    }

    /**
     * Lets the shields that take {@code type} hold what they can of a hit of {@code raw} units, the one ending
     * soonest first, each worth {@code worth} times what it has left, and returns the units they held. A shield with
     * less than a unit left breaks, and at no worth every shield the hit meets breaks and holds nothing.
     */
    public int absorb(DamageType type, int raw, float worth) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield == null || raw <= 0) {
            return 0;
        }
        EffectDynamics.ShieldHit hit = shield.shieldPool().hitBy(type, raw, worth);
        this.putShield(shield, hit.left());
        return hit.absorbed();
    }

    private void putShield(Effect shield, Optional<EffectDynamics.ShieldPool> left) {
        if (left.isPresent()) {
            this.active.put(EffectKind.SHIELD, shield.withShield(left.get()));
        } else {
            this.active.remove(EffectKind.SHIELD);
        }
    }

    /** The active shield's pool, in points; {@link ShieldGauge#none()} without one. */
    public ShieldGauge shieldGauge() {
        Effect shield = this.active.get(EffectKind.SHIELD);
        return shield == null ? ShieldGauge.none()
                : new ShieldGauge(DamageUnits.inPoints(shield.shieldRemaining()), DamageUnits.inPoints(shield.shieldMax()));
    }

    public boolean has(EffectKind kind) {
        return this.active.containsKey(kind);
    }

    /** Every active effect, in kind order: a copy, so a caller may pass them on. */
    public List<Effect> effects() {
        return List.copyOf(this.active.values());
    }

    /** The active effect of {@code kind}; empty when it isn't active. */
    public Optional<Effect> find(EffectKind kind) {
        return Optional.ofNullable(this.active.get(kind));
    }

    /**
     * Holds the shared count {@code kind} through the next tick: it builds a stack every {@code ticksPerStack} up to
     * {@code level}, never past the kind's cap, and loses none; once nothing holds it, it loses a stack every
     * {@code fadeTicks}. Of several holds in one tick the quickest pace, the highest level and the longest fade step
     * count, so holds never add up; stacks past the level are kept, not built.
     */
    public void hold(EffectKind kind, int ticksPerStack, int level, int fadeTicks) {
        int slot = kind.ordinal();
        int pace = Math.max(1, ticksPerStack);
        this.holdTicks[slot] = this.holdTicks[slot] == 0 ? pace : Math.min(this.holdTicks[slot], pace);
        this.holdCap[slot] = Math.max(this.holdCap[slot], Math.min(kind.maxStacks(), level));
        this.holdFade[slot] = Math.max(this.holdFade[slot], Math.max(1, fadeTicks));
        this.held = true;
    }

    /** Every active pool holds the marks it earns, at one pace whichever pools there are. */
    private void holdByPools() {
        for (EffectKind pool : POOLS) {
            Effect effect = this.active.get(pool);
            if (effect != null) {
                for (EffectKind debuff : EffectInteractions.earnedBy(pool)) {
                    this.hold(debuff, Effect.STACK_INTERVAL_TICKS, debuff.maxStacks(), effect.fadeTicks());
                }
            }
        }
    }

    /** One tick of every hold: progress toward the next stack, which lands while under the level. */
    private void buildHeld() {
        for (EffectKind kind : EffectKind.values()) {
            int slot = kind.ordinal();
            this.heldNow[slot] = this.holdTicks[slot] > 0;
            if (this.holdTicks[slot] == 0) {
                this.holdProgress[slot] = 0f;
                continue;
            }
            Effect existing = this.active.get(kind);
            int stacks = existing == null ? 0 : existing.stacks();
            float gathered = this.holdProgress[slot] + 1f / this.holdTicks[slot];
            while (gathered >= 1f && stacks < this.holdCap[slot]) {
                gathered -= 1f;
                stacks++;
            }
            this.holdProgress[slot] = stacks < this.holdCap[slot] ? gathered : 0f;
            if (stacks > 0) {
                Effect count = existing == null ? Effect.counting(kind, stacks).withStacks(stacks, 0)
                        : existing.withStacks(stacks, existing.remainingTicks());
                this.active.put(kind, count.withFadeTicks(this.holdFade[slot]));
            }
            this.progress[slot] = 0f;
        }
    }

    /**
     * Whether something holds the active shared count {@code kind} now, so it loses nothing however long: a pool, a
     * field or an aura, rather than an application's own hold.
     */
    public boolean isHeld(EffectKind kind) {
        Effect effect = this.active.get(kind);
        return effect != null && kind.isCount()
                && (this.heldNow[kind.ordinal()] || effect.isHolder() && effect.remainingTicks() > 0);
    }

    /** How many sources keep a share of the active {@code kind}; {@code 0} when inactive or not kept in shares of stacks. */
    public int sources(EffectKind kind) {
        Effect effect = this.active.get(kind);
        return effect == null ? 0 : effect.stackShares().size();
    }

    /** Ends the active {@code kind} and tells whether there was one, for an effect that one hit spends. */
    public boolean consume(EffectKind kind) {
        return this.active.remove(kind) != null;
    }

    /** Active kinds in enum order, as a snapshot, for UI markers. */
    public Set<EffectKind> activeKinds() {
        return this.active.isEmpty() ? EnumSet.noneOf(EffectKind.class) : EnumSet.copyOf(this.active.keySet());
    }

    /**
     * Ticks left on the active effect of {@code kind}, or until it next loses a stack: a shared count's hold, else its
     * fade step, and the first share to end of a kind in shares of stacks. Empty when none is active, for a pool, for a
     * count something holds, and for one that lasts for good.
     */
    public OptionalInt remainingTicks(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null || kind.isFuelPool() || effect.isLasting() || this.isHeld(kind)) {
            return OptionalInt.empty();
        }
        if (kind.isCount()) {
            return OptionalInt.of(effect.remainingTicks() > 0 ? effect.remainingTicks()
                    : Math.max(1, effect.fadeTicks() - (int) this.progress[kind.ordinal()]));
        }
        return OptionalInt.of(effect.stackShares().isEmpty() ? effect.remainingTicks() : effect.nextShareEnd());
    }

    /** The kinds an active effect currently keeps out, for the inspector. */
    public Set<EffectKind> blockedKinds() {
        return EffectInteractions.blockedBy(this.active.keySet());
    }

    /** The stack count of the active {@code kind}; {@code 0} when inactive or the kind does not stack. */
    public int stacks(EffectKind kind) {
        Effect effect = this.active.get(kind);
        return effect == null ? 0 : effect.stacks();
    }

    /** The active kind that keeps {@code kind} out now, if one does. */
    public Optional<EffectKind> keptOutBy(EffectKind kind) {
        return EffectInteractions.keptOutBy(kind, this.active.keySet());
    }

    /**
     * How full a pool or a chill is, from 0 to 1: a pool's fuel over what a top-up can fill it to, a chill's slow now.
     * Empty for every other kind.
     */
    public OptionalDouble fullness(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null) {
            return OptionalDouble.empty();
        }
        return switch (kind.rule().stacking()) {
            case POOL -> OptionalDouble.of(Math.min(1f, burnTicksOf(effect.fuelLevel()) / FULL_BURN_TICKS));
            case TIMED -> switch (effect.dynamics()) {
                case EffectDynamics.Slow _, EffectDynamics.GrowingSlow _ -> OptionalDouble.of(slowNow(effect));
                default -> OptionalDouble.empty();
            };
            case COUNT, SHARES -> OptionalDouble.empty();
        };
    }

    /**
     * The ticks the clock {@link #remainingTicks} reads started from: what it was authored for, or what is left if more,
     * and a fading count's step; empty for a kind with no clock.
     */
    public OptionalInt clockTicks(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null || kind.isFuelPool() || effect.isLasting() || this.isHeld(kind)) {
            return OptionalInt.empty();
        }
        if (kind.isCount() && effect.remainingTicks() == 0) {
            return OptionalInt.of(Math.max(1, effect.fadeTicks()));
        }
        return OptionalInt.of(Math.max(effect.authoredDurationTicks(), effect.remainingTicks()));
    }

    /** The share of speed the active {@code kind} takes away now; {@code 0f} if it isn't active or doesn't slow. */
    public float slowNow(EffectKind kind) {
        Effect effect = this.active.get(kind);
        return effect == null ? 0f : slowNow(effect);
    }

    /** Whether a kind that stops the enemy is active. */
    public boolean isStopped() {
        for (EffectKind kind : EffectKind.stopping()) {
            if (this.active.containsKey(kind)) {
                return true;
            }
        }
        return false;
    }

    /** Whether no effect is active and nothing holds one, so the stats this object contributes cannot change on a tick. */
    public boolean isEmpty() {
        return this.active.isEmpty() && !this.held && !this.heldBefore;
    }

    /**
     * Adds every active effect's stat modifiers: each kind's {@link EffectKind#modifiersAt} for its
     * stacks, a ceiling on speed, the strongest slow's share of speed and a heal's regeneration. Nothing here
     * allocates.
     */
    public void contributeTo(StatAccumulator<EnemyStat> accumulator) {
        float strongestSlow = 0f;
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            this.contribute(entry.getKey(), entry.getValue(), accumulator);
            if (!entry.getKey().isSpeedCeiling()) {
                strongestSlow = Math.max(strongestSlow, slowNow(entry.getValue()));
            }
        }
        if (strongestSlow > 0f) {
            accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - strongestSlow);
        }
    }

    private static void contribute(EffectKind kind, Effect effect, StatAccumulator<EnemyStat> accumulator) {
        switch (kind.rule().stacking()) {
            case COUNT -> kind.modifiersAt(effect.stacks()).applyTo(accumulator);
            case TIMED, POOL -> kind.modifiersAt(1).applyTo(accumulator);
            // A heal's shares carry regeneration instead of stacks; a shield's carry neither.
            case SHARES -> {
                if (kind.maxStacks() > 0) {
                    kind.modifiersAt(effect.stacks()).applyTo(accumulator);
                } else {
                    accumulator.addFlat(EnemyStat.REGENERATION, effect.healPerTick());
                }
            }
        }
        if (kind.isSpeedCeiling()) {
            accumulator.capAtShareOfBase(EnemyStat.MOVE_SPEED, 1f - slowNow(effect));
        }
    }

    /** The slow that counts now: of equals, the first in kind order. */
    private Optional<Effect> strongestSlow() {
        return this.active.values().stream()
                .filter(effect -> !effect.kind().isSpeedCeiling() && slowNow(effect) > 0f)
                .reduce((a, b) -> slowNow(b) > slowNow(a) ? b : a);
    }

    /**
     * What the active {@code kind} alone adds to the stats now, for a breakdown: its own modifiers and its slow if that
     * is the one that counts.
     */
    public void contributionOf(EffectKind kind, StatAccumulator<EnemyStat> accumulator) {
        Effect effect = this.active.get(kind);
        if (effect == null) {
            return;
        }
        contribute(kind, effect, accumulator);
        this.strongestSlow().filter(effect::equals)
                .ifPresent(slow -> accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - slowNow(slow)));
    }

    /** {@link #tick(float)} for an enemy that did not move. */
    public void tick() {
        this.tick(0f);
    }

    /**
     * Applies one tick of damage-over-time, then counts every duration down and removes what
     * expired.
     * {@code cellsMoved} is how far the enemy walked this tick, which a bleed turns into damage.
     */
    public void tick(float cellsMoved) {
        this.tick(cellsMoved, PeriodicDamageListener.none());
    }

    /** {@link #tick(float)}, telling {@code listener} which effect deals each part of the tick's damage. */
    public void tick(float cellsMoved, PeriodicDamageListener listener) {
        this.holdByPools();
        if (this.held || this.heldBefore) {
            this.buildHeld();
        }
        this.heldBefore = this.held;
        List<EffectKind> expired = new ArrayList<>();
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            EffectKind kind = entry.getKey();
            Effect effect = entry.getValue();
            Optional<Effect> next = switch (effect.dynamics()) {
                case EffectDynamics.Stacks _ -> this.tickCount(effect);
                case EffectDynamics.FuelPool _ -> {
                    listener.dealing(kind);
                    yield this.tickPool(effect);
                }
                case EffectDynamics.Distance distance -> {
                    listener.dealing(kind);
                    this.bleed(effect, distance, cellsMoved);
                    yield countDown(effect);
                }
                case EffectDynamics.HealOverTime heal -> effect.afterTicks(1);
                case EffectDynamics.ShieldPool shield -> effect.afterTicks(1);
                case EffectDynamics.StackShares shares -> effect.afterTicks(1);
                default -> countDown(effect);
            };
            if (next.isPresent()) {
                entry.setValue(next.get());
            } else {
                expired.add(kind);
            }
        }
        expired.forEach(this.active::remove);
        if (this.held) {
            Arrays.fill(this.holdTicks, 0);
            Arrays.fill(this.holdCap, 0);
            Arrays.fill(this.holdFade, 0);
            this.held = false;
        }
    }

    /** Runs a timer down by a tick; empty once it runs out. */
    private static Optional<Effect> countDown(Effect effect) {
        if (effect.isLasting()) {
            return Optional.of(effect);
        }
        int remaining = effect.remainingTicks() - 1;
        return remaining <= 0 ? Optional.empty() : Optional.of(effect.withRemainingTicks(remaining));
    }

    /** Deals the damage owed for {@code cellsMoved}, keeping the part of a unit that is not yet whole. */
    private void bleed(Effect bleeding, EffectDynamics.Distance distance, float cellsMoved) {
        int slot = bleeding.kind().ordinal();
        float owed = this.progress[slot] + distance.perCell().amount() * cellsMoved;
        int whole = (int) owed;
        this.progress[slot] = owed - whole;
        if (whole > 0) {
            bleeding.sink().apply(new Damage(whole, distance.perCell().type()));
        }
    }

    /**
     * A shared count loses nothing while a holder holds it or its hold runs; after that it falls one stack each time
     * its fade step fills, the first a whole step after the hold, and ends at no stacks.
     */
    private Optional<Effect> tickCount(Effect count) {
        int slot = count.kind().ordinal();
        if (this.heldNow[slot] || count.remainingTicks() > 0) {
            this.progress[slot] = 0f;
            return Optional.of(count.remainingTicks() > 0 ? count.withRemainingTicks(count.remainingTicks() - 1) : count);
        }
        int step = Math.max(1, count.fadeTicks());
        float gathered = this.progress[slot] + 1f;
        int stacks = count.stacks();
        while (gathered >= step && stacks > 0) {
            gathered -= step;
            stacks--;
        }
        if (stacks == 0) {
            this.progress[slot] = 0f;
            return Optional.empty();
        }
        this.progress[slot] = gathered;
        return Optional.of(stacks == count.stacks() ? count : count.withStacks(stacks, 0));
    }

    /**
     * Burns {@link #BURN_RATE} times the root of the pool's fuel, every share by the same part, and on a pulse tick,
     * or once it is out, deals what has burned since the last, rounded and split by share; the part of a unit left
     * over stays owed, so a pool deals exactly its fuel. Pulses ride the stack clock, which is a multiple of the pulse
     * interval, so a pool always pulses on its first tick.
     */
    private Optional<Effect> tickPool(Effect effect) {
        float total = effect.fuelLevel();
        float burned = total < LAST_UNIT ? total : Math.min(total, BURN_RATE * (float) Math.sqrt(total));
        float share = total > 0f ? burned / total : 0f;
        List<FuelContribution> fuel = new ArrayList<>(effect.fuel().size());
        float owed = 0f;
        for (FuelContribution contribution : effect.fuel()) {
            FuelContribution after = contribution.burned(share);
            owed += after.owed();
            fuel.add(after);
        }
        boolean out = total - burned < LAST_UNIT;
        if (out || effect.stackClock() % POOL_PULSE_TICKS == 0) {
            this.pay(fuel, owed, effect.poolType());
        }
        if (out) {
            return Optional.empty();
        }
        // The clock always counts down by one, as pulses ride it.
        int clock = effect.stackClock() - 1;
        if (clock <= 0) {
            clock = Effect.STACK_INTERVAL_TICKS;
        }
        return Optional.of(effect.withFuel(fuel).withStackClock(clock));
    }

    /** Deals {@code owed}, rounded, through the shares' sinks, each its part, and keeps each share's remainder owed. */
    private void pay(List<FuelContribution> fuel, float owed, DamageType type) {
        int damage = Math.round(owed);
        if (damage <= 0) {
            return;
        }
        int[] shares = apportionPoolDamage(fuel, owed, damage);
        for (int i = 0; i < fuel.size(); i++) {
            if (shares[i] > 0) {
                fuel.get(i).sink().apply(new Damage(shares[i], type));
                fuel.set(i, fuel.get(i).paid(shares[i]));
            }
        }
    }

    /**
     * Splits {@code roundedTotal} by share using largest-remainder apportionment, so the parts sum
     * exactly. Ties go to the earliest contribution, keeping runs reproducible.
     */
    private static int[] apportionPoolDamage(List<FuelContribution> contributions, float total, int roundedTotal) {
        int n = contributions.size();
        int[] shares = new int[n];
        float[] remainders = new float[n];
        int assigned = 0;
        for (int i = 0; i < n; i++) {
            float raw = Math.max(0f, contributions.get(i).owed()) / total * roundedTotal;
            shares[i] = (int) Math.floor(raw);
            remainders[i] = raw - shares[i];
            assigned += shares[i];
        }
        int leftover = roundedTotal - assigned;
        while (leftover > 0) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (remainders[i] >= 0 && (best == -1 || remainders[i] > remainders[best])) {
                    best = i;
                }
            }
            shares[best]++;
            remainders[best] = -1f;
            leftover--;
        }
        return shares;
    }
}
