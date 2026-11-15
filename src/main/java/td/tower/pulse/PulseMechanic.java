package td.tower.pulse;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Obelisk. A mechanic reshapes the spec the tower reads every tick; it does nothing by
 * default, so a mechanic implements only its own.
 */
public interface PulseMechanic extends Mechanic {

    default PulseSpec refineSpec(PulseSpec spec) {
        return spec;
    }

}
