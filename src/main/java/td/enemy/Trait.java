package td.enemy;

import td.damage.Damage;

/**
 * A passive, always-on modifier of an enemy's properties or of incoming interactions -
 * generalizes what used to be a hand-written override on one of the five now-retired leaf
 * enemy classes (see {@link PercentResistTrait}/{@link HurtSpeedTrait}, which reproduce
 * Square's old resistance and Triangle's old hurt-speed curve). Every method has a no-op
 * default, so a definition with no traits at all (Circle's case) needs no special-cased
 * "no traits" branch anywhere that reads one - an empty {@code List<Trait>} already behaves
 * exactly like the old absence of any override.
 * <p>
 * A single {@link Trait} instance is shared by every mob built from the same
 * {@link EnemyDefinition}, regardless of which wave's level spawned it - {@link TraitContext}
 * is what lets {@link PercentResistTrait}/{@link HurtSpeedTrait} (Square's resistance,
 * Triangle's hurt curve) stay level-scaled without needing a fresh instance per mob.
 * <p>
 * Note: Ghost's invisibility does <b>not</b> route through {@link #isValidTarget} - single-
 * target towers filter by {@link EnemyMob.type} (see {@code td.tower.targeting.OfTypeTargetQuery}/
 * {@code InRangeTargetQuery.ofType}), a separate, pre-existing mechanism {@link EnemyDefinition#mobType()}
 * feeds directly. {@link #isValidTarget} stays available for a future trait that makes a mob
 * untargetable through some other means.
 */
public interface Trait {

    /** Resists (or otherwise modifies) an incoming hit - generalizes {@code absorb}. */
    default Damage onHit(Damage incoming, TraitContext context) {
        return incoming;
    }

    /** Whether this trait alone makes the mob permanently untargetable, independent of {@link EnemyMob.type}. */
    default boolean isValidTarget(TraitContext context) {
        return true;
    }

    /** A multiplier on intrinsic speed - generalizes Triangle's hurt curve. */
    default float speedFactor(TraitContext context) {
        return 1f;
    }
}
