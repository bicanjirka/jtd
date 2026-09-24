package td.effect;

import td.damage.Damage;
import td.damage.DamageType;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The effects currently active on one enemy, keyed by {@link EffectKind} - at most one of each
 * is ever active at a time, with one deliberate exception: {@code SLOW} additionally keeps a
 * single superseded application in the background (see {@link #applySlow}). Re-applying any
 * other kind already active keeps the stronger of the two (by whichever magnitude that kind
 * means - see {@link #magnitude}) but always extends the remaining duration to the longer of
 * the two, so a weaker top-up never cuts short a stronger effect already in progress, and a
 * later, longer application at the same strength is never wasted. {@code SLOW} and {@code BURN}
 * each combine differently from this baseline and from each other - see {@link #applySlow} and
 * {@link #applyBurn}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ActiveEffects {

    /**
     * Burn's fuel pool cap, as a multiple of the strongest single application's own intensity
     * this mob has ever taken - see {@link #applyBurn}.
     */
    private static final float BURN_LMAX_MULTIPLIER = 2f;
    /**
     * Burn's per-tick decay exponent: {@code alpha = e^(BURN_DECAY_EXPONENT / authoredDurationTicks)}
     * reaches {@code e^-3 ≈ 4.98%} of the original fuel right around when the authored duration
     * would have elapsed under the old flat model - see {@link #tickBurn}.
     */
    private static final double BURN_DECAY_EXPONENT = -3.0;

    private final Map<EffectKind, Effect> active = new EnumMap<>(EffectKind.class);
    private Effect slowSuperseded;

    private static Effect strongerOf(Effect a, Effect b) {
        Effect stronger = magnitude(a) >= magnitude(b) ? a : b;
        return stronger.withRemainingTicks(Math.max(a.remainingTicks(), b.remainingTicks()));
    }

    /**
     * How hard this kind of effect bites, in a unit specific to its kind - only ever compared
     * against another effect of the same kind, so the different units across cases are safe.
     */
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
     * Where {@code slow} sits in its own life, {@code [0, 1]} - {@code 0} at the moment it was
     * applied, {@code 1} once its authored duration has fully elapsed. Feeds the quadratic
     * ease-in recovery curve in {@link #slowCurrentMultiplier} - see {@code Effect.authoredDurationTicks}.
     */
    private static float slowProgress(Effect slow) {
        if (slow.authoredDurationTicks() <= 0) {
            return 1f;
        }
        float elapsed = slow.authoredDurationTicks() - slow.remainingTicks();
        return Math.max(0f, Math.min(1f, elapsed / slow.authoredDurationTicks()));
    }

    /**
     * A slowed enemy's speed recovers along a quadratic ease-in curve rather than snapping back
     * to full speed at expiry - barely noticeable at first, steepest right before it ends. See
     * {@code docs/features/FEATURE-effect-diminishing-returns.md}'s worked table.
     */
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
     * {@code SLOW} keeps a bounded 2-deep stack instead of collapsing a reapplication into one
     * blended scalar: the winning (highest-magnitude) application stays active, and at most one
     * superseded application keeps running its own clock in the background, resuming as the
     * active winner - from wherever its own curve has actually reached, not from scratch - once
     * the winner's authored duration elapses. A new application that beats the current winner
     * bumps it into the (single) superseded slot, dropping whatever was there before; one that
     * loses to the winner is compared against the existing superseded slot the same way, and
     * only the stronger of the two survives there. See {@code td/effect/CLAUDE.md}.
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
     * {@code BURN} is an additive, decaying fuel pool rather than a max-magnitude pick: a
     * reapplication tops up the existing pool by its own intensity, scaled down the closer the
     * pool already is to {@code Lmax} - a pool near the cap barely grows from another
     * application, an empty or low one grows close to the new application's full intensity. The
     * pool's decay rate ({@code authoredDurationTicks}, read by {@link #tickBurn}) is kept from
     * whichever application is already active - a reapplication only ever adds one more
     * {@link BurnContribution}, never changes the decay rate. Each reapplication's own
     * {@link DamageSink} is kept alongside its share of the pool rather than displacing the
     * existing sink, which is what lets {@link #tickBurn} credit every contributing tower for
     * its own share instead of crediting whichever tower happened to apply first. See
     * {@code td/effect/CLAUDE.md}.
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

    /**
     * Which kinds are currently active, in a stable (enum-declaration) order - for a UI marker
     * to key off, not for resolving anything. A snapshot: later changes to this holder don't
     * retroactively affect a set already handed out.
     */
    public Set<EffectKind> activeKinds() {
        return this.active.isEmpty() ? EnumSet.noneOf(EffectKind.class) : EnumSet.copyOf(this.active.keySet());
    }

    /**
     * The product of every active effect's speed multiplier - {@code 1f} (unaffected) with none
     * active. {@code SLOW} reads its current point on the recovery curve rather than its flat
     * authored value; every other kind (including {@code FREEZE}, which stays a hard,
     * non-gradated stop) reads its stored value directly, unchanged.
     */
    public float speedMultiplier() {
        float multiplier = 1f;
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            Effect effect = entry.getValue();
            multiplier *= entry.getKey() == EffectKind.SLOW ? slowCurrentMultiplier(effect) : effect.speedMultiplier();
        }
        return multiplier;
    }

    /**
     * Not a valid target while an {@link EffectKind#INVISIBLE} effect is active.
     */
    public boolean isInvisible() {
        return this.active.containsKey(EffectKind.INVISIBLE);
    }

    /**
     * Reduces {@code incoming} by the active {@link EffectKind#SHIELD} effect's percentage,
     * unreduced if none is active, or if the active shield is restricted to a
     * {@link td.damage.DamageType} {@code incoming} doesn't carry - the effect-side counterpart
     * to a {@code Trait}'s own, permanent {@code onHit} resistance, which this composes with
     * rather than replaces.
     */
    public Damage applyShield(Damage incoming) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        if (shield == null) {
            return incoming;
        }
        if (shield.shieldRestrictedTo().isPresent() && shield.shieldRestrictedTo().get() != incoming.type()) {
            return incoming;
        }
        return incoming.scaledBy(1f - shield.shieldPercent());
    }

    /**
     * How much health the active {@link EffectKind#HEAL} effect restores this tick, {@code 0}
     * with none active - the effect-side counterpart to {@link #applyShield}, a query rather
     * than a mutation, since restoring health is the mob's own {@code health} field to touch,
     * not this holder's.
     */
    public int healPerTick() {
        Effect heal = this.active.get(EffectKind.HEAL);
        return heal == null ? 0 : heal.healPerTick();
    }

    /**
     * Applies one tick of every active damage-over-time effect through its own sink, then
     * decrements every active effect's remaining duration, removing any that just expired.
     * {@code BURN} instead runs its own fuel-pool decay (see {@link #tickBurn}), which
     * self-terminates rather than expiring on a duration countdown. The superseded {@code SLOW}
     * application, if any, ticks its own clock in the background regardless of what happens to
     * the active winner, and is promoted into its place if the winner expires this tick.
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
     * One tick of {@code BURN}'s fuel-pool model: deals the pool's current rounded total, split
     * across every contributing tower in proportion to its own current share (see
     * {@link #apportionBurnDamage}), then decays every contribution by
     * {@code alpha = e^(BURN_DECAY_EXPONENT / authoredDurationTicks)} - the currently-active
     * application's own duration, kept fixed across any later top-up (see {@link #applyBurn}).
     * Decay is multiplicative, so decaying each contribution individually by the same
     * {@code alpha} leaves the pool's total exactly where decaying the pooled total once would
     * have. Empty once a tick's rounded damage would be zero - the pool's own termination floor,
     * since the exponential decay never reaches exactly zero on its own.
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
     * Splits {@code roundedTotal} across {@code contributions} in proportion to each one's own
     * share of {@code total}, using largest-remainder apportionment so the parts always sum to
     * exactly {@code roundedTotal} - with one contributor this reduces to
     * {@code roundedTotal} itself, exactly today's single-sink behaviour. Each contribution
     * first gets the floor of its exact share; whatever is left over goes to the largest
     * fractional remainders, earliest contribution first on a tie, so the result never depends
     * on iteration order alone and stays reproducible for {@code td.BalanceHarness}.
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
     * The superseded {@code SLOW} application's own clock, ticking down in the background
     * regardless of whether the active winner expires this same tick - so a promoted
     * application resumes its curve from wherever it has actually reached, not from a point one
     * tick stale. See {@link #applySlow}.
     */
    private void tickSlowSuperseded() {
        if (this.slowSuperseded == null) {
            return;
        }
        int remaining = this.slowSuperseded.remainingTicks() - 1;
        this.slowSuperseded = remaining <= 0 ? null : this.slowSuperseded.withRemainingTicks(remaining);
    }
}
