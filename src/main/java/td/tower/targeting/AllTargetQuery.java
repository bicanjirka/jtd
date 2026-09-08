package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/**
 * The identity element for {@link TargetQuery#and} - matches every enemy in play, so
 * intersecting it with another query leaves that query's matches unchanged.
 */
final class AllTargetQuery implements TargetQuery {

    static final TargetQuery INSTANCE = new AllTargetQuery();

    private AllTargetQuery() {
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        return List.of(enemies.getEnemies());
    }
}
