package td.projectile;

/** Double dispatch over the concrete {@link Projectile} types. */
public interface ProjectileVisitor<R> {
    R visitCannonball(CannonballProjectile projectile);

    R visitMissile(MissileProjectile projectile);
}
