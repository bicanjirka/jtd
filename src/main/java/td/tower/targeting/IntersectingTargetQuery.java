package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * The composition behind {@link TargetQuery#and}: matches whatever both delegate queries
 * match. Pure wiring - no domain logic of its own.
 */
final class IntersectingTargetQuery implements TargetQuery {

    private final TargetQuery first;
    private final TargetQuery second;

    IntersectingTargetQuery(TargetQuery first, TargetQuery second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public List<EnemyMob> matching(Context context) {
        List<EnemyMob> matches = new ArrayList<>(this.first.matching(context));
        matches.retainAll(this.second.matching(context));
        return matches;
    }
}
