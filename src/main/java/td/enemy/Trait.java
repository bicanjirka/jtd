package td.enemy;

import td.damage.Damage;

/**
 * A passive, always-on modifier of an enemy's properties or of incoming interactions -
 * generalizes what today is a hand-written override on one of the five leaf enemy classes
 * (see {@code EnemyMobSquare.absorb}, {@code EnemyMobGhost}'s invisibility,
 * {@code EnemyMobTriangle}'s hurt-speed curve). Every method has a no-op default, so a
 * definition with no traits at all (the migrated Circle's case) needs no special-cased
 * "no traits" branch anywhere that reads one - an empty {@code List<Trait>} already behaves
 * exactly like today's absence of any override.
 */
public interface Trait {

    /** Resists (or otherwise modifies) an incoming hit - generalizes {@code absorb}. */
    default Damage onHit(Damage incoming) {
        return incoming;
    }

    /** Whether this trait alone makes the mob permanently untargetable - generalizes Ghost's invisibility. */
    default boolean isValidTarget() {
        return true;
    }

    /** A multiplier on intrinsic speed as a function of current health fraction - generalizes Triangle's hurt curve. */
    default float speedFactor(float healthFraction) {
        return 1f;
    }
}
