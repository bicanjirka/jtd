package td.projectile;

import java.util.List;

/**
 * The live projectiles a renderer scans - the read-only slice of {@link ProjectileRoster}.
 */
public interface ProjectileRegistry {
    List<Projectile> getProjectiles();
}
