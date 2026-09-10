package td.enemy;

import td.damage.Damage;

/**
 * One enemy walking the level's path, as seen by towers, targeting queries and the renderer.
 * The implementation hierarchy lives behind {@link AbstractEnemyMob}; concrete types are
 * reached only through {@link EnemyMobVisitor}, never by casting or {@code instanceof}.
 */
public interface EnemyMob {
    void doTick(int gameTime);

    <R> R accept(EnemyMobVisitor<R> visitor);

    double getX();

    double getY();

    /** How far along its current lap of the path this mob is - a ranking value only, not a distance to rely on. */
    int getProgression();

    boolean validTarget();

    boolean validTarget(type type);

    boolean validTarget(type type0, type type1);

    long getHealth();

    boolean isDead();

    void doDamage(Damage damage);

    float getSpeed();

    String getInfoString();

    /**
     * What a tower is allowed to see. {@code Invisible} (the ghost) is skipped by
     * single-target towers and reachable only by area damage; {@code Flying} is declared but
     * unused by any enemy today.
     */
    enum type {
        Normal,
        Flying,
        Invisible
    }
}
