package td.wave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.enemy.EnemyCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a wave's token string (see the wave mini-language table in CLAUDE.md) into a
 * {@link WaveContent} against a given {@link EnemyCatalog} - no {@code GameWorld} needed,
 * unlike {@link Wave}, which is what turns that content into live, world-bound enemies. The
 * {@code catalog} argument is what lets a per-level custom or cloned enemy id resolve exactly
 * like a built-in one; there is no separate syntax for the two, since every non-reserved token
 * is looked up the same way. {@code e} is the one reserved token - the spacer - recognized
 * before any catalog lookup. A token this can't recognize as the spacer, a registered id, or an
 * integer repeat count is logged at {@code WARN} and treated as a multiplier of 1 rather than
 * failing the parse - see CLAUDE.md §9.
 */
public final class WaveScript {

    private static final Logger LOG = LoggerFactory.getLogger(WaveScript.class);
    private static final String EMPTY_TOKEN = "e";

    private WaveScript() {
    }

    public static WaveContent parse(String tokens, EnemyCatalog catalog) {
        return parse(tokens.split(" "), catalog);
    }

    public static WaveContent parse(String[] tokens, EnemyCatalog catalog) {
        List<WaveSlot> spawnSequence = new ArrayList<>();
        int repeat = 1;
        for (String token : tokens) {
            if (token.equals(EMPTY_TOKEN)) {
                for (int i = 0; i < repeat; i++) {
                    spawnSequence.add(new EmptySlot());
                }
                repeat = 1;
            } else if (catalog.contains(token)) {
                WaveSlot slot = new EnemySlot(catalog.get(token));
                for (int i = 0; i < repeat; i++) {
                    spawnSequence.add(slot);
                }
                repeat = 1;
            } else {
                try {
                    repeat = Integer.parseInt(token);
                } catch (NumberFormatException ex) {
                    LOG.warn("Unrecognized wave token '{}', treating as x1", token, ex);
                    repeat = 1;
                }
            }
        }
        return new WaveContent(spawnSequence);
    }
}
