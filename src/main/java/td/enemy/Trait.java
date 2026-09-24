package td.enemy;

import td.damage.DamageMix;
import td.stat.StatModifiers;

import java.util.Optional;

/**
 * A passive, always-on modifier of an enemy's stats. Every method but {@link #marker()} defaults to
 * no effect, so an enemy without traits needs no special case. {@link #marker()} has no default so
 * a new trait cannot compile without a glyph.
 * <p>
 * One instance is shared by every mob of a definition; per-mob inputs arrive in
 * {@link TraitContext}.
 */
public interface Trait extends TraitTemplate {

    /** Already resolved: a fixed trait is the same for every mob. */
    @Override
    default Optional<Trait> resolvedFor(DamageMix mix) {
        return Optional.of(this);
    }

    /** What this trait adds to the mob's stat sheet; re-read whenever the sheet resolves. */
    default StatModifiers modifiers(TraitContext context) {
        return StatModifiers.none();
    }

    /** The glyph shown for this trait in the marker row below the mob. */
    TraitMarker marker();
}
