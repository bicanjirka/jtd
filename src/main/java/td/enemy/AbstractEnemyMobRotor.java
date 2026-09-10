package td.enemy;

/**
 * A mob drawn spinning at a constant rate, independent of where the path leads. The rotation
 * accumulates per tick (not per frame) and stops while the mob is waiting to spawn or already
 * dead, so a fading corpse holds the angle it died at. Subclasses set {@code rotPerTime}
 * before {@code doInit} to change speed or direction.
 */
public abstract class AbstractEnemyMobRotor extends AbstractEnemyMob {

    /** Radians per tick; negative spins the other way. */
    protected float rotPerTime = (float) Math.toRadians(5.0);
    private double facingRadians;
    private int lastTickTime;

    public void doTick(int gameTime) {
        super.doTick(gameTime);
        if (!this.inactive && !this.dead) {
            this.facingRadians += this.rotPerTime * (gameTime - this.lastTickTime);
        }
        this.lastTickTime = gameTime;
    }

    public double getFacingRadians() {
        return this.facingRadians;
    }
}
