package td.enemy;

import td.util.GameWorld;

import java.util.HashMap;
import java.util.Map;

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

        public EnemyMob create(GameWorld context, int delay, int health, int price, int level) {
            return switch (this) {
                case Circle -> new EnemyMobCircle(context, delay, health, price, level);
                case Square -> new EnemyMobSquare(context, delay, health, price, level);
                case Triangle -> new EnemyMobTriangle(context, delay, health, price, level);
                case Ghost -> new EnemyMobGhost(context, delay, health, price, level);
                case Empty -> new EnemyMobEmpty(context, delay, health, price, level);
            };
        }
    }
}
