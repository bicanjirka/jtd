package td.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Projectiles in flight. Towers add to it during a tick. */
public class ProjectileRoster implements ProjectileRegistry {

    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();

    @Override
    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(this.projectiles);
    }

    public void add(Projectile projectile) {
        this.projectiles.add(projectile);
    }

    /**
     * Advances every projectile, then removes the finished ones; the list's iterator cannot remove.
     */
    public void doTick(int gameTime) {
        List<Projectile> finished = new ArrayList<>();
        for (Projectile projectile : this.projectiles) {
            projectile.doTick(gameTime);
            if (projectile.isFinished()) {
                finished.add(projectile);
            }
        }
        this.projectiles.removeAll(finished);
    }

    public void clear() {
        this.projectiles.clear();
    }
}
