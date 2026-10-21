package td.tower.aura;

import td.tower.upgrade.Mechanic;


/** What one owned node does to the Aura; nothing by default. */
public interface AuraMechanic extends Mechanic {

    default AuraSpec refineSpec(AuraSpec spec) {
        return spec;
    }

}
