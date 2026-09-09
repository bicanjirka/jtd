package td.wave;

import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A wave's {@link WaveContent} turned into live, world-bound enemies. Parsing the token
 * string into content is {@link WaveScript}'s job - this class only spawns it: each spawn
 * slot becomes one {@code EnemyMob} whose delay is its position in the flattened content,
 * so a later slot spawns later regardless of whether it's a real enemy or an Empty spacer.
 */
public class Wave {

    private final int baseHealth;
    private final int basePrice;
    private final int level;
    private final WaveContent content;
    private final List<EnemyMob> enemies;

    public Wave(GameWorld context, int baseHealth, int basePrice, int level, WaveContent content) {
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.level = level;
        this.content = content;
        this.enemies = spawnEnemies(context, content, baseHealth, basePrice, level);
    }

    private static List<EnemyMob> spawnEnemies(GameWorld context, WaveContent content, int baseHealth, int basePrice, int level) {
        List<EnemyMob> enemies = new ArrayList<>();
        int delay = 0;
        for (EnemyFactory.Enemy enemy : content.spawnSequence()) {
            enemies.add(enemy.create(context, delay, baseHealth, basePrice, level));
            delay++;
        }
        return List.copyOf(enemies);
    }

    public Set<EnemyFactory.Enemy> enemySet() {
        return this.content.enemySet();
    }

    public int enemyCount(EnemyFactory.Enemy e) {
        return this.content.enemyCount(e);
    }

    public int enemyCount() {
        return this.content.enemyCount();
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
