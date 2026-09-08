package td.wave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Wave {

    private static final Logger LOG = LoggerFactory.getLogger(Wave.class);

    private final int baseHealth;
    private final int basePrice;
    private final int level;
    private final List<EnemyMob> enemies;
    private final Map<EnemyFactory.Enemy, Integer> enemyCounts;
    private final int emptyMobCount;

    public Wave(GameWorld context, int baseHealth, int basePrice, int level, String[] names) {
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.level = level;

        ParsedNames parsed = parseNames(names, context, baseHealth, basePrice, level);
        this.enemies = parsed.enemies();
        this.enemyCounts = parsed.enemyCounts();
        this.emptyMobCount = parsed.emptyMobCount();
    }

    private record ParsedNames(List<EnemyMob> enemies, Map<EnemyFactory.Enemy, Integer> enemyCounts, int emptyMobCount) {
    }

    private static ParsedNames parseNames(String[] names, GameWorld context, int baseHealth, int basePrice, int level) {
        List<EnemyMob> enemies = new ArrayList<>();
        Map<EnemyFactory.Enemy, Integer> enemyCounts = new HashMap<>();
        int emptyMobCount = 0;
        int nr = 1;
        int count = 0;
        for (String name : names) {
            if (EnemyFactory.isEnemy(name)) {
                enemyCounts.merge(EnemyFactory.identifyEnemy(name), nr, Integer::sum);
                if (name.equals(EnemyFactory.Enemy.Empty.getName())) {
                    emptyMobCount += nr;
                }
                for (int e = 0; e < nr; e++) {
                    enemies.add(EnemyFactory.getEnemy(name, context, count, baseHealth, basePrice, level));
                    count++;
                }
                nr = 1;
            } else {
                try {
                    nr = Integer.parseInt(name);
                } catch (NumberFormatException ex) {
                    LOG.warn("Unrecognized wave token '{}' at level {}, treating as x1", name, level, ex);
                    nr = 1;
                }
            }
        }
        return new ParsedNames(List.copyOf(enemies), Map.copyOf(enemyCounts), emptyMobCount);
    }

    public Set<EnemyFactory.Enemy> enemySet() {
        return this.enemyCounts.keySet();
    }

    public int enemyCount(EnemyFactory.Enemy e) {
        return this.enemyCounts.getOrDefault(e, 0);
    }

    public int enemyCount() {
        return this.enemies.size() - this.emptyMobCount;
    }

    public EnemyMob[] getEnemies() {
        return this.enemies.toArray(new EnemyMob[0]);
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public int getLevel() {
        return level;
    }

}
