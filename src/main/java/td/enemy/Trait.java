package td.enemy;

import td.damage.Damage;
import td.damage.DamageMix;
import td.effect.EffectKind;

import java.util.Optional;

/**
 * A passive, always-on modifier of an enemy or of hits against it. Every method but
 * {@link #marker()} defaults to no effect, so an enemy without traits needs no special case.
 * {@link #marker()} has no default so a new trait cannot compile without a glyph.
 * <p>
 * One instance is shared by every mob of a definition; per-mob inputs arrive in
 * {@link TraitContext}. Invisibility does not go through {@link #isValidTarget}: it is an effect.
 */
public interface Trait extends TraitTemplate {

    /** Already resolved: a fixed trait is the same for every mob. */
    @Override
    default Optional<Trait> resolvedFor(DamageMix mix) {
        return Optional.of(this);
    }

    /** Resists or otherwise modifies an incoming hit. */
    default Damage onHit(Damage incoming, TraitContext context) {
        return incoming;
    }

    /** Whether this trait alone makes the mob untargetable. */
    default boolean isValidTarget(TraitContext context) {
        return true;
    }

    /** A multiplier on intrinsic speed. */
    default float speedFactor(TraitContext context) {
        return 1f;
    }

    /**
     * Whether this trait rejects an incoming status effect of {@code kind} before it is applied.
     */
    default boolean blocksEffect(EffectKind kind) {
        return false;
    }

    /** The glyph shown for this trait in the marker row below the mob. */
    TraitMarker marker();
}
