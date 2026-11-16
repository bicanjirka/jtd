package td.tower.aura;

import td.tower.upgrade.Mechanic;

/** What one owned node does to the Beacon's light; nothing by default. */
public interface AuraMechanic extends Mechanic {

    default LightSpec refineSpec(LightSpec spec) {
        return spec;
    }
}
