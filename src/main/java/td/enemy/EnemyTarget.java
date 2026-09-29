package td.enemy;

/**
 * An enemy as targeting and aiming see it: where it is, whether it may be affected, and whether it
 * may be aimed at.
 */
public interface EnemyTarget {

    double getX();

    double getY();

    /** How far along its lap this mob is; for ranking only. */
    int getProgression();

    /** Spawned, on the board and alive: anything an area effect may touch, hidden or not. */
    boolean validTarget();

    /** Whether stealth keeps towers from picking this mob as a target. */
    boolean isHidden();

    /** Whether a tower may pick this mob as the target of a shot, an aim or a trigger. */
    default boolean canBeTargeted() {
        return this.validTarget() && !this.isHidden();
    }
}
