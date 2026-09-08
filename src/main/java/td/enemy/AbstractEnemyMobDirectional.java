package td.enemy;

public abstract class AbstractEnemyMobDirectional extends AbstractEnemyMob {

    // Uses the path's own exact facing at this mob's position (see getPathFacingRadians())
    // rather than atan2 of one tick's pixel delta: at real-world sub-pixel-per-tick speeds,
    // a delta-based facing intermittently snaps to 0 whenever a tick's movement rounds away
    // to nothing on one axis - invisible only while movement stayed axis-aligned.
    public double getFacingRadians() {
        return this.getPathFacingRadians();
    }
}
