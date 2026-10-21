package td.tower.pulse;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Pulse. A mechanic may change whom the field touches and how Toll
 * builds. Every hook does nothing by default, so a mechanic implements only its own.
 */
public interface PulseMechanic extends Mechanic {

    default PulseSpec refineSpec(PulseSpec spec) {
        return spec;
    }

}
