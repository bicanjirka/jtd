package td.tower.splash;

import td.enemy.EnemyMob;

import java.util.List;

/**
 * How a shot landed.
 *
 * @param critical whether it counts as a crit for what triggers off one: an arc crit, or a
 *                 Thunderstrike; a Thunderclap's own crits don't count
 * @param arced    every enemy an arc struck, in order
 */
public record ShotResult(boolean critical, List<EnemyMob> arced) {

    public ShotResult {
        arced = List.copyOf(arced);
    }
}
