package td.fixtures;

import td.util.GameWorld;

/**
 * Shared setup for a tower test that fires a real projectile (a shell, a missile) and needs to
 * wait for it to land before asserting on its impact.
 */
public final class TowerFixtures {

    private TowerFixtures() {
    }

    /**
     * Ticks a world's live projectiles until none remain, or 50 ticks pass - enough for any
     * built-in projectile's flight at today's speeds, with a cap so a test that never fires one
     * fails fast instead of looping.
     */
    public static void flyProjectilesToCompletion(GameWorld world) {
        for (int t = 1; t <= 50 && !world.projectiles().getProjectiles().isEmpty(); t++) {
            world.projectiles().doTick(t);
        }
    }
}
