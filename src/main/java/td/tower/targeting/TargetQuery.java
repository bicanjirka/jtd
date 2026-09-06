package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.Context;

import java.util.List;

/**
 * Answers "which enemies are legal targets right now", as a fresh immutable
 * snapshot - never {@code Context}'s live enemy array itself. Replaces each
 * tower hand-rolling its own scan over {@code context.getEnemies()}.
 */
public interface TargetQuery {
    List<EnemyMob> matching(Context context);
}
