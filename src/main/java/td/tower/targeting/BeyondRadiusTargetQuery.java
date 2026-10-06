package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemies at least a fixed distance from a point: a tower's dead zone. The distance is in pixels, so
 * a range bonus never changes it.
 */
public final class BeyondRadiusTargetQuery implements TargetQuery {

    private final int x;
    private final int y;
    private final float radius;

    public BeyondRadiusTargetQuery(int x, int y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        float radius2 = this.radius * this.radius;
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (e.validTarget() && !WithinRange.of(e, this.x, this.y, radius2)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
