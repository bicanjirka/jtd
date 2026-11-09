package td.save;

import java.util.Map;
import java.util.Set;

/**
 * The rounds played so far, as the waves panel shows them.
 *
 * @param leaked      0-based rounds an enemy escaped
 * @param bonusesPaid the clear bonus each 0-based round paid; a round that paid none is absent
 */
public record RoundHistory(Set<Integer> leaked, Map<Integer, Integer> bonusesPaid) {

    private static final RoundHistory NONE = new RoundHistory(Set.of(), Map.of());

    public RoundHistory {
        leaked = Set.copyOf(leaked);
        bonusesPaid = Map.copyOf(bonusesPaid);
    }

    public static RoundHistory none() {
        return NONE;
    }
}
