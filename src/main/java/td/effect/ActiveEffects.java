package td.effect;

import td.damage.Damage;
import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatAccumulator;
import td.stat.StatModifier;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The effects active on one enemy, at most one per {@link EffectKind}. Reapplying a kind keeps the
 * stronger one and the longer remaining duration, so a weak top-up never cuts short a strong
 * effect. {@code SLOW} and {@code BURN} combine differently: see {@link #applySlow} and
 * {@link #applyBurn}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ActiveEffects {

    /** Caps the burn pool at this multiple of the strongest single application ever taken. */
    private static final float BURN_LMAX_MULTIPLIER = 2f;
    /**
     * Per-tick decay {@code alpha = e^(BURN_DECAY_EXPONENT / authoredDurationTicks)} leaves about
     * 5% of the fuel when the authored duration ends.
     */
    private static final double BURN_DECAY_EXPONENT = -3.0;

    /** A burning enemy is this much more likely to take a critical hit, from any tower. */
    private static final float BURN_CRIT_CHANCE_TAKEN = 2f;
    private static final StatModifier FROZEN = StatModifier.setTo(0f);
    private static final StatModifier HIDDEN = StatModifier.setTo(1f);
    private static final DamageType[] DAMAGE_TYPES = DamageType.values();

    private final Map<EffectKind, Effect> active = new EnumMap<>(EffectKind.class);
    private Effect slowSuperseded;

    private static Effect strongerOf(Effect a, Effect b) {
        Effect stronger = magnitude(a) >= magnitude(b) ? a : b;
        return stronger.withRemainingTicks(Math.max(a.remainingTicks(), b.remainingTicks()));
    }

    /** How hard an effect bites, in a unit specific to its kind; only compared within one kind. */
    private static float magnitude(Effect effect) {
        return switch (effect.kind()) {
            case SLOW, FREEZE -> 1f - effect.speedMultiplier();
            case BURN -> effect.damagePerTick().amount();
            case SHIELD -> effect.shieldPercent();
            // On/off, not gradated - any reapplication is at least as strong as what's already active.
            case INVISIBLE -> 1f;
            case HEAL -> effect.healPerTick();
        };
    }

    /**
     * Where {@code slow} is in its life: {@code 0} when applied, {@code 1} once its authored
     * duration has elapsed.
     */
    private static float slowProgress(Effect slow) {
        if (slow.authoredDurationTicks() <= 0) {
            return 1f;
        }
        float elapsed = slow.authoredDurationTicks() - slow.remainingTicks();
        return Math.max(0f, Math.min(1f, elapsed / slow.authoredDurationTicks()));
    }

    /** Speed recovers along a quadratic ease-in rather than snapping back at expiry. */
    private static float slowCurrentMultiplier(Effect slow) {
        float ratio = 1f - slow.speedMultiplier();
        float x = slowProgress(slow);
        return (1f - ratio) + ratio * (x * x);
    }

    public void apply(Effect effect) {
        switch (effect.kind()) {
            case SLOW -> this.applySlow(effect);
            case BURN -> this.applyBurn(effect);
            default -> {
                Effect existing = this.active.get(effect.kind());
                this.active.put(effect.kind(), existing == null ? effect : strongerOf(existing, effect));
            }
        }
    }

    /**
     * {@code SLOW} keeps a two-deep stack: the strongest application is active, and one superseded
     * application keeps its own clock in the background, resuming from where its curve has reached
     * once the winner expires. A newcomer that beats the winner bumps it into that slot; one that
     * loses competes for the slot, and the stronger stays.
     */
    private void applySlow(Effect incoming) {
        Effect winner = this.active.get(EffectKind.SLOW);
        if (winner == null) {
            this.active.put(EffectKind.SLOW, incoming);
            return;
        }
        if (magnitude(incoming) >= magnitude(winner)) {
            this.active.put(EffectKind.SLOW, incoming);
            this.slowSuperseded = winner;
        } else if (this.slowSuperseded == null || magnitude(incoming) >= magnitude(this.slowSuperseded)) {
            this.slowSuperseded = incoming;
        }
    }

    /**
     * {@code BURN} is an additive, decaying fuel pool. A reapplication adds its intensity, scaled
     * down the closer the pool is to its cap, and never changes the decay rate. Each contribution
     * keeps its own sink, so every contributing tower is credited for its share.
     */
    private void applyBurn(Effect incoming) {
        Effect existing = this.active.get(EffectKind.BURN);
        if (existing == null) {
            this.active.put(EffectKind.BURN, incoming);
            return;
        }
        float incomingL0 = incoming.damagePerTick().amount();
        float peakL0 = Math.max(existing.peakBurnL0(), incomingL0);
        float lmax = BURN_LMAX_MULTIPLIER * peakL0;
        float deltaL = incomingL0 * (1f - existing.fuelLevel() / lmax);
        List<BurnContribution> fuel = new ArrayList<>(existing.burnFuel());
        fuel.add(new BurnContribution(incoming.sink(), deltaL));
        this.active.put(EffectKind.BURN, existing.withBurnFuel(List.copyOf(fuel), peakL0));
    }

    /** Active kinds in enum order, as a snapshot, for UI markers. */
    public Set<EffectKind> activeKinds() {
        return this.active.isEmpty() ? EnumSet.noneOf(EffectKind.class) : EnumSet.copyOf(this.active.keySet());
    }

    /** Whether no effect is active, so the stats this object contributes cannot change on a tick. */
    public boolean isEmpty() {
        return this.active.isEmpty();
    }

    /**
     * Adds every active effect's stat modifiers: a slow multiplies speed at its point on the
     * recovery curve, a freeze sets it to zero, a shield lowers damage taken for the types it covers,
     * a heal adds regeneration, invisibility sets stealth and a burn doubles crit chance taken. Shields and heals go in as restorative,
     * so the enemy's spirit scales them.
     */
    public void contributeTo(StatAccumulator accumulator) {
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            Effect effect = entry.getValue();
            switch (entry.getKey()) {
                case SLOW -> accumulator.multiply(EnemyStat.MOVE_SPEED, slowCurrentMultiplier(effect));
                case FREEZE -> accumulator.add(EnemyStat.MOVE_SPEED, FROZEN);
                case SHIELD -> {
                    for (DamageType type : DAMAGE_TYPES) {
                        if (effect.shieldRestrictedTo().isEmpty() || effect.shieldRestrictedTo().get() == type) {
                            accumulator.restoreReduction(EnemyStat.damageTakenFor(type), effect.shieldPercent());
                        }
                    }
                }
                case HEAL -> accumulator.restoreFlat(EnemyStat.REGENERATION, effect.healPerTick());
                case INVISIBLE -> accumulator.add(EnemyStat.STEALTH, HIDDEN);
                case BURN -> accumulator.multiply(EnemyStat.CRIT_CHANCE_TAKEN, BURN_CRIT_CHANCE_TAKEN);
            }
        }
    }

    /**
     * Applies one tick of damage-over-time, then counts every duration down and removes what
     * expired. {@code BURN} decays instead of counting down, and the superseded {@code SLOW} ticks
     * in the background, taking over if the winner expires.
     */
    public void tick() {
        List<EffectKind> expired = new ArrayList<>();
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            EffectKind kind = entry.getKey();
            Effect effect = entry.getValue();
            if (kind == EffectKind.BURN) {
                Optional<Effect> decayed = this.tickBurn(effect);
                if (decayed.isPresent()) {
                    entry.setValue(decayed.get());
                } else {
                    expired.add(kind);
                }
                continue;
            }
            if (effect.damagePerTick().amount() > 0) {
                effect.sink().apply(effect.damagePerTick());
            }
            int remaining = effect.remainingTicks() - 1;
            if (remaining <= 0) {
                expired.add(kind);
            } else {
                entry.setValue(effect.withRemainingTicks(remaining));
            }
        }
        expired.forEach(this.active::remove);
        this.tickSlowSuperseded();
        if (expired.contains(EffectKind.SLOW) && this.slowSuperseded != null) {
            this.active.put(EffectKind.SLOW, this.slowSuperseded);
            this.slowSuperseded = null;
        }
    }

    /**
     * Deals the pool's rounded total, split by share, then decays every contribution by the same
     * {@code alpha}, which decays the total exactly. Ends once a tick would round to zero damage,
     * since exponential decay never reaches zero.
     */
    private Optional<Effect> tickBurn(Effect effect) {
        List<BurnContribution> contributions = effect.burnFuel();
        float total = effect.fuelLevel();
        int damage = Math.round(total);
        if (damage <= 0) {
            return Optional.empty();
        }
        int[] shares = apportionBurnDamage(contributions, total, damage);
        DamageType type = effect.damagePerTick().type();
        for (int i = 0; i < contributions.size(); i++) {
            if (shares[i] > 0) {
                contributions.get(i).sink().apply(new Damage(shares[i], type));
            }
        }
        double alpha = Math.exp(BURN_DECAY_EXPONENT / effect.authoredDurationTicks());
        List<BurnContribution> decayed = contributions.stream()
                .map(c -> c.decayedBy((float) alpha))
                .toList();
        return Optional.of(effect.withBurnFuel(decayed, effect.peakBurnL0()));
    }

    /**
     * Splits {@code roundedTotal} by share using largest-remainder apportionment, so the parts sum
     * exactly. Ties go to the earliest contribution, keeping runs reproducible.
     */
    private static int[] apportionBurnDamage(List<BurnContribution> contributions, float total, int roundedTotal) {
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

    /**
     * Ticks the superseded slow even when the winner expires this tick, so a promoted slow is never
     * one tick stale.
     */
    private void tickSlowSuperseded() {
        if (this.slowSuperseded == null) {
            return;
        }
        int remaining = this.slowSuperseded.remainingTicks() - 1;
        this.slowSuperseded = remaining <= 0 ? null : this.slowSuperseded.withRemainingTicks(remaining);
    }
}
