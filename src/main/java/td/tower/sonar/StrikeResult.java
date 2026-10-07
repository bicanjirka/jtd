package td.tower.sonar;

import td.enemy.EnemyMob;

/**
 * How a beam hit landed.
 *
 * @param target   the enemy hit
 * @param critical whether it crit
 * @param spec     how the Sonar was scanning when it hit
 */
public record StrikeResult(EnemyMob target, boolean critical, SonarSpec spec) {

    public boolean killed() {
        return this.target.isDead();
    }
}
