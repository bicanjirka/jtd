package td.tower.aura;

/** What one owned node does to the Aura; nothing by default. */
public interface AuraPerk {

    default AuraSpec refineSpec(AuraSpec spec) {
        return spec;
    }
}
