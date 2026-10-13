package td.tower.seeker;

import td.enemy.EnemyMob;
import td.tower.BehaviourLine;
import td.tower.upgrade.Perk;

import java.util.Optional;

/**
 * What one owned node does to the Seeker. A perk may change whom it may fire at, whom it picks, what
 * its nest holds, how many missiles a launch sends and what they carry, scale the hit a missile
 * lands, and react to how it landed. Every hook does nothing by default, so a perk implements only
 * its own.
 */
public interface SeekerPerk extends Perk {

    default SeekerSpec refineSpec(SeekerSpec spec) {
        return spec;
    }

    /** A multiple of the damage of the hit a missile is about to land on {@code target}. */
    default float damageFactor(EnemyMob target) {
        return 1f;
    }

    default void react(Impact impact, SeekerActions actions) {
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
