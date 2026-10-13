package td.tower.aura;

import td.tower.BehaviourLine;

import java.util.Optional;

/** What one owned node does to the Aura; nothing by default. */
public interface AuraPerk {

    default AuraSpec refineSpec(AuraSpec spec) {
        return spec;
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
