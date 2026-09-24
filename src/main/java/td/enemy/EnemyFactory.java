package td.enemy;

import td.util.GameWorld;

/**
 * Built-in enemies by id, for tests and simple call sites. Gameplay spawns through a level's
 * {@link EnemyCatalog}, which this bypasses.
 */
public final class EnemyFactory {

    private EnemyFactory() {
    }

    public static boolean isEnemy(String name) {
        return EnemyCatalog.builtIn().contains(name);
    }

    public static EnemyMob getEnemy(String name, GameWorld gameWorld, int delay, int health, int price, Rank rank) {
        return EnemyCatalog.builtIn().spawn(name, gameWorld, delay, health, price, rank);
    }
}
