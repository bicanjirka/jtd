package td.projectile;

/**
 * Where a {@link CannonballProjectile} lands, bound by its tower so the blast is credited like an
 * instant hit. A point, not a target: the shell detonates whether or not anything is there.
 */
@FunctionalInterface
public interface PointImpact {
    void onImpact(double x, double y);
}
