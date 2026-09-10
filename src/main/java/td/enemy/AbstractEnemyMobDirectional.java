package td.enemy;

/**
 * A mob drawn facing the direction it is travelling. No concrete enemy extends this today -
 * the two that carry a facing angle spin instead (see {@link AbstractEnemyMobRotor}) - but it
 * is the correct base for one that should point along the path.
 */
public abstract class AbstractEnemyMobDirectional extends AbstractEnemyMob {

    /**
     * The path's own exact facing at this mob's position, rather than {@code atan2} of one
     * tick's pixel delta: at real-world sub-pixel-per-tick speeds, a delta-based facing
     * intermittently snaps to 0 whenever a tick's movement rounds away to nothing on one axis
     * - a defect invisible only while movement stayed axis-aligned.
     */
    public double getFacingRadians() {
        return this.getPathFacingRadians();
    }
}
