package td.ui;

import td.tower.upgrade.RowPlace;
import td.tower.upgrade.TierRow;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Where every node of a tree sits on the Upgrades panel, in design units {@value #WIDTH} wide, derived from the
 * tree's rows: one row per tier, bottom up, with a gate band between two tiers for the upper one's unlock and, at the
 * fork's tier, the fork's sides. From the fork's tier up a row splits into what both sides share, on the left, and
 * the picked side's column. A cluster's branches share one place, showing the owned member's, or the previewed
 * member's dimmed. A folded tier is a thin row of small nodes. No tower has layout of its own. Pure.
 *
 * @param spots     each shown node's place, the unlocks and the fork's sides included
 * @param links     a node to the one it comes after, where both show
 * @param frames    a cluster's frame around its members
 * @param rows      every tier's row, top first
 * @param hidden    where a branch the tower didn't pick hides
 * @param previewed the nodes shown only because the hovered member opens them
 * @param split     the x of the line between the shared column and the side's; empty without a fork
 * @param height    the whole panel's height, the XP line and footer included
 */
record UpgradeTreeLayout(Map<UpgradeNode, Spot> spots, List<Link> links, List<Frame> frames, List<Row> rows,
                         List<Spot> hidden, Set<UpgradeNode> previewed, Optional<Float> split, float height) {

    static final int WIDTH = 300;
    /** The design height the panel asks for: four unfolded tiers fit it. */
    static final int HEIGHT = 380;
    static final float NODE = 32f;
    static final float SMALL_NODE = 14f;
    static final float PILL_WIDTH = 46f;
    static final float PILL_HEIGHT = 16f;
    static final float THUMB_X = 22f;
    static final float FOOTER = 20f;

    private static final float TOP = 40f;
    private static final float ROW = 54f;
    private static final float FOLDED_ROW = 22f;
    private static final float BAND = 22f;
    private static final float FORK_BAND = 52f;
    private static final float LEFT = 46f;
    private static final float RIGHT = 294f;
    private static final float PITCH = 40f;
    private static final float SMALL_PITCH = 20f;
    private static final float PAD = 4f;
    private static final float GAP = 6f;
    private static final float MARKER = 14f;
    private static final float NODE_TOP = 6f;

    /** One spot's centre and box. */
    record Spot(float x, float y, float width, float height) {

        boolean contains(float px, float py, float slack) {
            return Math.abs(px - this.x) <= this.width / 2 + slack && Math.abs(py - this.y) <= this.height / 2 + slack;
        }
    }

    record Link(UpgradeNode from, UpgradeNode to) {
    }

    /** A cluster's frame, named at its top edge. */
    record Frame(UpgradeCluster cluster, Spot box) {

        float left() {
            return this.box.x() - this.box.width() / 2;
        }

        float right() {
            return this.box.x() + this.box.width() / 2;
        }

        float top() {
            return this.box.y() - this.box.height() / 2;
        }

        float bottom() {
            return this.box.y() + this.box.height() / 2;
        }
    }

    /** A tier's row, and the band of its gate under it; a free tier's band is empty, tier 0 has none. */
    record Row(int tier, float top, float bottom, float bandBottom, boolean folded) {

        /** A full-height row with no band under it. */
        static Row of(int tier, float top, float bottom) {
            return new Row(tier, top, bottom, bottom, false);
        }

        Row withBandTo(float bandBottom) {
            return new Row(this.tier, this.top, this.bottom, bandBottom, this.folded);
        }

        Row foldedIf(boolean thin) {
            return new Row(this.tier, this.top, this.bottom, this.bandBottom, thin);
        }

        float centre() {
            return (this.top + this.bottom) / 2;
        }
    }

    /** The node size and pitch a row is laid out at. */
    private record Scale(float node, float pitch, float pad, float gap) {

        Scale squeezed(float share) {
            return share >= 1f ? this : new Scale(Math.min(this.node, this.pitch * share - 6f), this.pitch * share,
                    this.pad * share, this.gap * share);
        }
    }

    UpgradeTreeLayout {
        spots = Map.copyOf(spots);
        links = List.copyOf(links);
        frames = List.copyOf(frames);
        rows = List.copyOf(rows);
        hidden = List.copyOf(hidden);
        previewed = Set.copyOf(previewed);
    }

    /**
     * {@code tree}'s layout for a tower owning {@code owned}, with {@code preview}'s branches shown when it is a cluster
     * member, and every tier in {@code folded} thin.
     */
    static UpgradeTreeLayout of(UpgradeTree tree, UpgradeState owned, Optional<UpgradeNode> preview,
                                Set<Integer> folded) {
        return new Builder(tree, owned, preview, folded).build();
    }

    Optional<Spot> spotOf(UpgradeNode node) {
        return Optional.ofNullable(this.spots.get(node));
    }

    /** The node at design point ({@code x}, {@code y}), within {@code slack} of its box. */
    Optional<UpgradeNode> nodeAt(float x, float y, float slack) {
        return this.spots.entrySet().stream().filter(entry -> entry.getValue().contains(x, y, slack))
                .map(Map.Entry::getKey).findFirst();
    }

    /** The nodes of tier {@code tier}'s row as shown, the band's unlock and sides aside. */
    List<UpgradeNode> shownIn(int tier) {
        return this.spots.keySet().stream().filter(node -> node.tier() == tier && this.inRow(node)).toList();
    }

    private boolean inRow(UpgradeNode node) {
        Spot spot = this.spots.get(node);
        return this.rows.stream().anyMatch(row -> row.tier() == node.tier() && spot.y() >= row.top()
                && spot.y() <= row.bottom());
    }

    private static final class Builder {

        private final UpgradeTree tree;
        private final UpgradeState owned;
        private final Optional<UpgradeNode> preview;
        private final Set<Integer> folded;
        private final Map<UpgradeNode, Spot> spots = new LinkedHashMap<>();
        private final List<Frame> frames = new ArrayList<>();
        private final List<Spot> hidden = new ArrayList<>();
        private final Set<UpgradeNode> previewed = new LinkedHashSet<>();
        private final List<Row> rows = new ArrayList<>();
        private final int forkTier;
        private final float split;

        Builder(UpgradeTree tree, UpgradeState owned, Optional<UpgradeNode> preview, Set<Integer> folded) {
            this.tree = tree;
            this.owned = owned;
            this.preview = preview;
            this.folded = folded;
            this.forkTier = tree.fork().map(fork -> tree.node(fork.memberIds().getFirst()).orElseThrow().tier())
                    .orElse(Integer.MAX_VALUE);
            float shared = tree.rows().stream().filter(row -> row.tier() >= this.forkTier)
                    .map(row -> widthOf(row.places(), this.full(row.tier()))).reduce(0f, Math::max);
            this.split = LEFT + shared + GAP;
        }

        UpgradeTreeLayout build() {
            List<TierRow> tierRows = this.tree.rows();
            float y = TOP;
            for (int i = tierRows.size() - 1; i >= 0; i--) {
                TierRow row = tierRows.get(i);
                boolean thin = this.folded.contains(row.tier());
                float bottom = y + (thin ? FOLDED_ROW : ROW);
                this.placeRow(row, y, thin);
                float bandBottom = bottom + this.bandHeight(row.tier());
                this.placeBand(row.tier(), bottom, bandBottom);
                this.rows.add(Row.of(row.tier(), y, bottom).withBandTo(bandBottom).foldedIf(thin));
                y = bandBottom;
            }
            List<Link> links = new ArrayList<>();
            for (UpgradeNode node : this.spots.keySet()) {
                this.tree.previousInLine(node).filter(this.spots::containsKey)
                        .ifPresent(previous -> links.add(new Link(previous, node)));
            }
            Optional<Float> split = this.forkTier == Integer.MAX_VALUE ? Optional.empty() : Optional.of(this.split);
            return new UpgradeTreeLayout(this.spots, links, this.frames, this.rows, this.hidden, this.previewed, split,
                    y + FOOTER);
        }

        private float bandHeight(int tier) {
            if (tier == 0) {
                return 0f;
            }
            return tier == this.forkTier ? FORK_BAND : BAND;
        }

        private Scale full(int tier) {
            return this.folded.contains(tier) ? new Scale(SMALL_NODE, SMALL_PITCH, 2f, 3f) : new Scale(NODE, PITCH, PAD, GAP);
        }

        private void placeRow(TierRow row, float top, boolean thin) {
            Scale scale = this.full(row.tier());
            float centre = top + (thin ? FOLDED_ROW / 2 : NODE_TOP + NODE / 2);
            if (row.tier() < this.forkTier) {
                this.placeAll(row.places(), LEFT, RIGHT, centre, scale, false);
                return;
            }
            this.placeAll(row.places(), LEFT, this.split - GAP, centre, scale, false);
            row.side().ifPresent(side -> this.placeBranches(side, this.split + GAP, centre, scale, false, false));
        }

        /** The unlock's pill at the band's left; at the fork's tier, the fork's sides in the side column. */
        private void placeBand(int tier, float top, float bottom) {
            this.tree.unlockOf(tier).ifPresent(unlock -> this.spots.put(unlock,
                    new Spot(LEFT + PILL_WIDTH / 2, (top + bottom) / 2, PILL_WIDTH, PILL_HEIGHT)));
            if (tier != this.forkTier) {
                return;
            }
            UpgradeCluster fork = this.tree.fork().orElseThrow();
            float x = this.split + GAP + PITCH / 2;
            for (String id : fork.memberIds()) {
                this.spots.put(this.tree.node(id).orElseThrow(), new Spot(x, (top + bottom) / 2, NODE, NODE));
                x += PITCH;
            }
        }

        /** Lays {@code places} out from {@code left}, squeezed to end by {@code right}. */
        private void placeAll(List<RowPlace> places, float left, float right, float y, Scale scale, boolean dim) {
            float width = widthOf(places, scale);
            Scale fitted = scale.squeezed(width <= right - left ? 1f : (right - left) / width);
            float x = left;
            for (RowPlace place : places) {
                x = this.place(place, x, y, fitted, dim);
            }
        }

        /** Places {@code place} from {@code x}; returns where the next starts. */
        private float place(RowPlace place, float x, float y, Scale scale, boolean dim) {
            return switch (place) {
                case RowPlace.Lone lone -> {
                    this.put(lone.node(), x + scale.pitch() / 2, y, scale, dim);
                    yield x + scale.pitch();
                }
                case RowPlace.Group group -> {
                    float cursor = x + scale.pad();
                    for (UpgradeNode member : group.members()) {
                        this.put(member, cursor + scale.pitch() / 2, y, scale, dim);
                        cursor += scale.pitch();
                    }
                    float half = scale.node() / 2 + scale.pad();
                    float left = x + scale.pitch() / 2 - half + scale.pad();
                    float right = cursor - scale.pitch() / 2 + half;
                    this.frames.add(new Frame(group.cluster(), new Spot((left + right) / 2, y, right - left, 2 * half)));
                    yield cursor + scale.pad() + scale.gap();
                }
                case RowPlace.Branches branches -> {
                    this.placeBranches(branches, x, y, scale, dim, true);
                    yield x + widthOf(List.of(branches), scale);
                }
            };
        }

        /**
         * The branch of the previewed member, dimmed, else the owned member's; a marker where a branch hides, unless
         * {@code marked} is off: before the fork's pick its side column stays empty.
         */
        private void placeBranches(RowPlace.Branches branches, float x, float y, Scale scale, boolean dim,
                                   boolean marked) {
            List<String> members = branches.cluster().memberIds();
            int previewIndex = this.preview.filter(node -> !this.owned.owns(node.id()))
                    .map(node -> members.indexOf(node.id())).orElse(-1);
            int ownedIndex = -1;
            for (int i = 0; i < members.size(); i++) {
                if (this.owned.owns(members.get(i))) {
                    ownedIndex = i;
                }
            }
            int shown = previewIndex >= 0 ? previewIndex : ownedIndex;
            float cursor = x;
            if (shown >= 0) {
                boolean dimmed = dim || shown == previewIndex;
                for (RowPlace place : branches.branches().get(shown)) {
                    cursor = this.place(place, cursor, y, scale, dimmed);
                }
            }
            boolean hides = false;
            for (int i = 0; i < members.size(); i++) {
                hides |= i != shown && !branches.branches().get(i).isEmpty();
            }
            if (marked && hides) {
                float size = Math.min(MARKER, scale.node());
                this.hidden.add(new Spot(cursor + size / 2 + 2f, y, size, size));
            }
        }

        private void put(UpgradeNode node, float x, float y, Scale scale, boolean dim) {
            this.spots.put(node, new Spot(x, y, scale.node(), scale.node()));
            if (dim) {
                this.previewed.add(node);
            }
        }

        private static float widthOf(List<RowPlace> places, Scale scale) {
            float width = 0f;
            for (RowPlace place : places) {
                width += switch (place) {
                    case RowPlace.Lone lone -> scale.pitch();
                    case RowPlace.Group group -> group.members().size() * scale.pitch() + 2 * scale.pad() + scale.gap();
                    case RowPlace.Branches branches -> branches.branches().stream()
                            .map(branch -> widthOf(branch, scale)).reduce(0f, Math::max) + MARKER + 4f;
                };
            }
            return width;
        }
    }
}
