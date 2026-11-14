package td.tower.splash;

import td.enemy.EnemyMob;
import td.wave.Vec2;

/**
 * A hex jumping on, as it was, from an enemy that died with it.
 *
 * @param hex       the hex, with how long it has left as cast: what lengthened or shortened it on the dead enemy is
 *                  taken out
 * @param from      where the enemy died, in pixels
 * @param to        the enemy it jumps to
 * @param grownSlow the slow it had grown to, for a hex that slows
 */
public record HexJump(HexEvent.CarriedHex hex, Vec2 from, EnemyMob to, float grownSlow) {
}
