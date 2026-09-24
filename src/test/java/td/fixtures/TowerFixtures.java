package td.fixtures;

import td.util.GameWorld;

public final class TowerFixtures {

    private TowerFixtures() {
    }

    /**
     * Ticks until no projectile is left, capped at 50 ticks so a test that never fires fails fast.
     */
    public static void flyProjectilesToCompletion(GameWorld world) {
        for (int t = 1; t <= 50 && !world.projectiles().getProjectiles().isEmpty(); t++) {
            world.projectiles().doTick(t);
        }
    }
}
