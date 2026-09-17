package td.projectile;

/**
 * One shell or missile in flight, as seen by the roster and the renderer. The implementation
 * hierarchy lives behind {@link AbstractProjectile}; concrete types are reached only through
 * {@link ProjectileVisitor}, never by casting or {@code instanceof} - the same discipline
 * {@code EnemyMob}/{@code Tower} already follow.
 */
public interface Projectile {

    void doTick(int gameTime);

    <R> R accept(ProjectileVisitor<R> visitor);

    double getX();

    double getY();

    /**
     * This projectile's position as of the tick before last - the interpolation source for a render landing between two ticks.
     */
    double getPrevX();

    double getPrevY();

    /**
     * True once this projectile has resolved (hit something, or given up) and is ready to be dropped from the roster.
     */
    boolean isFinished();
}
