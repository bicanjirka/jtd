package td.tower.sonar;

import td.enemy.EnemyMob;
import td.tower.targeting.Viewpoint;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Remembers the enemies the beam passed during a revolution, so the ping can pick among them. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PingTracker {

    private final Set<EnemyMob> passed = Collections.newSetFromMap(new IdentityHashMap<>());

    public void pass(EnemyMob enemy) {
        this.passed.add(enemy);
    }

    /**
     * The healthiest enemies passed that are still alive and at least {@code rule.minRangeShare()}
     * of the range from {@code view}, as many as the rule says, healthiest first. The next
     * revolution starts empty.
     */
    public List<EnemyMob> pick(PingRule rule, Viewpoint view) {
        double nearest = rule.minRangeShare() * view.range();
        List<EnemyMob> candidates = new ArrayList<>();
        for (EnemyMob enemy : this.passed) {
            if (!enemy.isDead() && Math.hypot(enemy.getX() - view.x(), enemy.getY() - view.y()) >= nearest) {
                candidates.add(enemy);
            }
        }
        this.passed.clear();
        candidates.sort(Comparator.comparingInt(EnemyMob::getHealth).reversed()
                .thenComparing(Comparator.comparingInt(EnemyMob::getProgression).reversed()));
        return List.copyOf(candidates.subList(0, Math.min(rule.count(), candidates.size())));
    }
}
