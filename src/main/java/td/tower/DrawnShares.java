package td.tower;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** The shares a pane shows of what a tower draws by lot: only the kinds that come up, in the kinds' order. */
public final class DrawnShares {

    private DrawnShares() {
    }

    /** The kinds of {@code shares} above zero, each with its share. */
    public static <K extends Enum<K>> Map<K, Float> of(Class<K> kinds, Map<K, Float> shares) {
        Map<K, Float> drawn = new EnumMap<>(kinds);
        shares.forEach((kind, share) -> {
            if (share > 0f) {
                drawn.put(kind, share);
            }
        });
        return Collections.unmodifiableMap(drawn);
    }
}
