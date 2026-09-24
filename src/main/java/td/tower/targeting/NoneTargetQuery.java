package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/**
 * The absorber for {@link TargetQuery#and}: matches nothing, and the other side is never evaluated.
 */
final class NoneTargetQuery implements TargetQuery {

    static final TargetQuery INSTANCE = new NoneTargetQuery();

    private NoneTargetQuery() {
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        return List.of();
    }

    @Override
    public TargetQuery and(TargetQuery other) {
        return this;
    }
}
