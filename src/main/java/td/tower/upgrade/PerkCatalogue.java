package td.tower.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which perks each node of one tower's tree brings: what a node does beyond its buff. A node may
 * bring several. Each tower gets perks of its own, as some carry state.
 *
 * @param <P> the tower's perk type
 */
public final class PerkCatalogue<P> {

    private final Map<String, List<Supplier<? extends P>>> byNode;

    private PerkCatalogue(Map<String, List<Supplier<? extends P>>> byNode) {
        this.byNode = byNode;
    }

    public static <P> PerkCatalogue<P> empty() {
        return new PerkCatalogue<>(Map.of());
    }

    /** This catalogue with {@code perk} added to what the node {@code nodeId} brings. */
    public PerkCatalogue<P> with(String nodeId, Supplier<? extends P> perk) {
        Map<String, List<Supplier<? extends P>>> next = new HashMap<>(this.byNode);
        List<Supplier<? extends P>> perks = new ArrayList<>(next.getOrDefault(nodeId, List.of()));
        perks.add(perk);
        next.put(nodeId, List.copyOf(perks));
        return new PerkCatalogue<>(Map.copyOf(next));
    }

    /** New perks for {@code node}, in the order they were added; none if it brings only a buff. */
    public List<P> perksOf(UpgradeNode node) {
        return this.byNode.getOrDefault(node.id(), List.of()).stream()
                .<P>map(Supplier::get)
                .toList();
    }
}
