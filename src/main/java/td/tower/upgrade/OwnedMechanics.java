package td.tower.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The mechanics a tower's owned nodes bring, oldest first, each remembering the node that brought it. A node
 * is bought on one thread and the mechanics are read on another, so the list is replaced whole, never
 * changed in place.
 *
 * @param <P> the tower's mechanic type
 */
public final class OwnedMechanics<P extends Mechanic> {

    private final MechanicCatalogue<P> catalogue;
    private volatile Held<P> held = new Held<>(List.of(), List.of());

    public OwnedMechanics(MechanicCatalogue<P> catalogue) {
        this.catalogue = catalogue;
    }

    /** Adds what {@code node} brings; call it once, when the node is bought. */
    public void add(UpgradeNode node) {
        List<P> brought = this.catalogue.mechanicsOf(node);
        if (brought.isEmpty()) {
            return;
        }
        Held<P> now = this.held;
        List<String> from = new ArrayList<>(now.from());
        List<P> mechanics = new ArrayList<>(now.mechanics());
        brought.forEach(mechanic -> {
            from.add(node.id());
            mechanics.add(mechanic);
        });
        this.held = new Held<>(from, mechanics);
    }

    /** Drops what {@code removed} brought; the mechanics of every other node stay, with their state. */
    public void remove(Collection<UpgradeNode> removed) {
        Set<String> ids = removed.stream().map(UpgradeNode::id).collect(Collectors.toSet());
        Held<P> now = this.held;
        List<String> from = new ArrayList<>();
        List<P> mechanics = new ArrayList<>();
        for (int i = 0; i < now.from().size(); i++) {
            if (!ids.contains(now.from().get(i))) {
                from.add(now.from().get(i));
                mechanics.add(now.mechanics().get(i));
            }
        }
        this.held = new Held<>(from, mechanics);
    }

    /** The mechanics owned now. Read it once per action, so every step of it sees the same mechanics. */
    public List<P> all() {
        return this.held.mechanics();
    }

    /** Each mechanic beside the id of the node that brought it. */
    private record Held<P>(List<String> from, List<P> mechanics) {

        private Held {
            from = List.copyOf(from);
            mechanics = List.copyOf(mechanics);
        }
    }
}
