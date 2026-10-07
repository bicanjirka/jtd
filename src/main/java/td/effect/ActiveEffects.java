package td.effect;

import td.damage.Damage;
import td.damage.DamageType;
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
    /** Under Ash, a pool holds this many times as much and earns its stacks this many times as fast. */
    private static final int ASH_POOL_FACTOR = 2;
    /** The share of a chill an Ashen enemy still takes. */
    private static final float ASH_CHILL_TAKEN = 0.25f;
    /** Under Rime, each point of chill buys a freeze this many times the usual extra time. */
    private static final float RIME_CHILL_FACTOR = 2f;
    /**
     * Per-tick decay {@code alpha = e^(POOL_DECAY_EXPONENT / authoredDurationTicks)} leaves about
     * 5% of the fuel when the authored duration ends.
     */
    private static final double POOL_DECAY_EXPONENT = -3.0;
    /** A pool deals its damage in a pulse this often: four times a second. */
    private static final int POOL_PULSE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 4f);

    /** The most speed a chill takes away, so stacked chills never freeze. */
    private static final float MAX_CHILL = 0.8f;
    /** The most speed a full poison pool takes away; weaker than a chill, and it stacks with it. */
    private static final float POISON_MAX_SLOW = 0.3f;
    /** How much of a burn's damage a full chill takes away. */
    private static final float BURN_CHILL_DAMPENING = 0.5f;

    /**
     * A stack debuff loses a stack this often at neutral spirit: one per second, half as fast as a pool
     * earns them. A spirit above zero speeds it up, and at -100 it never happens.
     */
    private static final int STACK_DECAY_INTERVAL_TICKS = 20;

    /** The slowest a spirit-paced timer runs, however low the enemy's spirit. */
    private static final float MIN_DEBUFF_PACE = 0.25f;

    /** Extra damage taken per vulnerable stack, of every damage type. */
    private static final float VULNERABLE_PER_STACK = 0.15f;
    /** Damage taken while a priority lasts, of every damage type. */
    private static final float PRIORITY_DAMAGE_TAKEN = 1.15f;
    private static final float ARMOR_LOST_PER_SUNDERED_STACK = 5f;
    private static final float MAGIC_DAMAGE_TAKEN_PER_RESONATING_STACK = 0.08f;
    private static final float RESILIENCE_LOST_PER_FRACTURED_STACK = 10f;
    /** What an exposed or revealed enemy multiplies the crit chance taken by. */
    private static final float EXPOSED_CRIT_CHANCE_TAKEN = 2f;
    private static final StatModifier FROZEN = StatModifier.setTo(0f);
    private static final StatModifier HIDDEN = StatModifier.setTo(1f);
    private static final StatModifier REVEALED = StatModifier.setTo(0f);
    private static final DamageType[] DAMAGE_TYPES = DamageType.values();

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

    /** How hard an effect bites, in a unit specific to its kind; only compared within one kind. */
    private static float magnitude(Effect effect) {
        return switch (effect.kind()) {
            case FREEZE, DAZED -> 1f - effect.speedMultiplier();
            case CHILL -> effect.fuelLevel();
            case BURN, POISON -> effect.damagePerTick().amount();
            case SHIELD -> effect.shieldPercent();
            // On/off, not gradated - any reapplication is at least as strong as what's already active.
            case INVISIBLE, REVEALED, EXPOSED, MARKED, PRIORITY, CHARGED, DOOM, BLIGHT, CONTAGION, RIME, ASH -> 1f;
            case HEAL -> effect.healPerTick();
            case VULNERABLE, SCORCHED, SICKENED, SUNDERED, RESONATING, FRACTURED, SATURATED -> effect.stacks();
        };
    }

    /**
     * Applies {@code effect} unless an active effect keeps its kind out (see
     * {@link EffectInteractions}); applying it also removes the kinds it consumes, and a freeze
     * that consumes a chill lasts longer by the chill's level, twice that under Rime. Under Rime a
     * freeze also lands the burn it puts out, all of it at once.
     */
    public void apply(Effect effect) {
        if (EffectInteractions.blocks(this.active.keySet(), effect.kind())) {
            return;
        }
        Effect incoming = effect;
        boolean rime = this.active.containsKey(EffectKind.RIME);
        Effect chill = this.active.get(EffectKind.CHILL);
        if (effect.kind() == EffectKind.FREEZE && chill != null) {
            incoming = effect.withDurationScaledBy(1f + (rime ? RIME_CHILL_FACTOR : 1f) * chillLevel(chill));
        }
        Effect burn = this.active.get(EffectKind.BURN);
        if (effect.kind() == EffectKind.FREEZE && rime && burn != null) {
            burstPool(burn);
        }
        EffectInteractions.removedBy(effect.kind()).forEach(this.active::remove);
        switch (incoming.kind()) {
            case CHILL -> this.applyChill(incoming);
            case BURN, POISON -> this.applyPool(incoming);
            case VULNERABLE, SUNDERED, RESONATING, FRACTURED, SATURATED -> this.applyStacks(incoming);
            default -> {
                Effect existing = this.active.get(incoming.kind());
                this.active.put(incoming.kind(), existing == null ? incoming : strongerOf(existing, incoming));
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
        FuelContribution contribution = this.active.containsKey(EffectKind.ASH)
                ? authored.scaledTo(authored.amount() * ASH_CHILL_TAKEN) : authored;
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
        float lmax = POOL_LMAX_MULTIPLIER * peakL0 * this.ashFactor();
        float deltaL = incomingL0 * (1f - existing.fuelLevel() / lmax);
        List<FuelContribution> fuel = new ArrayList<>(existing.fuel());
        fuel.add(new FuelContribution(incoming.sink(), deltaL));
        this.active.put(incoming.kind(), existing.withFuel(List.copyOf(fuel), peakL0));
    }

    /** Adds a stack of the debuff that {@code pool} earns, starting the debuff if the enemy has none. */
    private void earnStack(EffectKind pool) {
        EffectKind debuff = pool.debuffEarned().orElseThrow();
        Effect existing = this.active.get(debuff);
        this.active.put(debuff, Effect.stackDebuff(debuff, existing == null ? 1 : existing.stacks() + 1));
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

    /** Takes {@code fraction} of the shield's strength; a shield that is gone stays gone. */
    public void weakenShield(float fraction) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield != null) {
            this.active.put(EffectKind.SHIELD, shield.withShieldPercent(shield.shieldPercent() * (1f - fraction)));
        }
    }

    /** Whether crits find this enemy more often: it is exposed, or revealed, which exposes it. */
    private boolean isExposed() {
        return this.active.containsKey(EffectKind.EXPOSED) || this.active.containsKey(EffectKind.REVEALED);
    }

    public boolean has(EffectKind kind) {
        return this.active.containsKey(kind);
    }

    /** Ends the active {@code kind} and tells whether there was one, for an effect that one hit spends. */
    /** Every active effect, in kind order: a copy, so a caller may pass them on. */
    public List<Effect> effects() {
        return List.copyOf(this.active.values());
    }

    /** The active effect of {@code kind}; empty when it isn't active. */
    public Optional<Effect> find(EffectKind kind) {
        return Optional.ofNullable(this.active.get(kind));
    }

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
     * Adds every active effect's stat modifiers: a chill multiplies speed by what is left of its
     * level and a poison by what is left of its pool, a freeze sets it to zero, a shield adds
     * shielding for the types it covers, a heal adds regeneration, invisibility sets stealth to
     * one and a reveal sets it to zero (the lowest set value wins), a vulnerability multiplies damage
     * taken by its stacks, each scorched stack lowers resilience by one and each sickened stack
     * lowers spirit by one. Shields and heals go in as restorative, so the enemy's spirit scales them.
     * Being exposed or revealed doubles the crit chance taken, once however many apply.
     */
    public void contributeTo(StatAccumulator accumulator) {
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            Effect effect = entry.getValue();
            switch (entry.getKey()) {
                case CHILL -> accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - chillLevel(effect));
                case FREEZE, DAZED -> accumulator.add(EnemyStat.MOVE_SPEED, FROZEN);
                case SHIELD -> {
                    for (DamageType type : DAMAGE_TYPES) {
                        if (effect.shieldRestrictedTo().isEmpty() || effect.shieldRestrictedTo().get() == type) {
                            accumulator.restoreFlat(EnemyStat.shieldingFor(type), effect.shieldPercent());
                        }
                    }
                }
                case HEAL -> accumulator.restoreFlat(EnemyStat.REGENERATION, effect.healPerTick());
                case INVISIBLE -> accumulator.add(EnemyStat.STEALTH, HIDDEN);
                case REVEALED -> accumulator.add(EnemyStat.STEALTH, REVEALED);
                case VULNERABLE -> multiplyDamageTaken(accumulator, 1f + VULNERABLE_PER_STACK * effect.stacks());
                case PRIORITY -> multiplyDamageTaken(accumulator, PRIORITY_DAMAGE_TAKEN);
                case RESONATING -> accumulator.multiply(EnemyStat.MAGIC_DAMAGE_TAKEN,
                        1f + MAGIC_DAMAGE_TAKEN_PER_RESONATING_STACK * effect.stacks());
                case SUNDERED -> accumulator.addFlat(EnemyStat.ARMOR,
                        -ARMOR_LOST_PER_SUNDERED_STACK * effect.stacks());
                case FRACTURED -> accumulator.addFlat(EnemyStat.RESILIENCE,
                        -RESILIENCE_LOST_PER_FRACTURED_STACK * effect.stacks());
                case EXPOSED, MARKED, SATURATED, CHARGED, DOOM, BLIGHT, CONTAGION, RIME, ASH -> {
                }
                case BURN -> {
                }
                case SCORCHED -> accumulator.addFlat(EnemyStat.RESILIENCE, -effect.stacks());
                case SICKENED -> accumulator.addFlat(EnemyStat.SPIRIT, -effect.stacks());
                case POISON -> {
                    float pool = effect.fuelLevel() / (POOL_LMAX_MULTIPLIER * effect.peakL0());
                    accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - POISON_MAX_SLOW * Math.min(1f, pool));
                }
            }
        }
        if (this.isExposed()) {
            accumulator.multiply(EnemyStat.CRIT_CHANCE_TAKEN, EXPOSED_CRIT_CHANCE_TAKEN);
        }
    }

    private static void multiplyDamageTaken(StatAccumulator accumulator, float taken) {
        for (DamageType type : DAMAGE_TYPES) {
            accumulator.multiply(EnemyStat.damageTakenFor(type), taken);
        }
    }

    /** {@link #tick(float)} at neutral spirit. */
    public void tick() {
        this.tick(1f);
    }

    /**
     * Applies one tick of damage-over-time, then counts every duration down and removes what
     * expired. A decaying level fades instead of counting down. {@code spiritFactor} ({@code 1} at
     * neutral spirit, {@code 0} when spirit is at its floor) sets the pace of the stack debuffs a pool
     * earns; every debuff a tower applies runs at {@code max(MIN_DEBUFF_PACE, spiritFactor)}.
     */
    public void tick(float spiritFactor) {
        List<EffectKind> expired = new ArrayList<>();
        float burnFactor = 1f - BURN_CHILL_DAMPENING * this.chillLevel() / MAX_CHILL;
        float debuffPace = Math.max(MIN_DEBUFF_PACE, spiritFactor);
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            EffectKind kind = entry.getKey();
            Effect effect = entry.getValue();
            Optional<Effect> next;
            if (kind.isStackDebuff()) {
                next = this.decayStacks(effect, kind == EffectKind.FRACTURED ? debuffPace : spiritFactor);
            } else if (kind == EffectKind.CHILL) {
                next = this.tickChill(effect, this.paceSteps(kind, debuffPace));
            } else if (kind.isFuelPool()) {
                next = this.tickPool(effect, kind == EffectKind.BURN ? burnFactor : 1f);
            } else {
                if (effect.damagePerTick().amount() > 0) {
                    effect.sink().apply(effect.damagePerTick());
                }
                int steps = kind.isPacedBySpirit() ? this.paceSteps(kind, debuffPace) : 1;
                int remaining = effect.remainingTicks() - steps;
                next = remaining <= 0 ? Optional.empty() : Optional.of(effect.withRemainingTicks(remaining));
            }
            if (next.isPresent()) {
                entry.setValue(next.get());
            } else {
                expired.add(kind);
            }
        }
        expired.forEach(this.active::remove);
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
     * stacks. A Fault Line Fractured holds while the enemy is exposed.
     */
    private Optional<Effect> decayStacks(Effect debuff, float pace) {
        if (debuff.faultLine() && this.isExposed()) {
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
        if (clock % (Effect.STACK_INTERVAL_TICKS / this.ashFactor()) == 0) {
            this.earnStack(effect.kind());
        }
        if (clock <= 0) {
            clock = Effect.STACK_INTERVAL_TICKS;
        }
        return Optional.of(effect.withFuel(decayed, effect.peakL0()).withStackClock(clock));
    }

    /** The pool's total over one pulse window as a multiple of its level now: {@code 1 + a + a^2 + ...}. */
    /** How much more a pool holds, and how much faster it earns stacks: twice under Ash. */
    private int ashFactor() {
        return this.active.containsKey(EffectKind.ASH) ? ASH_POOL_FACTOR : 1;
    }

    /** Lands everything {@code pool} would still have dealt, at once, each contributor its share. */
    private static void burstPool(Effect pool) {
        float total = pool.fuelLevel();
        double alpha = Math.exp(POOL_DECAY_EXPONENT / pool.authoredDurationTicks());
        int damage = (int) Math.round(total / (1.0 - alpha));
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
