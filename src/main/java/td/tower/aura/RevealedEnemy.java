package td.tower.aura;

import td.enemy.EnemyMob;

/** A hidden enemy the light revealed as it entered, and the tick it did. */
public record RevealedEnemy(EnemyMob enemy, int atTick) {
}
