package td.enemy;

import td.util.GameWorld;

/**
 * A stable, global-catalog convenience for test code and simple call sites that just want "the
 * built-in enemy named X" without needing per-level catalog scoping. {@link EnemyCatalog} is the
 * general mechanism (register/clone per level, spawn from an arbitrary definition); this is a
 * thin wrapper over a freshly built {@link EnemyCatalog#builtIn()} for the common case, plus
 * {@code "e"} (the wave mini-language's spacer), which is deliberately not a registered
 * {@link EnemyCatalog} definition (see {@code WaveScript}) but still needs to be reachable here
 * since {@link EnemyMobEmpty} is real, spawnable, and useful in isolation for tests. Real
 * gameplay spawning ({@code WaveScript}/{@code Wave}/{@code GameEngine}) goes through
 * {@link EnemyCatalog} directly, not this class, since it needs per-level scoping this doesn't
 * offer.
 */
public final class EnemyFactory {

    private static final String EMPTY_TOKEN = "e";

    private EnemyFactory() {
    }

    public static boolean isEnemy(String name) {
        return name.equals(EMPTY_TOKEN) || EnemyCatalog.builtIn().contains(name);
    }

    public static EnemyMob getEnemy(String name, GameWorld gameWorld, int delay, int health, int price, int level) {
        if (name.equals(EMPTY_TOKEN)) {
            return new EnemyMobEmpty(gameWorld, delay, health, price, level);
        }
        return EnemyCatalog.builtIn().spawn(name, gameWorld, delay, health, price, level);
    }
}
