package td.tower.sonar;

import td.enemy.EnemyMob;

/**
 * How a beam hit landed, or that the enemy dodged it: the beam still passed it.
 *
 * @param target   the enemy hit
 * @param critical whether it crit
 * @param dodged   whether the enemy dodged it, so nothing that answers a landed hit should happen
 * @param spec     how the Sonar was scanning when it hit
 */
public record StrikeResult(EnemyMob target, boolean critical, boolean dodged, SonarSpec spec) {

    public boolean killed() {
        return this.target.isDead();
    }
}
