package td.tower.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The mechanics a tower holds, each remembering the node that brought it: first those its earned tiers' own nodes
 * grant, lowest tier first, then those its owned nodes bring, oldest first. A node is bought on one thread and the
 * mechanics are read on another, so the list is replaced whole, never changed in place.
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
        List<Owned<P>> brought = this.broughtBy(node);
        if (brought.isEmpty()) {
            return;
        }
        List<Owned<P>> bought = new ArrayList<>(this.held.bought());
        bought.addAll(brought);
        this.held = new Held<>(this.held.granted(), bought);
    }

    /** Drops what {@code removed} brought; the mechanics of every other node stay, with their state. */
    public synchronized void remove(Collection<UpgradeNode> removed) {
        Set<String> ids = removed.stream().map(UpgradeNode::id).collect(Collectors.toSet());
        List<Owned<P>> now = this.held.bought();
        List<Owned<P>> kept = now.stream().filter(owned -> !ids.contains(owned.node().id())).toList();
        if (kept.size() != now.size()) {
            this.held = new Held<>(this.held.granted(), kept);
        }
    }

    /**
     * Holds what {@code tierNodes}, the own nodes of the tiers the tower has earned, bring: a node still among them keeps
     * its mechanics and their state, a new one brings fresh ones, and one no longer among them takes its own away.
     */
    public synchronized void grant(List<UpgradeNode> tierNodes) {
        List<Owned<P>> granted = tierNodes.stream().flatMap(node -> {
            List<Owned<P>> kept = this.held.granted().stream().filter(owned -> owned.node().equals(node)).toList();
            return kept.isEmpty() ? this.broughtBy(node).stream() : kept.stream();
        }).toList();
        this.held = new Held<>(granted, this.held.bought());
    }

    /**
     * Replaces every mechanic with a new one from the same node, in the same order, so none keeps what it counted.
     * Built from what is held rather than from the owned nodes, so a node bought on another thread meanwhile is kept.
     */
    public synchronized void renew() {
        this.held = new Held<>(this.renewed(this.held.granted()), this.renewed(this.held.bought()));
    }

    /** The mechanics held now. Read it once per action, so every step of it sees the same mechanics. */
    public List<P> all() {
        return this.held.mechanics();
    }

    private List<Owned<P>> renewed(List<Owned<P>> owned) {
        return owned.stream()
                .map(Owned::node)
                .distinct()
                .flatMap(node -> this.broughtBy(node).stream())
                .toList();
    }

    private List<Owned<P>> broughtBy(UpgradeNode node) {
        return this.catalogue.mechanicsOf(node).stream().map(mechanic -> new Owned<>(node, mechanic)).toList();
    }

    /** One mechanic beside the node that brought it. */
    private record Owned<P>(UpgradeNode node, P mechanic) {
    }

    /** The granted and the bought mechanics, with the bare mechanics of both kept for the readers of every tick. */
    private record Held<P>(List<Owned<P>> granted, List<Owned<P>> bought, List<P> mechanics) {

        Held(List<Owned<P>> granted, List<Owned<P>> bought) {
            this(List.copyOf(granted), List.copyOf(bought), Stream.concat(granted.stream(), bought.stream())
                    .map(Owned::mechanic).toList());
        }
    }
}
