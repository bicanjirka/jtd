package td.tower.pulse;

import td.tower.BehaviourLine;
import td.tower.upgrade.Perk;

import java.util.Optional;

/**
 * What one owned node does to the Pulse. A perk may change whom the field touches and how Toll
 * builds. Every hook does nothing by default, so a perk implements only its own.
 */
public interface PulsePerk extends Perk {

    default PulseSpec refineSpec(PulseSpec spec) {
        return spec;
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
