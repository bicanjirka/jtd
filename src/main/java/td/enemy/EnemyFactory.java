package td.enemy;

import td.util.GameWorld;

/**
 * A stable, global-catalog convenience for test code and simple call sites that just want "the
 * built-in enemy named X" without needing per-level catalog scoping. {@link EnemyCatalog} is the
 * general mechanism (register/clone per level, spawn from an arbitrary definition); this is a
 * thin wrapper over a freshly built {@link EnemyCatalog#builtIn()} for the common case. Real
 * gameplay spawning ({@code WaveScript}/{@code Wave}/{@code GameEngine}) goes through
 * {@link EnemyCatalog} directly, not this class, since it needs per-level scoping this doesn't
 * offer.
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
