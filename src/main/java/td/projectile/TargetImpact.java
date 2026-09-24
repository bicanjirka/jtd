package td.projectile;

import td.enemy.HitReceiver;

/**
 * Where a {@link MissileProjectile} lands, bound by its tower. The mob hit may not be the one it
 * was fired at.
 */
@FunctionalInterface
public interface TargetImpact {
    void onImpact(HitReceiver target);
}
