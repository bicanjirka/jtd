package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;

/** Enemies either query matches, each once, the first query's matches first. */
final class UnionTargetQuery implements TargetQuery {

    private final TargetQuery first;
    private final TargetQuery second;

    UnionTargetQuery(TargetQuery first, TargetQuery second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemies) {
        List<EnemyMob> matches = new ArrayList<>(this.first.matching(enemies));
        for (EnemyMob enemy : this.second.matching(enemies)) {
            if (!matches.contains(enemy)) {
                matches.add(enemy);
            }
        }
        return matches;
    }
}
