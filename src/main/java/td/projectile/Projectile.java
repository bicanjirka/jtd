package td.projectile;

/**
 * One shell or missile in flight. Concrete types are reached only through
 * {@link ProjectileVisitor}.
 */
public interface Projectile {

    void doTick(int gameTime);

    <R> R accept(ProjectileVisitor<R> visitor);

    double getX();

    double getY();

    /** Position as of the previous tick, the interpolation source. */
    double getPrevX();

    double getPrevY();

    /** True once resolved, so the roster drops it. */
    boolean isFinished();
}
