package td.projectile;

import java.util.List;

/** The read-only view of live projectiles the renderer scans. */
public interface ProjectileRegistry {
    List<Projectile> getProjectiles();
}
