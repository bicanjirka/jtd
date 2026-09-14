package td.projectile;

/**
 * Where a {@link CannonballProjectile} sends its arrival - the tower that fired it binds this
 * to its own splash query and {@code dealDamage}, so a shell's impact is credited to its
 * tower exactly like an instant hit is. Deliberately a point, not a target: a cannonball is
 * aimed at a position, not a mob, and detonates there whether or not anything is still there
 * to hit.
 */
@FunctionalInterface
public interface PointImpact {
    void onImpact(double x, double y);
}
