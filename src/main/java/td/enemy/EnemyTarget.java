package td.enemy;

/** An enemy as targeting and aiming see it: where it is and whether it may be shot. */
public interface EnemyTarget {

    double getX();

    double getY();

    /** How far along its lap this mob is; for ranking only. */
    int getProgression();

    boolean validTarget();

    boolean validTarget(EnemyMob.Type type);
}
