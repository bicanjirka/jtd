package td.tower.upgrade;

import java.util.List;

/** A cluster's pick a tower has made: the member it holds and the members it passed over. */
public record UpgradeDecision(UpgradeNode chosen, List<UpgradeNode> passedOver) {

    public UpgradeDecision {
        passedOver = List.copyOf(passedOver);
    }
}
