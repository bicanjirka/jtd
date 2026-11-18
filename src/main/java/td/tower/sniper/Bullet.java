package td.tower.sniper;

import td.enemy.EnemyMob;
import td.tower.ShotFlight;
import td.tower.TowerCapability;
import td.tower.TowerStats;

/**
 * A shot on its way. It carries the stats it was fired with, so a charge spent on it is not lost
 * however the tower changes before it lands.
 *
 * @param shot   the shot as the mechanics shaped it
 * @param stats  the stats it was fired with
 * @param target the enemy it was aimed at
 * @param flight when it flies
 */
public record Bullet(SniperShot shot, TowerStats stats, EnemyMob target, ShotFlight flight) {

    public int firedAt() {
        return this.flight.firedAt();
    }

    public int landsAt() {
        return this.flight.landsAt();
    }

    /** Whether it is sure to crit, which shows before it lands. */
    public boolean sureCrit() {
        return this.stats.has(TowerCapability.GUARANTEED_CRIT);
    }
}
