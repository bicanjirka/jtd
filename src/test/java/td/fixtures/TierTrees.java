package td.fixtures;

import td.tower.Tower;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

/**
 * A small tree, on tiers whose first gate also asks a payment, that the upgrade tests share: two roots in a cluster with a line behind each, a
 * lone node in tier 0 and one in tier I, a cluster of two in tier II, and the top of the first root's line in tier III.
 */
public final class TierTrees {

    /** Tiers whose first gate asks a payment on top of its XP. */
    public static final List<UpgradeTier> PAID_TIERS = List.of(UpgradeTier.open(), UpgradeTier.gated(60, 20),
            UpgradeTier.gated(120, 0), UpgradeTier.gated(200, 0));

    public static final UpgradeNode A1 = UpgradeNode.of("a.1", "A", 10);
    public static final UpgradeNode B1 = UpgradeNode.of("b.1", "B", 10);
    public static final UpgradeNode LONE = UpgradeNode.of("lone", "Lone", 8);
    public static final UpgradeNode A2 = UpgradeNode.of("a.2", "A II", 15).inTier(1).after(A1);
    public static final UpgradeNode B2 = UpgradeNode.of("b.2", "B II", 15).inTier(1).after(B1);
    public static final UpgradeNode MID = UpgradeNode.of("mid", "Mid", 12).inTier(1);
    public static final UpgradeNode A3 = UpgradeNode.of("a.3", "A III", 20).inTier(2).after(A2);
    public static final UpgradeNode S1 = UpgradeNode.of("s.1", "S1", 25).inTier(2);
    public static final UpgradeNode S2 = UpgradeNode.of("s.2", "S2", 25).inTier(2);
    public static final UpgradeNode TOP = UpgradeNode.of("top", "Top", 30).inTier(3).after(A3);
    public static final UpgradeCluster ROOTS = UpgradeCluster.of("Roots", A1, B1);
    public static final UpgradeCluster SPECIALS = UpgradeCluster.of("Specials", S1, S2);

    public static final UpgradeTree TREE = UpgradeTree.of(A1, B1, LONE, A2, B2, MID, A3, S1, S2, TOP)
            .withCluster(ROOTS).withCluster(SPECIALS).withTiers(TierTrees.PAID_TIERS);

    /** Tier I's unlock. */
    public static final UpgradeNode TIER_ONE = TREE.unlockOf(1).orElseThrow();
    /** Enough XP for every tier. */
    public static final int ALL_XP = 1_000;

    private TierTrees() {
    }

    public static UpgradeState state(UpgradeNode... owned) {
        return new UpgradeState(List.of(owned));
    }

    public static Tower towerOwning(GameWorld world, int xp, UpgradeNode... owned) {
        Tower tower = FakeTower.owning(world, 0, 0, state(owned));
        tower.earnXp(xp);
        return tower;
    }
}
