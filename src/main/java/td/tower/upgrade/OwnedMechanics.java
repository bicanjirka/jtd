package td.tower.upgrade;

import java.util.ArrayList;
import java.util.List;

/**
 * The mechanics a tower's owned nodes bring, oldest first. A node is bought on one thread and the mechanics
 * are read on another, so the list is replaced whole, never changed in place.
 *
 * @param <P> the tower's mechanic type
 */
public final class OwnedMechanics<P extends Mechanic> {

    private final MechanicCatalogue<P> catalogue;
    private volatile List<P> owned = List.of();

    public OwnedMechanics(MechanicCatalogue<P> catalogue) {
        this.catalogue = catalogue;
    }

    /** Adds what {@code node} brings; call it once, when the node is bought. */
    public void add(UpgradeNode node) {
        List<P> brought = this.catalogue.mechanicsOf(node);
        if (brought.isEmpty()) {
            return;
        }
        List<P> next = new ArrayList<>(this.owned);
        next.addAll(brought);
        this.owned = List.copyOf(next);
    }

    /** The mechanics owned now. Read it once per action, so every step of it sees the same mechanics. */
    public List<P> all() {
        return this.owned;
    }
}
