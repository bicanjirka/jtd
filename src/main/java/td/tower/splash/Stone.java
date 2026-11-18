package td.tower.splash;

import td.tower.ShotFlight;
import td.tower.TowerStats;
import td.wave.Vec2;

/**
 * A stone on its way: unguided, from the tower to where its target stood, with the stats it was thrown with.
 *
 * @param charged whether it carries an Arc
 */
public record Stone(Vec2 to, TowerStats stats, boolean charged, ShotFlight flight) {
}
