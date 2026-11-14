package td.ui;

import td.term.TermNumbers;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.IconFace;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

/**
 * One node of the tree as its hover describes it, read once per refresh.
 *
 * @param tierOpen whether the node's tier is open, or for a tier's unlock, whether its gate is met
 * @param tierNeed what opens the tier while it isn't, e.g. {@code "Tier I · XP 40 / 60"}; empty once it is
 * @param cluster  the name of the cluster or fork it is a member of, empty outside one
 * @param rivals   the other members of its cluster, empty outside one
 * @param words    what it adds to the tower: its stat rows, then its sentences
 * @param face     the icon of the line it belongs to, at the level it brings the line to
 * @param addsTo   the name of the line's first node when this deepens a line, else empty
 * @param unlock   whether it is a tier's unlock rather than an upgrade
 */
record UpgradeOffer(UpgradeNode node, boolean tierOpen, String tierNeed, Optional<String> cluster,
                    List<UpgradeNode> rivals, NodeWords words, IconFace face, Optional<String> addsTo, boolean unlock) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
    }

    /** {@code node} of {@code tower}'s tree, as it stands for the tower now. */
    static UpgradeOffer of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        boolean unlock = tree.isUnlock(node);
        int tier = node.tier();
        boolean open = unlock ? tree.isOpen(tier - 1, tower, world) && tree.xpMet(tier, tower, world)
                : tree.isOpen(tier, tower, world);
        Optional<UpgradeCluster> cluster = tree.clusterOf(node);
        List<UpgradeNode> rivals = cluster.map(c -> c.memberIds().stream().filter(id -> !id.equals(node.id()))
                .map(id -> tree.node(id).orElseThrow()).toList()).orElse(List.of());
        int level = tree.levelOf(node);
        return new UpgradeOffer(node, open, open ? "" : tierNeed(tree, tier, unlock ? tier : tier + 1, tower, world),
                cluster.map(UpgradeCluster::name), rivals, tower.wordsOf(node),
                TermLooks.faceOfLine(tower.getType(), tree.artNodeOf(node), level),
                level > 1 ? Optional.of(tree.firstOf(node).displayName()) : Optional.empty(), unlock);
    }

    /**
     * The lowest tier up to {@code tier} that stays closed, and what it lacks: its XP, or its unlock below
     * {@code unlocksBelow}; a tier's own unlock doesn't wait for itself.
     */
    private static String tierNeed(UpgradeTree tree, int tier, int unlocksBelow, Tower tower, GameWorld world) {
        for (int t = 1; t <= tier; t++) {
            if (!tree.xpMet(t, tower, world)) {
                return UpgradeTier.nameOf(t) + " · XP " + tower.experience().xp() + " / " + tree.tiers().get(t).xp();
            }
            Optional<UpgradeNode> unlock = tree.unlockOf(t);
            if (t < unlocksBelow && unlock.isPresent() && !tree.isPaidOpen(t, tower.upgrades())) {
                return UpgradeTier.nameOf(t) + " · unlock " + TermNumbers.cash(unlock.get().price());
            }
        }
        return "";
    }

    /** Whether it is one of a cluster's members. */
    boolean inCluster() {
        return !this.rivals.isEmpty();
    }
}
