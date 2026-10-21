package td.tower.aura;

import td.tower.upgrade.Perk;


/** What one owned node does to the Aura; nothing by default. */
public interface AuraPerk extends Perk {

    default AuraSpec refineSpec(AuraSpec spec) {
        return spec;
    }

}
