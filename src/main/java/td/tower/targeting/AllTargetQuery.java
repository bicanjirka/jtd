package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/** The identity for {@link TargetQuery#and}: matches every enemy. */
final class AllTargetQuery implements TargetQuery {

    static final TargetQuery INSTANCE = new AllTargetQuery();

    private AllTargetQuery() {
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        return List.of(enemies.getEnemies());
    }
}
