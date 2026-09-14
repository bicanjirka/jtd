package td.projectile;

import td.enemy.EnemyMob;

/**
 * Where a {@link MissileProjectile} sends its arrival - the tower that fired it binds this to
 * its own {@code dealDamage} on the specific mob the missile reached, which may not be the
 * mob it was originally fired at (see {@link MissileProjectile}'s retargeting).
 */
@FunctionalInterface
public interface TargetImpact {
    void onImpact(EnemyMob target);
}
