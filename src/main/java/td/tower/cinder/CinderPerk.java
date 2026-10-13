package td.tower.cinder;

import td.tower.BehaviourLine;

import java.util.Optional;

/**
 * What one owned node does to the Cinder. A perk reshapes the spec the tower reads whenever it aims
 * or fires; it does nothing by default, so a perk implements only its own.
 */
public interface CinderPerk {

    default CinderSpec refineSpec(CinderSpec spec) {
        return spec;
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
