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
    private volatile Held<P> held = new Held<>(List.of());

    public OwnedMechanics(MechanicCatalogue<P> catalogue) {
        this.catalogue = catalogue;
    }

    /** Which mechanics each node brings. */
    public MechanicCatalogue<P> catalogue() {
        return this.catalogue;
    }

    /** Adds what {@code node} brings; call it once, when the node is bought. */
    public synchronized void add(UpgradeNode node) {
        List<Owned<P>> brought = this.broughtBy(node);
        if (brought.isEmpty()) {
            return;
        }
        List<Owned<P>> owned = new ArrayList<>(this.held.owned());
        owned.addAll(brought);
        this.held = new Held<>(owned);
    }

    /** Drops what {@code removed} brought; the mechanics of every other node stay, with their state. */
    public synchronized void remove(Collection<UpgradeNode> removed) {
        Set<String> ids = removed.stream().map(UpgradeNode::id).collect(Collectors.toSet());
        List<Owned<P>> now = this.held.owned();
        List<Owned<P>> kept = now.stream().filter(owned -> !ids.contains(owned.node().id())).toList();
        if (kept.size() != now.size()) {
            this.held = new Held<>(kept);
        }
    }

    /**
     * Replaces every mechanic with a new one from the same node, in the same order, so none keeps what it counted.
     * Built from what is held rather than from the owned nodes, so a node bought on another thread meanwhile is kept.
     */
    public synchronized void renew() {
        this.held = new Held<>(this.held.owned().stream()
                .map(Owned::node)
                .distinct()
                .flatMap(node -> this.broughtBy(node).stream())
                .toList());
    }

    /** The mechanics owned now. Read it once per action, so every step of it sees the same mechanics. */
    public List<P> all() {
        return this.held.mechanics();
    }

    private List<Owned<P>> broughtBy(UpgradeNode node) {
        return this.catalogue.mechanicsOf(node).stream().map(mechanic -> new Owned<>(node, mechanic)).toList();
    }

    /** One mechanic beside the node that brought it. */
    private record Owned<P>(UpgradeNode node, P mechanic) {
    }

    /** The owned mechanics, oldest first, with the bare mechanics kept for the readers of every tick. */
    private record Held<P>(List<Owned<P>> owned, List<P> mechanics) {

        Held(List<Owned<P>> owned) {
            this(List.copyOf(owned), owned.stream().map(Owned::mechanic).toList());
        }
    }
}
