package td.effect;

import td.damage.Damage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The effects currently active on one enemy, keyed by {@link EffectKind} so at most one of
 * each is ever active at a time. Re-applying a kind already active keeps the stronger of the
 * two (by whichever magnitude that kind means - see {@link #magnitude}) but always extends
 * the remaining duration to the longer of the two, so a weaker top-up never cuts short a
 * stronger effect already in progress, and a later, longer application at the same strength
 * is never wasted.
 */
public final class ActiveEffects {

    private final Map<EffectKind, Effect> active = new EnumMap<>(EffectKind.class);

    public void apply(Effect effect) {
        Effect existing = this.active.get(effect.kind());
        this.active.put(effect.kind(), existing == null ? effect : strongerOf(existing, effect));
    }

    /**
     * Which kinds are currently active, in a stable (enum-declaration) order - for a UI marker
     * to key off, not for resolving anything. A snapshot: later changes to this holder don't
     * retroactively affect a set already handed out.
     */
    public Set<EffectKind> activeKinds() {
        return this.active.isEmpty() ? EnumSet.noneOf(EffectKind.class) : EnumSet.copyOf(this.active.keySet());
    }

    /** The product of every active effect's speed multiplier - {@code 1f} (unaffected) with none active. */
    public float speedMultiplier() {
        float multiplier = 1f;
        for (Effect effect : this.active.values()) {
            multiplier *= effect.speedMultiplier();
        }
        return multiplier;
    }

    /** Not a valid target while an {@link EffectKind#INVISIBLE} effect is active. */
    public boolean isInvisible() {
        return this.active.containsKey(EffectKind.INVISIBLE);
    }

    /**
     * Reduces {@code incoming} by the active {@link EffectKind#SHIELD} effect's percentage,
     * unreduced if none is active - the effect-side counterpart to a {@code Trait}'s own,
     * permanent {@code onHit} resistance, which this composes with rather than replaces.
     */
    public Damage applyShield(Damage incoming) {
        Effect shield = this.active.get(EffectKind.SHIELD);
        return shield == null ? incoming : incoming.scaledBy(1f - shield.shieldPercent());
    }

    /**
     * Applies one tick of every active damage-over-time effect through its own sink, then
     * decrements every active effect's remaining duration, removing any that just expired.
     */
    public void tick() {
        List<EffectKind> expired = new ArrayList<>();
        for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
            Effect effect = entry.getValue();
            if (effect.damagePerTick().amount() > 0) {
                effect.sink().apply(effect.damagePerTick());
            }
            int remaining = effect.remainingTicks() - 1;
            if (remaining <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(effect.withRemainingTicks(remaining));
            }
        }
        expired.forEach(this.active::remove);
    }

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
        };
    }
}
