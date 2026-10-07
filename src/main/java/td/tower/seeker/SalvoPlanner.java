package td.tower.seeker;

import td.enemy.EnemyMob;
import td.tower.targeting.TargetSelector;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Picks the target of one missile: the selector's choice among the enemies not yet {@code avoided},
 * or among all of them when every one has been. Pure: one candidate list per launch.
 */
public final class SalvoPlanner {

    private SalvoPlanner() {
    }

    public static Optional<EnemyMob> pick(TargetSelector selector, List<EnemyMob> candidates, Set<EnemyMob> avoided) {
        List<EnemyMob> fresh = candidates.stream().filter(enemy -> !avoided.contains(enemy)).toList();
        return selector.selectFrom(fresh.isEmpty() ? candidates : fresh);
    }
}
