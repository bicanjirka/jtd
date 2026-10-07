package td.tower.sonar;

import td.enemy.EnemyMob;

import java.util.List;

/**
 * A finished revolution.
 *
 * @param pinged the enemies its ping Exposed, healthiest first
 * @param spec   how the Sonar was scanning during it
 */
public record Revolution(List<EnemyMob> pinged, SonarSpec spec) {
}
