package td.projectile;

/** Dispatches over the closed set of concrete projectile kinds - see {@link Projectile}. */
public interface ProjectileVisitor<R> {
    R visitCannonball(CannonballProjectile projectile);

    R visitMissile(MissileProjectile projectile);
}
