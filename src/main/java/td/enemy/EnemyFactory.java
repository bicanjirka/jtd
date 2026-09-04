package td.enemy;

import td.util.Context;

import java.util.HashMap;
import java.util.Map;

public class EnemyFactory {

    private static final Map<String, Enemy> table = new HashMap<String, Enemy>();

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

    public static EnemyMob getEnemy(String name, Context context, int delay, int health, int price, int level) {
        return table.get(name).getCopy(context, delay, health, price, level);
    }

    public enum Enemy {
        Circle("c", new EnemyMobCircle()),
        Square("s", new EnemyMobSquare()),
        Triangle("t", new EnemyMobTriangle()),
        Ghost("g", new EnemyMobGhost()),
        Empty("e", new EnemyMobEmpty());

        private final String name;
        private final EnemyMob instance;

        Enemy(String name, EnemyMob instance) {
            this.name = name;
            this.instance = instance;
        }

        public String getName() {
            return this.name;
        }

        public EnemyMob getCopy(Context context, int delay, int health, int price, int level) {
            return instance.create(context, delay, health, price, level);
        }
    }
}
