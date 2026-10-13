package td.tower.splash;

/**
 * How a shot landed.
 *
 * @param critical whether it counts as a crit for what triggers off one: an arc crit, or a
 *                 Thunderstrike; a Thunderclap's own crits don't count
 */
public record ShotResult(boolean critical) {
}
