package td.tower.mortar;

import td.tower.BehaviourLine;

import java.util.Optional;

/**
 * What one owned node does to the Mortar. A perk reshapes the spec the tower reads whenever it looks
 * for a target or lands a shell; it does nothing by default, so a perk implements only its own.
 */
public interface MortarPerk {

    default MortarSpec refineSpec(MortarSpec spec) {
        return spec;
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
