package td.tower.upgrade;

import java.util.List;

/** An exclusive choice a tower has made: the node it bought and the members it locked out. */
public record UpgradeDecision(UpgradeNode chosen, List<UpgradeNode> passedOver) {

    public UpgradeDecision {
        passedOver = List.copyOf(passedOver);
    }
}
