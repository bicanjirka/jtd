package td.tower;

import td.enemy.EnemyMob;
import td.enemy.HitResult;

/** Told on the game-loop thread of every hit a tower lands on a living enemy, damage over time included. */
@FunctionalInterface
public interface TowerHitListener {
    void landed(Tower tower, EnemyMob enemy, HitResult result);
}
