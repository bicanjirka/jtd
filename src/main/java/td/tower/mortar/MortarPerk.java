package td.tower.mortar;

/**
 * What one owned node does to the Mortar. A perk reshapes the spec the tower reads whenever it looks
 * for a target or lands a shell; it does nothing by default, so a perk implements only its own.
 */
public interface MortarPerk {

    default MortarSpec refineSpec(MortarSpec spec) {
        return spec;
    }
}
