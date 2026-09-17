package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

/**
 * Picks at most one enemy out of a candidate list already produced by a {@link TargetQuery}.
 */
public interface TargetSelector {
    Optional<EnemyMob> selectFrom(List<EnemyMob> candidates);
}
