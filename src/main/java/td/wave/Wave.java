package td.wave;

import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.util.Context;

import java.util.*;

public class Wave {
    private final List<EnemyMob> enemies = new ArrayList<>();
    private final List<String[]> nameStrings = new ArrayList<>();
    private final int baseHealth;
    private final int basePrice;
    private boolean finalised = false;
    private final Context context;
    private final int level;
    private int emptyMobs;

    private final Map<EnemyFactory.Enemy, Integer> enemyCounts = new HashMap<>();

    public Wave(Context context, int baseHealth, int basePrice, int level) {
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.context = context;
        this.level = level;
    }

    public void addEnemiesFromNames(String[] names) {
        this.nameStrings.add(names);
    }

    private void countEnemy(EnemyFactory.Enemy e, int count) {
        enemyCounts.merge(e, count, Integer::sum);
    }

    public Set<EnemyFactory.Enemy> enemySet() {
        if (!this.finalised) {
            this.finalise();
        }
        return enemyCounts.keySet();
    }

    public int enemyCount(EnemyFactory.Enemy e) {
        if (!this.finalised) {
            this.finalise();
        }
        Integer i = enemyCounts.get(e);
        if (i == null) {
            return 0;
        }
        return i;
    }

    public int enemyCount() {
        if (!this.finalised) {
            this.finalise();
        }
        return this.enemies.size() - this.emptyMobs;
    }

    private void finalise() {
        for (String[] names : this.nameStrings) {
            int nr = 1;
            int count = 0;
            for (String name : names) {
                if (EnemyFactory.isEnemy(name)) {
                    this.countEnemy(EnemyFactory.identifyEnemy(name), nr);
                    if (name.equals(EnemyFactory.Enemy.Empty.getName())) {
                        this.emptyMobs += nr;
                    }
                    for (int e = 0; e < nr; e++) {
                        enemies.add(EnemyFactory.getEnemy(name, this.context, count, this.baseHealth, this.basePrice, this.level));
                        count++;
                    }
                    nr = 1;
                } else {
                    try {
                        nr = Integer.parseInt(name);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                        nr = 1;
                    }
                }
            }
        }
        this.finalised = true;
    }

    public EnemyMob[] getEnemies() {
        if (!this.finalised) {
            this.finalise();
        }
        return enemies.toArray(new EnemyMob[0]);
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

    public List<String[]> getNameStrings() {
        return this.nameStrings;
    }

}