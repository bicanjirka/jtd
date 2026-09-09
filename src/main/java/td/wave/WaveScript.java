package td.wave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.enemy.EnemyFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a wave's token string (see the wave mini-language table in CLAUDE.md) into a
 * {@link WaveContent} - no {@code GameWorld} needed, unlike {@link Wave}, which is what
 * turns that content into live, world-bound enemies. A token this can't recognize as
 * either an enemy letter or an integer repeat count is logged at {@code WARN} and treated
 * as a multiplier of 1 rather than failing the parse - see CLAUDE.md's Gotchas.
 */
public final class WaveScript {

    private static final Logger LOG = LoggerFactory.getLogger(WaveScript.class);

    private WaveScript() {
    }

    public static WaveContent parse(String tokens) {
        return parse(tokens.split(" "));
    }

    public static WaveContent parse(String[] tokens) {
        List<EnemyFactory.Enemy> spawnSequence = new ArrayList<>();
        int repeat = 1;
        for (String token : tokens) {
            if (EnemyFactory.isEnemy(token)) {
                EnemyFactory.Enemy enemy = EnemyFactory.identifyEnemy(token);
                for (int i = 0; i < repeat; i++) {
                    spawnSequence.add(enemy);
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
