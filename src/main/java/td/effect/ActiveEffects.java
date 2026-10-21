package td.effect;

import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatAccumulator;
import td.stat.StatModifier;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
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
 * effect. The decaying kinds and the stacking kinds combine differently: see {@link #applyChill},
 * {@link #applyPool} and {@link #applyStacks}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ActiveEffects {

    /** Caps a fuel pool at this multiple of the strongest single application ever taken. */
    private static final float POOL_LMAX_MULTIPLIER = 2f;
    /**
     * Per-tick decay {@code alpha = e^(POOL_DECAY_EXPONENT / authoredDurationTicks)} leaves about
     * 5% of the fuel when the authored duration ends.
     */
    private static final double POOL_DECAY_EXPONENT = -3.0;
    /** A pool deals its damage in a pulse this often: four times a second. */
    private static final int POOL_PULSE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 4f);

    /** The most speed a chill takes away, so stacked chills never freeze. */
    static final float MAX_CHILL = 0.8f;
    /** The most speed a full poison pool takes away; weaker than a chill, and it stacks with it. */
    static final float POISON_MAX_SLOW = 0.3f;
    /** How much of a burn's damage a full chill takes away. */
    static final float BURN_CHILL_DAMPENING = 0.5f;

    /**
     * A stack debuff loses a stack this often at neutral spirit: one per second, half as fast as a pool
     * earns them. A spirit above zero speeds it up, and at -100 it never happens.
     */
    public static final int STACK_DECAY_INTERVAL_TICKS = 20;

    /** The slowest a spirit-paced timer runs, however low the enemy's spirit. */
    private static final float MIN_DEBUFF_PACE = 0.25f;

    /** A burn pool reveals an invisible enemy while it holds more than this, in damage units a tick. */
    private static final float BURN_REVEAL_FUEL = 30f;
    private static final EffectKind[] BURNING_KINDS = {EffectKind.BURN, EffectKind.SOULFIRE};
    private static final PoolTuning NO_TUNING = PoolTuning.standard();
    /** What a burning pool above {@link #BURN_REVEAL_FUEL} does to stealth. */
    private static final StatModifier REVEALED = StatModifier.setTo(0f);

    private final Map<EffectKind, Effect> active = new EnumMap<>(EffectKind.class);
    /**
     * Per kind, spirit-scaled progress that is not yet a whole step: towards a stack debuff's next
     * lost stack, or the part of a tick a paced timer has gathered.
     */
    private final float[] progress = new float[EffectKind.values().length];

    private static Effect strongerOf(Effect a, Effect b) {
        Effect stronger = magnitude(a) >= magnitude(b) ? a : b;
        return stronger.withRemainingTicks(Math.max(a.remainingTicks(), b.remainingTicks()));
    }

    /** How hard an effect bites, in a unit specific to its family; only compared within one kind. */
    private static float magnitude(Effect effect) {
        return switch (effect.dynamics()) {
            // On/off, not gradated - any reapplication is at least as strong as what's already active.
            case EffectDynamics.Flag _ -> 1f;
            case EffectDynamics.Stacks stacks -> stacks.stacks();
            case EffectDynamics.ChillLevel _ -> effect.fuelLevel();
            case EffectDynamics.FuelPool pool -> pool.perTick().amount();
            case EffectDynamics.HealOverTime heal -> heal.perTick();
            case EffectDynamics.TickDamage damage -> damage.perTick().amount();
            case EffectDynamics.ShieldPool shield -> shield.remaining();
            case EffectDynamics.Distance distance -> distance.perCell().amount();
        };
    }

    /**
     * Applies {@code effect} unless an active effect keeps its kind out (see
     * {@link EffectInteractions}); applying it also removes the kinds it consumes. How a freeze and a
     * fire are bent by what is already there (chill, Rime, Tar) is read from {@link EffectInteractions}.
     */
    public void apply(Effect effect) {
        Set<EffectKind> kinds = this.active.keySet();
        if (EffectInteractions.blocks(kinds, effect.kind())) {
            return;
        }
        Effect incoming = effect;
        if (effect.kind() == EffectKind.FREEZE) {
            Effect chill = this.active.get(EffectKind.CHILL);
            float lengthFactor = EffectInteractions.freezeLengthFactor(kinds, chill == null ? 0f : chillLevel(chill));
            if (lengthFactor != 1f) {
                incoming = incoming.withDurationScaledBy(lengthFactor);
            }
            int extraTicks = EffectInteractions.freezeExtraTicks(kinds);
            if (extraTicks > 0) {
                incoming = incoming.withExtraTicks(extraTicks);
            }
            for (EffectKind fire : EffectKind.values()) {
                Effect burning = fire.isBurning() ? this.active.get(fire) : null;
                if (burning != null) {
                    burstPool(burning, EffectInteractions.freezeBurstShare(kinds, burning.tuning().freezeBurstShare()));
                }
            }
        }
        if (effect.kind().isBurning() && !this.active.containsKey(effect.kind())) {
            float startFactor = EffectInteractions.fireStartFactor(kinds);
            if (startFactor != 1f) {
                incoming = incoming.withPoolScaledBy(startFactor);
            }
        }
        EffectInteractions.removedBy(effect.kind()).forEach(this.active::remove);
        switch (incoming.kind().rule().stacking()) {
            case LEVEL -> this.applyChill(incoming);
            case POOL -> this.applyPool(incoming);
            case SHARED_CLOCK, EARNED -> this.applyStacks(incoming);
            case REFRESH -> {
                Effect existing = this.active.get(incoming.kind());
                // A hold renewed before it ends would never end: its diminishing returns only count fresh holds.
                boolean holding = existing != null && incoming.kind().category() == EffectCategory.HARD_CC;
                this.active.put(incoming.kind(), existing == null ? incoming
                        : holding ? existing : strongerOf(existing, incoming));
            }
        }
    }

    private static float chillLevel(Effect chill) {
        return Math.min(MAX_CHILL, chill.fuelLevel());
    }

    /**
     * {@code CHILL} is a level that adds up, up to {@value #MAX_CHILL}, and decays linearly: each
     * application keeps the slope it came with. An application only adds what fits under the cap,
     * so a chill held at the cap is refilled only by what has decayed.
     */
    private void applyChill(Effect incoming) {
        Effect existing = this.active.get(EffectKind.CHILL);
        float headroom = MAX_CHILL - (existing == null ? 0f : existing.fuelLevel());
        if (headroom <= 0f) {
            return;
        }
        FuelContribution authored = incoming.fuel().getFirst();
        float taken = EffectInteractions.chillTaken(this.active.keySet());
        FuelContribution contribution = taken != 1f ? authored.scaledTo(authored.amount() * taken) : authored;
        FuelContribution added = contribution.amount() <= headroom ? contribution : contribution.scaledTo(headroom);
        if (existing == null) {
            this.active.put(EffectKind.CHILL, incoming.withFuel(List.of(added), 0f));
            return;
        }
        List<FuelContribution> fuel = new ArrayList<>(existing.fuel());
        fuel.add(added);
        this.active.put(EffectKind.CHILL, existing.withFuel(List.copyOf(fuel), 0f));
    }

    /**
     * A fuel pool ({@code BURN} or {@code POISON}, each its own pool) is additive and decaying. A
     * reapplication adds its intensity, scaled down the closer the pool is to its cap, and never
     * changes the decay rate or earns a stack. A pool that starts earns its first stack at once and
     * another every {@code STACK_INTERVAL_TICKS} while it lasts. Each contribution keeps its own
     * sink, so every contributing tower is credited for its share.
     */
    private void applyPool(Effect incoming) {
        Effect existing = this.active.get(incoming.kind());
        if (existing == null) {
            this.active.put(incoming.kind(), incoming);
            this.earnStack(incoming.kind());
            return;
        }
        float incomingL0 = incoming.damagePerTick().amount();
        float peakL0 = Math.max(existing.peakL0(), incomingL0);
        PoolTuning tuning = existing.tuning().strongerWith(incoming.tuning());
        float lmax = tuning.capFactor() * peakL0 * this.poolFactor();
        float deltaL = incomingL0 * (1f - existing.fuelLevel() / lmax);
        List<FuelContribution> fuel = new ArrayList<>(existing.fuel());
        fuel.add(new FuelContribution(incoming.sink(), deltaL));
        this.active.put(incoming.kind(), existing.withFuel(List.copyOf(fuel), peakL0).withTuning(tuning));
    }

    /** Adds a stack of each debuff that {@code pool} earns, starting the debuff if the enemy has none. */
    private void earnStack(EffectKind pool) {
        for (EffectKind debuff : EffectInteractions.earnedBy(pool)) {
            Effect existing = this.active.get(debuff);
            int stacks = Math.min(debuff.maxStacks(), existing == null ? 1 : existing.stacks() + 1);
            this.active.put(debuff, Effect.stackDebuff(debuff, stacks));
        }
    }

    /**
     * A stacking kind adds up to {@link EffectKind#maxStacks()} on the enemy, whichever tower applied
     * them, on one clock that every application refreshes. A full stack only refreshes. An effect
     * allowed more stacks keeps that cap, and once Fault Line has touched a Fractured, it stays that
     * way, until it wears off.
     */
    private void applyStacks(Effect incoming) {
        Effect existing = this.active.get(incoming.kind());
        Effect next = existing != null && existing.faultLine() ? incoming.withFaultLine() : incoming;
        int cap = Math.max(next.effectiveStackCap(), existing == null ? 0 : existing.effectiveStackCap());
        int stacks = Math.min(cap, (existing == null ? 0 : existing.stacks()) + incoming.stacks());
        int remaining = Math.max(existing == null ? 0 : existing.remainingTicks(), incoming.remainingTicks());
        this.active.put(incoming.kind(), next.withStackCap(cap).withStacks(stacks, remaining));
    }

    /** Takes {@code fraction} of what the shield has left; a shield that is gone stays gone. */
    public void weakenShield(float fraction) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield != null) {
            this.active.put(EffectKind.SHIELD, shield.withShieldRemaining(shield.shieldRemaining() * (1f - fraction)));
        }
    }

    /**
     * Takes what the active shield can absorb of {@code amount} units of {@code type} and returns it;
     * a shield limited to the other type absorbs nothing. A shield with less than a unit left breaks.
     */
    public int absorb(DamageType type, int amount) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield == null || amount <= 0
                || (shield.shieldRestrictedTo().isPresent() && shield.shieldRestrictedTo().get() != type)) {
            return 0;
        }
        int absorbed = Math.min(amount, (int) shield.shieldRemaining());
        float left = shield.shieldRemaining() - absorbed;
        if (left < 1f) {
            this.active.remove(EffectKind.SHIELD);
        } else {
            this.active.put(EffectKind.SHIELD, shield.withShieldRemaining(left));
        }
        return absorbed;
    }

    /** The active shield's pool, in points; {@link ShieldGauge#none()} without one. */
    public ShieldGauge shieldGauge() {
        Effect shield = this.active.get(EffectKind.SHIELD);
        return shield == null ? ShieldGauge.none()
                : new ShieldGauge(DamageUnits.inPoints(shield.shieldRemaining()), DamageUnits.inPoints(shield.shieldMax()));
    }

    /** Whether crits find this enemy more often: it is exposed, or revealed, which exposes it. */
    private boolean isExposed() {
        return EffectInteractions.isActiveOrImplied(EffectKind.EXPOSED, this.active.keySet());
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

    /** Takes {@code count} stacks off the active {@code kind}, keeping its clock; none left ends it. */
    public void loseStacks(EffectKind kind, int count) {
        Effect effect = this.active.get(kind);
        if (effect == null) {
            return;
        }
        int left = effect.stacks() - count;
        if (left <= 0) {
            this.active.remove(kind);
        } else {
            this.active.put(kind, effect.withStacks(left, effect.remainingTicks()));
        }
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
     * Ticks left on the active effect of {@code kind}; empty when none is active or for a decaying
     * level, which fades rather than counting down.
     */
    public OptionalInt remainingTicks(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null || kind.isDecaying()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(effect.remainingTicks());
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
     * How full a fading level or pool is, from 0 to 1: a chill's level over its cap, a pool's fuel over what a
     * top-up can fill it to. Empty for every other kind.
     */
    public OptionalDouble fullness(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null) {
            return OptionalDouble.empty();
        }
        return switch (kind.rule().stacking()) {
            case LEVEL -> OptionalDouble.of(chillLevel(effect) / MAX_CHILL);
            case POOL -> OptionalDouble.of(Math.min(1f,
                    effect.fuelLevel() / (effect.tuning().capFactor() * effect.peakL0() * this.poolFactor())));
            case REFRESH, SHARED_CLOCK, EARNED -> OptionalDouble.empty();
        };
    }

    /**
     * The ticks the active {@code kind}'s clock started from: what it was authored for, or what is left if more;
     * empty for a kind with no clock.
     */
    public OptionalInt clockTicks(EffectKind kind) {
        Effect effect = this.active.get(kind);
        if (effect == null || kind.isDecaying()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Math.max(effect.authoredDurationTicks(), effect.remainingTicks()));
    }

    /** How much speed the active chill takes away, from {@code 0} to {@value #MAX_CHILL}. */
    public float chillLevel() {
        Effect chill = this.active.get(EffectKind.CHILL);
        return chill == null ? 0f : chillLevel(chill);
    }

    /** Whether no effect is active, so the stats this object contributes cannot change on a tick. */
    public boolean isEmpty() {
        return this.active.isEmpty();
    }

    /**
     * Adds every active effect's stat modifiers: each kind's {@link EffectKind#modifiersAt} for its
     * stacks (when what it needs is there), the chill level on speed, the pools' own rules, a heal's
     * regeneration, and what the burning pools carry. Being exposed or revealed counts once however
     * many apply. Nothing here allocates.
     */
    public void contributeTo(StatAccumulator<EnemyStat> accumulator) {
        Set<EffectKind> kinds = this.active.keySet();
        this.burningTuning().carried().applyTo(accumulator);
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            EffectKind kind = entry.getKey();
            Effect effect = entry.getValue();
            if (!EffectInteractions.contributes(kind, kinds)) {
                continue;
            }
            switch (kind.rule().stacking()) {
                case SHARED_CLOCK, EARNED -> kind.modifiersAt(effect.stacks()).applyTo(accumulator);
                case REFRESH -> kind.modifiersAt(1).applyTo(accumulator);
                case LEVEL -> accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - chillLevel(effect));
                case POOL -> this.contributePool(kind, effect, accumulator);
            }
            if (kind == EffectKind.HEAL) {
                accumulator.restoreFlat(EnemyStat.REGENERATION, effect.healPerTick());
            }
        }
        if (!this.active.containsKey(EffectKind.EXPOSED) && EffectInteractions.isActiveOrImplied(EffectKind.EXPOSED, kinds)) {
            EffectKind.EXPOSED.modifiersAt(1).applyTo(accumulator);
        }
    }

    /** A burning pool that holds enough reveals the enemy; a poison slows by how full it is. */
    private void contributePool(EffectKind kind, Effect pool, StatAccumulator<EnemyStat> accumulator) {
        if (kind.isBurning() && pool.fuelLevel() > BURN_REVEAL_FUEL) {
            accumulator.add(EnemyStat.STEALTH, REVEALED);
        }
        if (kind == EffectKind.POISON) {
            float share = pool.fuelLevel() / (POOL_LMAX_MULTIPLIER * pool.peakL0());
            accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - POISON_MAX_SLOW * Math.min(1f, share));
        }
    }

    /** What every fire on the enemy has made of it together: the stronger of each setting. */
    private PoolTuning burningTuning() {
        PoolTuning tuning = null;
        for (EffectKind fire : BURNING_KINDS) {
            Effect pool = this.active.get(fire);
            if (pool != null) {
                tuning = tuning == null ? pool.tuning() : tuning.strongerWith(pool.tuning());
            }
        }
        return tuning == null ? NO_TUNING : tuning;
    }

    /** {@link #tick(float)} at neutral spirit. */
    public void tick() {
        this.tick(1f);
    }

    /** {@link #tick(float, float)} for an enemy that did not move. */
    public void tick(float spiritFactor) {
        this.tick(spiritFactor, 0f);
    }

    /**
     * Applies one tick of damage-over-time, then counts every duration down and removes what
     * expired. A decaying level fades instead of counting down. {@code spiritFactor} ({@code 1} at
     * neutral spirit, {@code 0} when spirit is at its floor) sets the pace of the stack debuffs a pool
     * earns; every debuff a tower applies runs at {@code max(MIN_DEBUFF_PACE, spiritFactor)}, and not at
     * all while spirit is at its floor.
     * {@code cellsMoved} is how far the enemy walked this tick, which a bleed turns into damage.
     */
    public void tick(float spiritFactor, float cellsMoved) {
        List<EffectKind> expired = new ArrayList<>();
        float burnFactor = 1f - BURN_CHILL_DAMPENING * this.chillLevel() / MAX_CHILL;
        float debuffPace = spiritFactor <= 0f ? 0f : Math.max(MIN_DEBUFF_PACE, spiritFactor);
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            EffectKind kind = entry.getKey();
            Effect effect = entry.getValue();
            Optional<Effect> next = switch (effect.dynamics()) {
                case EffectDynamics.Stacks _ when kind.isStackDebuff() ->
                        this.decayStacks(effect, kind.rule().pacing() == EffectPacing.SPIRIT ? debuffPace : spiritFactor);
                case EffectDynamics.ChillLevel _ -> this.tickChill(effect, this.paceSteps(kind, debuffPace));
                case EffectDynamics.FuelPool _ -> this.tickPool(effect, kind.isBurning() ? burnFactor : 1f);
                case EffectDynamics.Distance distance -> {
                    this.bleed(effect, distance, cellsMoved);
                    yield this.countDown(effect, debuffPace);
                }
                case EffectDynamics.TickDamage damage -> {
                    if (damage.perTick().amount() > 0) {
                        effect.sink().apply(damage.perTick());
                    }
                    yield this.countDown(effect, debuffPace);
                }
                default -> this.countDown(effect, debuffPace);
            };
            if (next.isPresent()) {
                entry.setValue(next.get());
            } else {
                expired.add(kind);
            }
        }
        expired.forEach(this.active::remove);
    }

    /** Runs a timer down by a tick, or by its spirit pace when it has one; empty once it runs out. */
    private Optional<Effect> countDown(Effect effect, float debuffPace) {
        EffectKind kind = effect.kind();
        int steps = kind.isPacedBySpirit() ? this.paceSteps(kind, debuffPace) : 1;
        int remaining = effect.remainingTicks() - steps;
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

    /** The whole ticks a paced timer of {@code kind} runs this tick at {@code pace}, carrying the rest over. */
    private int paceSteps(EffectKind kind, float pace) {
        float gathered = this.progress[kind.ordinal()] + pace;
        int steps = (int) gathered;
        this.progress[kind.ordinal()] = gathered - steps;
        return steps;
    }

    /**
     * Lets a stack debuff fall one stack each time its spirit-scaled progress fills; ends at no
     * stacks. A Fault Line Fractured holds while the enemy is exposed, and a mark holds while a pool
     * that earns it is still active.
     */
    private Optional<Effect> decayStacks(Effect debuff, float pace) {
        if (debuff.faultLine() && this.isExposed() || EffectInteractions.isEarnedBy(debuff.kind(), this.active.keySet())) {
            return Optional.of(debuff);
        }
        int slot = debuff.kind().ordinal();
        float gathered = this.progress[slot] + pace;
        int stacks = debuff.stacks();
        while (gathered >= STACK_DECAY_INTERVAL_TICKS && stacks > 0) {
            gathered -= STACK_DECAY_INTERVAL_TICKS;
            stacks--;
        }
        if (stacks == 0) {
            this.progress[slot] = 0f;
            return Optional.empty();
        }
        this.progress[slot] = gathered;
        return Optional.of(debuff.withStacks(stacks, debuff.remainingTicks()));
    }

    /** {@code steps} ticks of linear decay on every contribution; ends when all of them have run out. */
    private Optional<Effect> tickChill(Effect effect, int steps) {
        if (steps == 0) {
            return Optional.of(effect);
        }
        List<FuelContribution> remaining = effect.fuel().stream()
                .map(contribution -> {
                    FuelContribution decayed = contribution;
                    for (int i = 0; i < steps; i++) {
                        decayed = decayed.decayedLinearly();
                    }
                    return decayed;
                })
                .filter(c -> c.amount() > 0f)
                .toList();
        return remaining.isEmpty() ? Optional.empty() : Optional.of(effect.withFuel(remaining, 0f));
    }

    /**
     * On a pulse tick, deals what the pool would have dealt over the next {@code POOL_PULSE_TICKS}
     * ticks, rounded and times {@code damageFactor}, split by share. Every tick decays every
     * contribution by the same {@code alpha}, which decays the total exactly, and lets the stack
     * clock run. Ends once a tick would round to zero damage before the factor, since exponential
     * decay never reaches zero. Pulses ride the stack clock, which is a multiple of the pulse
     * interval, so a pool always pulses on its first tick.
     */
    private Optional<Effect> tickPool(Effect effect, float damageFactor) {
        List<FuelContribution> contributions = effect.fuel();
        float total = effect.fuelLevel();
        if (Math.round(total) <= 0) {
            return Optional.empty();
        }
        double alpha = Math.exp(POOL_DECAY_EXPONENT / effect.authoredDurationTicks());
        if (effect.stackClock() % POOL_PULSE_TICKS == 0) {
            int damage = Math.round(total * (float) windowFactor(alpha) * damageFactor);
            if (damage > 0) {
                int[] shares = apportionPoolDamage(contributions, total, damage);
                DamageType type = effect.damagePerTick().type();
                for (int i = 0; i < contributions.size(); i++) {
                    if (shares[i] > 0) {
                        contributions.get(i).sink().apply(new Damage(shares[i], type));
                    }
                }
            }
        }
        List<FuelContribution> decayed = contributions.stream()
                .map(c -> c.decayedBy((float) alpha))
                .toList();
        // The clock always counts down by one, as pulses ride it; Ash only earns stacks more often.
        int clock = effect.stackClock() - 1;
        int stackInterval = Math.max(1, Effect.STACK_INTERVAL_TICKS / (this.poolFactor() * effect.tuning().stackRate()));
        if (clock % stackInterval == 0) {
            this.earnStack(effect.kind());
        }
        if (clock <= 0) {
            clock = Effect.STACK_INTERVAL_TICKS;
        }
        return Optional.of(effect.withFuel(decayed, effect.peakL0()).withStackClock(clock));
    }

    /** How much more a pool holds, and how much faster it earns stacks. */
    private int poolFactor() {
        return EffectInteractions.poolFactor(this.active.keySet());
    }

    /** Lands {@code share} of everything {@code pool} would still have dealt, at once, each contributor its part. */
    private static void burstPool(Effect pool, float share) {
        float total = pool.fuelLevel();
        int damage = Math.round(share * pool.remainingPoolDamage());
        if (damage <= 0) {
            return;
        }
        int[] shares = apportionPoolDamage(pool.fuel(), total, damage);
        for (int i = 0; i < pool.fuel().size(); i++) {
            if (shares[i] > 0) {
                pool.fuel().get(i).sink().apply(new Damage(shares[i], pool.damagePerTick().type()));
            }
        }
    }

    /** The pool's total over one pulse window as a multiple of its level now: {@code 1 + a + a^2 + ...}. */
    private static double windowFactor(double alpha) {
        double factor = 0;
        double term = 1;
        for (int i = 0; i < POOL_PULSE_TICKS; i++) {
            factor += term;
            term *= alpha;
        }
        return factor;
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
            float raw = contributions.get(i).amount() / total * roundedTotal;
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
