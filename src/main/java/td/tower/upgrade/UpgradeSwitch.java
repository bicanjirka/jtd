package td.tower.upgrade;

import java.util.List;

/**
 * What switching a cluster or a fork from {@code from} to {@code to} takes: {@code to} takes {@code from}'s place
 * for nothing, and what came after {@code from} is sold back. {@code kept} is what the tower owns after.
 */
public record UpgradeSwitch(UpgradeNode from, UpgradeNode to, List<UpgradeNode> sold, UpgradeState kept) {

    public UpgradeSwitch {
        sold = List.copyOf(sold);
    }
}
