package td.tower.pulse;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * How long each enemy has been in the field since it last earned a Toll stack, so a stack comes
 * every {@code ticksPerStack} ticks inside. An enemy that leaves starts again when it returns.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class TollTracker {

    private final Map<EnemyMob, Integer> ticksInside = new IdentityHashMap<>();

    /**
     * Counts one more tick for each enemy in {@code inside}, forgets the rest, and tells who has just
     * earned a stack.
     */
    public List<EnemyMob> tick(List<EnemyMob> inside, int ticksPerStack) {
        this.ticksInside.keySet().removeIf(enemy -> !inside.contains(enemy));
        List<EnemyMob> earners = new ArrayList<>();
        for (EnemyMob enemy : inside) {
            int ticks = this.ticksInside.merge(enemy, 1, Integer::sum);
            if (ticks >= ticksPerStack) {
                this.ticksInside.put(enemy, 0);
                earners.add(enemy);
            }
        }
        return earners;
    }
}
