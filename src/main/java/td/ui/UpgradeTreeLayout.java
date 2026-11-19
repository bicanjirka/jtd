package td.ui;

import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Where every node of a tree sits on the Upgrades panel, in design units {@value #WIDTH} wide, from the tower's
 * hand-made {@link UpgradeTreePlan}: a band per tier, bottom up, each one or more rows of nodes over a floor strip, with
 * its numeral in a strip at its left. A link leaves the top of what a node needs, runs in the channel above that row
 * and climbs into the node's bottom; a cluster's frame stands in for its members where the rule names the cluster or
 * all of them share one need. A need more than a tier below is a badge on a top corner instead. Nodes that come after
 * different members of one cluster may share a slot, since only one of them can ever be bought: the slot shows the
 * node of the member picked, or hovered, and stands empty before either. Pure.
 *
 * @param bands  every tier's band, tier 0 first
 * @param spots  each node's place
 * @param frames a cluster's frame around its members
 * @param links  a need to what needs it, as a path
 * @param badges a need more than a tier below, on the corner of what needs it
 * @param shared every slot more than one node shares
 * @param height the whole design's height, the footer included
 */
record UpgradeTreeLayout(List<Band> bands, Map<UpgradeNode, Spot> spots, List<Frame> frames, List<Link> links,
                         List<Badge> badges, List<Shared> shared, float height) {

    static final int WIDTH = 300;
    /** The design height the panel asks for: five one-row tiers fit it. */
    static final int HEIGHT = 380;
    static final float NODE = 30f;
    static final float PITCH = 37f;
    static final float ROW = 46f;
    static final float STRIP = 15f;
    static final float BAND_X = 6f;
    static final float NUMERAL_WIDTH = 16f;
    static final float BODY_X = BAND_X + NUMERAL_WIDTH + 2f;
    static final float FOOTER = 16f;
    static final float FRAME_PAD = 4f;
    static final float BADGE = 6f;

    private static final float TOP = 4f;
    private static final float GAP = 6f;
    private static final float FIRST_SLOT = BODY_X + 4f + NODE / 2;
    /** From a row's top to its nodes' centre: the channel a link runs in sits above the nodes. */
    private static final float NODE_DROP = 24f;
    /** A frame's name, over its top edge, that an outgoing link keeps clear of: roughly its width per letter. */
    private static final float LABEL_LETTER = 4.6f;
    private static final float LABEL_PAD = 10f;

    /** A tier's band: its rows over its floor strip. */
    record Band(int tier, float top, float bottom) {

        float stripTop() {
            return this.bottom - STRIP;
        }
    }

    /** One node's centre, size, and the top of the row it stands in. */
    record Spot(float x, float y, float size, float rowTop) {

        boolean contains(float px, float py, float slack) {
            return Math.abs(px - this.x) <= this.size / 2 + slack && Math.abs(py - this.y) <= this.size / 2 + slack;
        }
    }

    /** A box by its edges. */
    record Box(float left, float top, float right, float bottom) {
    }

    /** A cluster's frame around its members, named at its top edge. */
    record Frame(UpgradeCluster cluster, Box box, float rowTop) {
    }

    /** One end of a link or a badge: a node, or a cluster's frame standing for all its members. */
    record End(List<UpgradeNode> nodes, Optional<UpgradeCluster> cluster, Box box, float rowTop) {

        End {
            nodes = List.copyOf(nodes);
        }

        float left() {
            return this.box.left();
        }

        float top() {
            return this.box.top();
        }

        float right() {
            return this.box.right();
        }

        float bottom() {
            return this.box.bottom();
        }

        float x() {
            return (this.box.left() + this.box.right()) / 2;
        }

        float y() {
            return (this.box.top() + this.box.bottom()) / 2;
        }

        int tier() {
            return this.nodes.getFirst().tier();
        }
    }

    record Point(float x, float y) {
    }

    /** {@code from} needed by {@code to}, drawn along {@code path}. */
    record Link(End from, End to, List<Point> path) {

        Link {
            path = List.copyOf(path);
        }
    }

    /** {@code from} needed by {@code to}, shown as a small circle centred at ({@code x}, {@code y}). */
    record Badge(End from, End to, float x, float y) {
    }

    /**
     * A slot that nodes behind different members of {@code cluster} share.
     *
     * @param bySide each member of the cluster to the node the slot shows for it
     */
    record Shared(Spot spot, UpgradeCluster cluster, Map<UpgradeNode, UpgradeNode> bySide) {

        Shared {
            bySide = Map.copyOf(bySide);
        }

        /** The member that decides the slot: the hovered one of the cluster, else the one owned. */
        Optional<UpgradeNode> pick(Predicate<UpgradeNode> owned, Optional<UpgradeNode> hovered) {
            return hovered.filter(this.bySide::containsKey)
                    .or(() -> this.bySide.keySet().stream().filter(owned).findFirst());
        }
    }

    /**
     * What the shared slots show now.
     *
     * @param hidden the nodes whose slot shows another member's node, or none
     * @param empty  the shared slots whose cluster has no member picked
     */
    record Showing(Set<UpgradeNode> hidden, List<Shared> empty) {

        Showing {
            hidden = Set.copyOf(hidden);
            empty = List.copyOf(empty);
        }

        static Showing all() {
            return new Showing(Set.of(), List.of());
        }

        boolean shows(UpgradeNode node) {
            return !this.hidden.contains(node);
        }

        /** Whether any of {@code end}'s nodes shows. */
        boolean shows(End end) {
            return end.nodes().stream().anyMatch(this::shows);
        }
    }

    UpgradeTreeLayout {
        bands = List.copyOf(bands);
        spots = Map.copyOf(spots);
        frames = List.copyOf(frames);
        links = List.copyOf(links);
        badges = List.copyOf(badges);
        shared = List.copyOf(shared);
    }

    /**
     * {@code tree} laid out by {@code plan}. Throws when the plan names a node or cluster the tree lacks, places one
     * twice or in another tier, places a cluster's member alone, leaves one out, or has another number of tiers.
     */
    static UpgradeTreeLayout of(UpgradeTree tree, UpgradeTreePlan plan) {
        return new Builder(tree, plan).build();
    }

    Optional<Spot> spotOf(UpgradeNode node) {
        return Optional.ofNullable(this.spots.get(node));
    }

    /**
     * What each shared slot shows when {@code owned} says what the tower owns and {@code hovered} is under the
     * pointer: the node of the member hovered, else of the member owned, else nothing.
     */
    Showing showing(Predicate<UpgradeNode> owned, Optional<UpgradeNode> hovered) {
        Set<UpgradeNode> hidden = new HashSet<>();
        List<Shared> empty = new ArrayList<>();
        for (Shared slot : this.shared) {
            Optional<UpgradeNode> pick = slot.pick(owned, hovered);
            if (pick.isEmpty()) {
                empty.add(slot);
            }
            slot.bySide().forEach((side, node) -> {
                if (pick.filter(side::equals).isEmpty()) {
                    hidden.add(node);
                }
            });
        }
        return new Showing(hidden, empty);
    }

    /** The node {@code showing} shows at design point ({@code x}, {@code y}), within {@code slack} of its box. */
    Optional<UpgradeNode> nodeAt(float x, float y, float slack, Showing showing) {
        return this.spots.entrySet().stream().filter(entry -> showing.shows(entry.getKey()))
                .filter(entry -> entry.getValue().contains(x, y, slack)).map(Map.Entry::getKey).findFirst();
    }

    /** The empty shared slot at design point ({@code x}, {@code y}), within {@code slack} of its box. */
    Optional<Shared> emptySlotAt(float x, float y, float slack, Showing showing) {
        return showing.empty().stream().filter(slot -> slot.spot().contains(x, y, slack)).findFirst();
    }

    /** {@code nodes} by where they sit, left to right, the lower row first where two share a column. */
    List<UpgradeNode> leftToRight(List<UpgradeNode> nodes) {
        return nodes.stream().sorted(Comparator.comparingDouble((UpgradeNode node) -> this.spots.get(node).x())
                .thenComparing(node -> -this.spots.get(node).y())).toList();
    }

    Band bandOf(int tier) {
        return this.bands.get(tier);
    }

    private static final class Builder {

        private final UpgradeTree tree;
        private final UpgradeTreePlan plan;
        private final Map<UpgradeNode, Spot> spots = new LinkedHashMap<>();
        private final Map<UpgradeCluster, Frame> frames = new LinkedHashMap<>();
        private final List<Band> bands = new ArrayList<>();

        Builder(UpgradeTree tree, UpgradeTreePlan plan) {
            this.tree = tree;
            this.plan = plan;
        }

        UpgradeTreeLayout build() {
            int tiers = this.tree.tiers().size();
            if (this.plan.tiers().size() != tiers) {
                throw new IllegalArgumentException("The plan has " + this.plan.tiers().size() + " tiers, the tree "
                        + tiers);
            }
            float y = TOP;
            Band[] placed = new Band[tiers];
            for (int tier = tiers - 1; tier >= 0; tier--) {
                List<UpgradeTreePlan.Row> rows = this.plan.tiers().get(tier).rows();
                float bottom = y + rows.size() * ROW + STRIP;
                placed[tier] = new Band(tier, y, bottom);
                for (int r = 0; r < rows.size(); r++) {
                    this.placeRow(tier, rows.get(r), y + (rows.size() - 1 - r) * ROW);
                }
                y = bottom + GAP;
            }
            this.bands.addAll(List.of(placed));
            for (UpgradeNode node : this.tree.nodes()) {
                if (!this.spots.containsKey(node)) {
                    throw new IllegalArgumentException("The plan leaves out " + node.id());
                }
            }
            List<Shared> shared = this.shared();
            List<Link> links = new ArrayList<>();
            List<Badge> badges = new ArrayList<>();
            for (Need need : this.needs()) {
                if (need.to().tier() - need.from().tier() >= 2) {
                    badges.add(badgeOn(need, badges.stream().filter(badge -> badge.to().equals(need.to())).count()));
                } else {
                    links.add(new Link(need.from(), need.to(), pathOf(need.from(), need.to())));
                }
            }
            return new UpgradeTreeLayout(this.bands, this.spots, List.copyOf(this.frames.values()), links, badges,
                    shared, y - GAP + FOOTER);
        }

        /**
         * Every slot more than one node is placed at. Throws unless the nodes there come after different members of
         * one cluster.
         */
        private List<Shared> shared() {
            Map<Spot, List<UpgradeNode>> bySpot = new LinkedHashMap<>();
            this.spots.forEach((node, spot) -> bySpot.computeIfAbsent(spot, _ -> new ArrayList<>()).add(node));
            List<Shared> shared = new ArrayList<>();
            bySpot.forEach((spot, nodes) -> {
                if (nodes.size() > 1) {
                    shared.add(this.sharedOf(spot, nodes));
                }
            });
            return shared;
        }

        private Shared sharedOf(Spot spot, List<UpgradeNode> nodes) {
            Map<UpgradeNode, UpgradeNode> bySide = new LinkedHashMap<>();
            Set<UpgradeCluster> clusters = new HashSet<>();
            for (UpgradeNode node : nodes) {
                Optional<UpgradeNode> side = this.tree.branchOf(node);
                if (side.isEmpty() || bySide.putIfAbsent(side.get(), node) != null) {
                    throw new IllegalArgumentException(sharers(nodes) + " share a slot but don't each come after a"
                            + " different member of one cluster");
                }
                clusters.add(this.tree.clusterOf(side.get()).orElseThrow());
            }
            if (clusters.size() != 1) {
                throw new IllegalArgumentException(sharers(nodes) + " share a slot but come after different clusters");
            }
            return new Shared(spot, clusters.iterator().next(), bySide);
        }

        private static String sharers(List<UpgradeNode> nodes) {
            return String.join(" and ", nodes.stream().map(UpgradeNode::id).toList());
        }

        /**
         * The {@code before}-th badge on a node's top-left corner, and on to its right; a frame's go on its top-right
         * corner and on to its left, clear of its name.
         */
        private static Badge badgeOn(Need need, long before) {
            float step = before * (2 * BADGE + 1f);
            End to = need.to();
            float x = to.cluster().isPresent() ? to.right() - 1f - step : to.left() + 1f + step;
            return new Badge(need.from(), to, x, to.top() + 1f);
        }

        private void placeRow(int tier, UpgradeTreePlan.Row row, float rowTop) {
            float centre = rowTop + NODE_DROP;
            for (UpgradeTreePlan.Slot slot : row.slots()) {
                float x = FIRST_SLOT + slot.at() * PITCH;
                if (!slot.cluster()) {
                    UpgradeNode node = this.nodeOf(slot.key(), tier);
                    if (this.tree.clusterOf(node).isPresent()) {
                        throw new IllegalArgumentException(node.id() + " is placed apart from its cluster");
                    }
                    this.put(node, x, centre, rowTop);
                    continue;
                }
                UpgradeCluster cluster = this.tree.clusters().stream().filter(c -> c.name().equals(slot.key()))
                        .findFirst().or(() -> this.tree.fork().filter(c -> c.name().equals(slot.key())))
                        .orElseThrow(() -> new IllegalArgumentException("No cluster " + slot.key()));
                List<String> members = cluster.memberIds();
                for (int i = 0; i < members.size(); i++) {
                    this.put(this.nodeOf(members.get(i), tier), x + i * PITCH, centre, rowTop);
                }
                float half = NODE / 2 + FRAME_PAD;
                this.frames.put(cluster, new Frame(cluster, new Box(x - half, centre - half,
                        x + (members.size() - 1) * PITCH + half, centre + half), rowTop));
            }
        }

        private UpgradeNode nodeOf(String id, int tier) {
            UpgradeNode node = this.tree.node(id).filter(found -> !this.tree.isUnlock(found))
                    .orElseThrow(() -> new IllegalArgumentException("No node " + id));
            if (node.tier() != tier) {
                throw new IllegalArgumentException(id + " is placed in tier " + tier + " but sits in " + node.tier());
            }
            return node;
        }

        private void put(UpgradeNode node, float x, float y, float rowTop) {
            if (this.spots.putIfAbsent(node, new Spot(x, y, NODE, rowTop)) != null) {
                throw new IllegalArgumentException("The plan places " + node.id() + " twice");
            }
        }

        /** A need of one node or frame by another, in the order the tree declares the needing nodes. */
        private record Need(End from, End to) {
        }

        /**
         * Every need to draw: a node's each, from the frame of a cluster when the rule takes any of its members, and
         * into its own cluster's frame when every member shares it; a need two ends share is drawn once.
         */
        private List<Need> needs() {
            Set<Need> needs = new LinkedHashSet<>();
            for (UpgradeNode node : this.tree.nodes()) {
                Set<String> mentioned = node.requires().mentionedNodes();
                if (mentioned.isEmpty()) {
                    continue;
                }
                End to = this.tree.clusterOf(node).filter(cluster -> this.allShare(cluster, mentioned))
                        .map(this::endOf).orElseGet(() -> this.endOf(node));
                boolean either = node.requires().requiredNodes().size() < mentioned.size();
                Optional<UpgradeCluster> named = either ? this.clusterOfAll(mentioned) : Optional.empty();
                if (named.isPresent()) {
                    needs.add(new Need(this.endOf(named.get()), to));
                    continue;
                }
                mentioned.stream().map(id -> this.tree.node(id).orElseThrow()).filter(this.spots::containsKey)
                        .sorted(Comparator.comparingDouble(before -> this.spots.get(before).x()))
                        .forEach(before -> needs.add(new Need(this.endOf(before), to)));
            }
            return List.copyOf(needs);
        }

        private boolean allShare(UpgradeCluster cluster, Set<String> mentioned) {
            return cluster.memberIds().stream().allMatch(id -> this.tree.node(id).orElseThrow().requires()
                    .mentionedNodes().equals(mentioned));
        }

        /** The placed cluster whose members are exactly {@code ids}. */
        private Optional<UpgradeCluster> clusterOfAll(Set<String> ids) {
            return this.frames.keySet().stream().filter(cluster -> Set.copyOf(cluster.memberIds()).equals(ids))
                    .findFirst();
        }

        private End endOf(UpgradeNode node) {
            Spot spot = this.spots.get(node);
            float half = spot.size() / 2;
            return new End(List.of(node), Optional.empty(), new Box(spot.x() - half, spot.y() - half,
                    spot.x() + half, spot.y() + half), spot.rowTop());
        }

        private End endOf(UpgradeCluster cluster) {
            Frame frame = this.frames.get(cluster);
            return new End(cluster.memberIds().stream().map(id -> this.tree.node(id).orElseThrow()).toList(),
                    Optional.of(cluster), frame.box(), frame.rowTop());
        }

        /**
         * Side to side within a row; else up from the source's top into the channel above its row, across, and up
         * into the target's bottom. From a frame the path keeps clear of its name, heading for the target where the
         * frame allows.
         */
        private static List<Point> pathOf(End from, End to) {
            if (Math.abs(from.y() - to.y()) < 1f) {
                boolean rightward = from.x() < to.x();
                return List.of(new Point(rightward ? from.right() : from.left(), from.y()),
                        new Point(rightward ? to.left() : to.right(), to.y()));
            }
            float x = from.x();
            if (from.cluster().isPresent()) {
                float clear = from.left() + from.cluster().get().name().length() * LABEL_LETTER + LABEL_PAD;
                x = Math.clamp(to.x(), Math.min(clear, from.right() - 6f), from.right() - 6f);
            }
            if (Math.abs(x - to.x()) < 0.5f) {
                return List.of(new Point(x, from.top()), new Point(x, to.bottom()));
            }
            float channel = from.rowTop() - GAP / 2;
            return List.of(new Point(x, from.top()), new Point(x, channel), new Point(to.x(), channel),
                    new Point(to.x(), to.bottom()));
        }
    }
}
