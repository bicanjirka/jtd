package td.tower.upgrade;

import td.term.TermText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which mechanics each node of one tower's tree brings: what a node does beyond its buff. A node may
 * bring several. Each tower gets mechanics of its own, as some carry state.
 *
 * @param <P> the tower's mechanic type
 */
public final class MechanicCatalogue<P extends Mechanic> {

    private final Map<String, List<Supplier<? extends P>>> byNode;

    private MechanicCatalogue(Map<String, List<Supplier<? extends P>>> byNode) {
        this.byNode = byNode;
    }

    public static <P extends Mechanic> MechanicCatalogue<P> empty() {
        return new MechanicCatalogue<>(Map.of());
    }

    /** This catalogue with {@code mechanic} added to what the node {@code nodeId} brings. */
    public MechanicCatalogue<P> with(String nodeId, Supplier<? extends P> mechanic) {
        Map<String, List<Supplier<? extends P>>> next = new HashMap<>(this.byNode);
        List<Supplier<? extends P>> mechanics = new ArrayList<>(next.getOrDefault(nodeId, List.of()));
        mechanics.add(mechanic);
        next.put(nodeId, List.copyOf(mechanics));
        return new MechanicCatalogue<>(Map.copyOf(next));
    }

    /** New mechanics for {@code node}, in the order they were added; none if it brings only a buff. */
    public List<P> mechanicsOf(UpgradeNode node) {
        return this.byNode.getOrDefault(node.id(), List.of()).stream()
                .<P>map(Supplier::get)
                .toList();
    }

    /** What {@code node}'s mechanics add, in words, in the order they were added. */
    public List<TermText> describe(UpgradeNode node) {
        return this.mechanicsOf(node).stream().map(Mechanic::describe).toList();
    }
}
