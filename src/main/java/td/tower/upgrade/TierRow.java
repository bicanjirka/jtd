package td.tower.upgrade;

import java.util.List;
import java.util.Optional;

/**
 * One tier's upgrades as the panel lays them out, left to right, and as the number keys count them. From the fork's
 * tier up, {@code side} holds what comes after each side of the fork, and {@code places} what both sides share.
 */
public record TierRow(int tier, List<RowPlace> places, Optional<RowPlace.Branches> side) {

    public TierRow {
        places = List.copyOf(places);
    }
}
