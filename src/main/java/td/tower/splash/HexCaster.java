package td.tower.splash;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

/**
 * Whoever casts hexes, as its {@link HexKeeper} sees it: what the hexes pay out is credited to it, and a hex that
 * jumps on is its to send.
 */
public interface HexCaster {

    /** A hex's payout: periodic, so it never crits. */
    void dealPeriodicDamage(EnemyMob target, Damage damage);

    /** {@code cells} in pixels. */
    float distance(float cells);

    /** Every enemy a hex may jump to. */
    EnemyRegistry enemies();

    /** Sends a hex on from an enemy that died with it. */
    void jump(HexJump jump);
}
