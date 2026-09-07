package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.Context;

import java.util.List;

/**
 * The absorber for {@link TargetQuery#and} - matches nothing, and wrapping a query with it
 * makes the combination immune to whatever it is intersected with next: {@code and} returns
 * {@code this} directly rather than building an {@link IntersectingTargetQuery}, so the other
 * side is never even evaluated.
 */
final class NoneTargetQuery implements TargetQuery {

    static final TargetQuery INSTANCE = new NoneTargetQuery();

    private NoneTargetQuery() {
    }

    @Override
    public List<EnemyMob> matching(Context context) {
        return List.of();
    }

    @Override
    public TargetQuery and(TargetQuery other) {
        return this;
    }
}
