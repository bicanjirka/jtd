package td.tower.splash;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.zone.ZoneOwner;

/**
 * Whoever casts hexes, as its {@link HexKeeper} sees it: what the hexes put on enemies and the damage they pay out
 * are credited to it.
 */
public interface HexCaster extends ZoneOwner {

    /** A hex's payout: periodic, so it never crits. */
    void dealPeriodicDamage(EnemyMob target, Damage damage);

    /** {@code cells} in pixels, grown by however much further the caster's hexes reach. */
    float distance(float cells);

    /** Every enemy a payout may reach. */
    EnemyRegistry enemies();

    /** Leaves the cursed cloud a hexed enemy that died leaves, when its caster has Mastery. */
    void leaveCloud(EnemyMob dead);
}
