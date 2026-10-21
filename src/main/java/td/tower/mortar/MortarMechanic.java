package td.tower.mortar;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Mortar. A mechanic reshapes the spec the tower reads whenever it looks
 * for a target or lands a shell; it does nothing by default, so a mechanic implements only its own.
 */
public interface MortarMechanic extends Mechanic {

    default MortarSpec refineSpec(MortarSpec spec) {
        return spec;
    }

}
