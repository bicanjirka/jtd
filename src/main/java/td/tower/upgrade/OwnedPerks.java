package td.tower.upgrade;

import java.util.ArrayList;
import java.util.List;

/**
 * The perks a tower's owned nodes bring, oldest first. A node is bought on one thread and the perks
 * are read on another, so the list is replaced whole, never changed in place.
 *
 * @param <P> the tower's perk type
 */
public final class OwnedPerks<P> {

    private final PerkCatalogue<P> catalogue;
    private volatile List<P> owned = List.of();

    public OwnedPerks(PerkCatalogue<P> catalogue) {
        this.catalogue = catalogue;
    }

    /** Adds what {@code node} brings; call it once, when the node is bought. */
    public void add(UpgradeNode node) {
        List<P> brought = this.catalogue.perksOf(node);
        if (brought.isEmpty()) {
            return;
        }
        List<P> next = new ArrayList<>(this.owned);
        next.addAll(brought);
        this.owned = List.copyOf(next);
    }

    /** The perks owned now. Read it once per action, so every step of it sees the same perks. */
    public List<P> all() {
        return this.owned;
    }
}
