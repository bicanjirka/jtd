package td.enemy;

import td.util.GameWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a wave-script letter to a concrete enemy type and constructs it. {@link Enemy} is the
 * closed set of spawnable types - its {@code create} switch has no {@code default}, so adding
 * a constant without wiring up its class is a compile error rather than a silent gap.
 */
public class EnemyFactory {

    private static final Map<String, Enemy> table = new HashMap<>();

    static {
        for (Enemy enemy : Enemy.values()) {
            table.put(enemy.getName(), enemy);
        }
    }

    public static boolean isEnemy(String name) {
        return table.containsKey(name);
    }

    public static Enemy identifyEnemy(String name) {
        return table.get(name);
    }

    public static EnemyMob getEnemy(String name, GameWorld context, int delay, int health, int price, int level) {
        return table.get(name).create(context, delay, health, price, level);
    }

    /** The spawnable enemy types and their wave-script letters - see the mini-language table in CLAUDE.md. */
    public enum Enemy {
        Circle("c"),
        Square("s"),
        Triangle("t"),
        Ghost("g"),
        Empty("e");

        private final String name;

        Enemy(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public EnemyMob create(GameWorld gameWorld, int delay, int health, int price, int level) {
            return switch (this) {
                case Circle -> new EnemyMobCircle(gameWorld, delay, health, price, level);
                case Square -> new EnemyMobSquare(gameWorld, delay, health, price, level);
                case Triangle -> new EnemyMobTriangle(gameWorld, delay, health, price, level);
                case Ghost -> new EnemyMobGhost(gameWorld, delay, health, price, level);
                case Empty -> new EnemyMobEmpty(gameWorld, delay, health, price, level);
            };
        }
    }
}
