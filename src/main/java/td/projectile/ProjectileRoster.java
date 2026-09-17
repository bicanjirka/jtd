package td.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The projectiles currently in flight. A {@code CopyOnWriteArrayList}, like
 * {@code TowerRoster}'s tower list, because a tower spawns a projectile from the game-loop
 * thread while this same list is read from the EDT for rendering.
 */
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
     * Advances every live projectile one tick, then drops whichever ones just finished.
     * Finished projectiles are collected into a plain list and removed afterward rather than
     * through an iterator - {@code CopyOnWriteArrayList}'s iterator does not support removal.
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

    /**
     * Level teardown: no projectile in flight carries over to the next level.
     */
    public void clear() {
        this.projectiles.clear();
    }
}
