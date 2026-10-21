package td.tower.pulse;

import td.tower.upgrade.Perk;


/**
 * What one owned node does to the Pulse. A perk may change whom the field touches and how Toll
 * builds. Every hook does nothing by default, so a perk implements only its own.
 */
public interface PulsePerk extends Perk {

    default PulseSpec refineSpec(PulseSpec spec) {
        return spec;
    }

}
