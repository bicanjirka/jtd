package td.ui;

import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where every node of a tree sits on the Upgrades panel, in design units of a {@value #WIDTH} by
 * {@value #HEIGHT} panel, derived from what the tree already says: Core climbs the rook's wall by level,
 * Base's Range line runs along its foundation, a Head line takes the row of its key (4, 5, 3 from the top)
 * with its level as column, an exclusive pair of level IVs forks, and specials sit centred on the halo in
 * the order they are declared. No tower has layout of its own. Pure.
 *
 * @param spots      each node's place
 * @param links      a line's node to the next, a forked level IV excluded
 * @param forks      a line's level III to its exclusive pair of level IVs
 * @param rootChoice the head roots that exclude each other, meeting at a diamond; empty for none
 */
record UpgradeTreeLayout(Map<UpgradeNode, Spot> spots, List<Link> links, List<Fork> forks,
                         Optional<RootChoice> rootChoice) {

    static final int WIDTH = 300;
    static final int HEIGHT = 420;
    static final float NODE = 32f;
    static final float SPECIAL_NODE = 28f;
    static final float SPINE_X = 24f;
    static final float HALO_X = 172f;
    static final float HALO_Y = 66f;
    static final float HALO_RX = 112f;
    static final float HALO_RY = 18f;
    static final float FOOTER_Y = 404f;

    private static final int MAX_SPECIALS = 6;
    private static final float[] COLUMNS = {0f, 92f, 142f, 192f, 252f};
    private static final float[] SPINE = {0f, 368f, 318f, 110f};
    private static final float RANGE_ROW = 368f;
    private static final Map<Integer, Float> HEAD_ROWS = Map.of(4, 148f, 5, 220f, 3, 288f);
    private static final float FORK_SPREAD = 19f;
    private static final float FORK_X = 224f;
    private static final float ROOT_CHOICE_X = 60f;
    private static final float SPECIAL_SPACING = 36f;

    /** One node's centre and size; a Core step and a special are round. */
    record Spot(float x, float y, float size, boolean round) {
    }

    record Link(UpgradeNode from, UpgradeNode to) {
    }

    /** Where {@code from}'s line splits toward {@code members}. */
    record Fork(UpgradeNode from, float x, float y, List<UpgradeNode> members) {

        Fork {
            members = List.copyOf(members);
        }
    }

    /** The diamond the exclusive head roots meet at. */
    record RootChoice(float x, float y, List<UpgradeNode> members) {

        RootChoice {
            members = List.copyOf(members);
        }
    }

    UpgradeTreeLayout {
        spots = Map.copyOf(spots);
        links = List.copyOf(links);
        forks = List.copyOf(forks);
    }

    /**
     * {@code tree}'s layout.
     *
     * @throws IllegalArgumentException for a tree whose shape doesn't fit the panel, naming what doesn't
     */
    static UpgradeTreeLayout of(UpgradeTree tree) {
        Map<UpgradeNode, Spot> spots = new LinkedHashMap<>();
        List<Link> links = new ArrayList<>();
        List<Fork> forks = new ArrayList<>();
        List<UpgradeNode> specials = tree.nodesIn(UpgradeSlot.SPECIAL);
        if (specials.size() > MAX_SPECIALS) {
            throw new IllegalArgumentException("More specials than the halo holds: " + specials.size());
        }
        for (UpgradeNode node : tree.nodes()) {
            spots.put(node, switch (node.slot()) {
                case CORE -> new Spot(SPINE_X, SPINE[node.tier().level()], NODE, true);
                case BASE -> new Spot(COLUMNS[node.tier().level()], RANGE_ROW, NODE, false);
                case HEAD -> headSpot(tree, node);
                case SPECIAL -> specialSpot(specials.indexOf(node), specials.size());
            });
        }
        for (UpgradeNode node : tree.nodes()) {
            Optional<UpgradeNode> previous = previousInLine(tree, node);
            Optional<ExclusiveChoice> pair = levelFourPair(tree, node);
            if (previous.isEmpty()) {
                continue;
            }
            if (pair.isEmpty()) {
                links.add(new Link(previous.get(), node));
            } else if (pair.get().nodeIds().indexOf(node.id()) == 0) {
                forks.add(new Fork(previous.get(), FORK_X, spots.get(previous.get()).y(),
                        pair.get().nodeIds().stream().map(id -> tree.node(id).orElseThrow()).toList()));
            }
        }
        rejectShared(spots);
        return new UpgradeTreeLayout(spots, links, forks, rootChoice(tree, spots));
    }

    Optional<Spot> spotOf(UpgradeNode node) {
        return Optional.ofNullable(this.spots.get(node));
    }

    private static Spot headSpot(UpgradeTree tree, UpgradeNode node) {
        Float row = HEAD_ROWS.get(tree.keyOf(tree.firstOf(node)));
        if (row == null) {
            throw new IllegalArgumentException("No head row for upgrade node: " + node.id());
        }
        Optional<ExclusiveChoice> pair = levelFourPair(tree, node);
        float y = pair.map(choice -> choice.nodeIds().indexOf(node.id()) == 0 ? row - FORK_SPREAD : row + FORK_SPREAD)
                .orElse(row);
        return new Spot(COLUMNS[node.tier().level()], y, NODE, false);
    }

    /** Centred on the halo's front, following its curve. */
    private static Spot specialSpot(int index, int count) {
        float x = HALO_X + (index - (count - 1) / 2f) * SPECIAL_SPACING;
        float across = (x - HALO_X) / HALO_RX;
        float y = HALO_Y + HALO_RY * (float) Math.sqrt(Math.max(0f, 1f - across * across));
        return new Spot(x, y, SPECIAL_NODE, true);
    }

    /** The choice of two level IVs {@code node} is one of; empty for any other node. */
    private static Optional<ExclusiveChoice> levelFourPair(UpgradeTree tree, UpgradeNode node) {
        if (node.tier().level() != UpgradeTier.HEAD_4.level() || node.slot() != UpgradeSlot.HEAD) {
            return Optional.empty();
        }
        return tree.choiceOf(node).filter(choice -> choice.nodeIds().size() == 2);
    }

    private static Optional<UpgradeNode> previousInLine(UpgradeTree tree, UpgradeNode node) {
        return node.tier().previous().flatMap(previous -> tree.nodes().stream()
                .filter(candidate -> candidate.tier() == previous && node.requires().requiredNodes().contains(candidate.id()))
                .findFirst());
    }

    private static Optional<RootChoice> rootChoice(UpgradeTree tree, Map<UpgradeNode, Spot> spots) {
        return tree.choices().stream()
                .map(choice -> choice.nodeIds().stream().map(id -> tree.node(id).orElseThrow()).toList())
                .filter(members -> members.stream().allMatch(m -> m.slot() == UpgradeSlot.HEAD && m.tier().level() == 1))
                .findFirst()
                .map(members -> new RootChoice(ROOT_CHOICE_X, (float) members.stream()
                        .mapToDouble(m -> spots.get(m).y()).average().orElse(0), members));
    }

    private static void rejectShared(Map<UpgradeNode, Spot> spots) {
        Map<Spot, UpgradeNode> taken = new HashMap<>();
        spots.forEach((node, spot) -> {
            UpgradeNode other = taken.putIfAbsent(spot, node);
            if (other != null) {
                throw new IllegalArgumentException(
                        "Upgrade nodes " + other.id() + " and " + node.id() + " share a place on the panel");
            }
        });
    }
}
