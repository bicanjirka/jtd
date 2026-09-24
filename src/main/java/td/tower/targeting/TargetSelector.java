package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

/** Picks at most one enemy from a {@link TargetQuery}'s candidates. */
public interface TargetSelector {
    Optional<EnemyMob> selectFrom(List<EnemyMob> candidates);
}
