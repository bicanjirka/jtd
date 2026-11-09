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

    /** Which mechanics each node brings. */
    public MechanicCatalogue<P> catalogue() {
        return this.catalogue;
    }

    /** Adds what {@code node} brings; call it once, when the node is bought. */
    public synchronized void add(UpgradeNode node) {
        List<P> brought = this.catalogue.mechanicsOf(node);
        if (brought.isEmpty()) {
            return;
        }
        Held<P> now = this.held;
        List<UpgradeNode> from = new ArrayList<>(now.from());
        List<P> mechanics = new ArrayList<>(now.mechanics());
        brought.forEach(mechanic -> {
            from.add(node);
            mechanics.add(mechanic);
        });
        this.held = new Held<>(from, mechanics);
    }

    /** Drops what {@code removed} brought; the mechanics of every other node stay, with their state. */
    public synchronized void remove(Collection<UpgradeNode> removed) {
        Set<String> ids = removed.stream().map(UpgradeNode::id).collect(Collectors.toSet());
        Held<P> now = this.held;
        List<UpgradeNode> from = new ArrayList<>();
        List<P> mechanics = new ArrayList<>();
        for (int i = 0; i < now.from().size(); i++) {
            if (!ids.contains(now.from().get(i).id())) {
                from.add(now.from().get(i));
                mechanics.add(now.mechanics().get(i));
            }
        }
        if (from.size() == now.from().size()) {
            return;
        }
        this.held = new Held<>(from, mechanics);
    }

    /**
     * Replaces every mechanic with a new one from the same node, in the same order, so none keeps what it counted.
     * Built from what is held rather than from the owned nodes, so a node bought on another thread meanwhile is kept.
     */
    public synchronized void renew() {
        Held<P> now = this.held;
        List<UpgradeNode> from = new ArrayList<>();
        List<P> mechanics = new ArrayList<>();
        UpgradeNode last = null;
        for (UpgradeNode node : now.from()) {
            if (node != last) {
                for (P mechanic : this.catalogue.mechanicsOf(node)) {
                    from.add(node);
                    mechanics.add(mechanic);
                }
                last = node;
            }
        }
        this.held = new Held<>(from, mechanics);
    }

    /** The mechanics owned now. Read it once per action, so every step of it sees the same mechanics. */
    public List<P> all() {
        return this.held.mechanics();
    }

    /** Each mechanic beside the id of the node that brought it. */
    private record Held<P>(List<UpgradeNode> from, List<P> mechanics) {

        private Held {
            from = List.copyOf(from);
            mechanics = List.copyOf(mechanics);
        }
    }
}
