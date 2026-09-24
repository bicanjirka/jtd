package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;

/** Matches what both delegates match. */
final class IntersectingTargetQuery implements TargetQuery {

    private final TargetQuery first;
    private final TargetQuery second;

    IntersectingTargetQuery(TargetQuery first, TargetQuery second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        List<EnemyMob> matches = new ArrayList<>(this.first.matching(enemies));
        matches.retainAll(this.second.matching(enemies));
        return matches;
    }
}
