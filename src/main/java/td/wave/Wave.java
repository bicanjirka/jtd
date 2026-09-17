package td.wave;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A wave's {@link WaveContent}, and the ability to turn it into live, world-bound enemies on
 * demand. Parsing the token string into content is {@link WaveScript}'s job - this class only
 * spawns it: each spawn slot becomes zero or more {@code EnemyMob}s (an {@link EmptySlot}
 * spacer spawns none), with a delay derived from its position in the flattened content, so a
 * later slot spawns later regardless of how many mobs the slots before it produced.
 * <p>
 * <strong>Spawning is deferred to {@link #spawn()}</strong>, which is what makes a level load
 * a single atomic publication. An {@code EnemyMob} binds to the world's installed path when it
 * is built, so building every wave's enemies in this constructor forced {@code loadLevel} to
 * install the path <em>before</em> constructing the waves - and therefore to publish the level
 * in two writes instead of one. Deferring the spawn to the moment a wave actually starts
 * removes that ordering entirely, and stops a level with eighteen waves allocating every enemy
 * of all eighteen before the first one runs.
 */
public class Wave {

    private final GameWorld gameWorld;
    private final int baseHealth;
    private final int basePrice;
    private final int level;
    private final WaveContent content;

    public Wave(GameWorld gameWorld, int baseHealth, int basePrice, int level, WaveContent content) {
        this.gameWorld = gameWorld;
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.level = level;
        this.content = content;
    }

    private static List<EnemyMob> spawnEnemies(GameWorld gameWorld, WaveContent content, int baseHealth, int basePrice, int level) {
        List<EnemyMob> enemies = new ArrayList<>();
        int delay = 0;
        for (WaveSlot slot : content.spawnSequence()) {
            enemies.addAll(spawnSlot(slot, gameWorld, delay, baseHealth, basePrice, level));
            delay++;
        }
        return List.copyOf(enemies);
    }

    private static List<EnemyMob> spawnSlot(WaveSlot slot, GameWorld gameWorld, int delay, int health, int price, int level) {
        return switch (slot) {
            case EnemySlot s -> List.of(new DefinedEnemyMob(s.definition(), gameWorld, delay, health, price, level));
            case EmptySlot ignored -> List.of();
        };
    }

    public Set<EnemyDefinition> enemySet() {
        return this.content.enemySet();
    }

    public int enemyCount(EnemyDefinition definition) {
        return this.content.enemyCount(definition);
    }

    public int enemyCount() {
        return this.content.enemyCount();
    }

    /**
     * Builds this wave's enemies against the world as it stands right now, bound to the path
     * currently installed. Called once, when the wave starts - each call produces a fresh,
     * independent set of mobs, so calling it twice would put two copies of the wave on the
     * board. The counts and definitions {@link #enemySet()} and {@link #enemyCount()} report
     * come from the content and need no spawn, which is what lets the wave-preview panel
     * describe a wave that has not run yet.
     */
    public EnemyMob[] spawn() {
        return spawnEnemies(this.gameWorld, this.content, this.baseHealth, this.basePrice, this.level)
                .toArray(new EnemyMob[0]);
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
