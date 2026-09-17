package td.wave;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.SpawnParameters;
import td.util.GameWorld;
import td.util.RandomSource;

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

    // The footprint a shaped slot's formation fills is the path's own corridor width, pulled in
    // by this fraction so a member's body doesn't itself hang over the edge.
    private static final double PATH_WIDTH_MARGIN_FRACTION = 0.7;

    private final GameWorld gameWorld;
    private final int baseHealth;
    private final int basePrice;
    private final int level;
    private final WaveContent content;
    private final long scatterSeed;

    public Wave(GameWorld gameWorld, int baseHealth, int basePrice, int level, WaveContent content, long scatterSeed) {
        this.gameWorld = gameWorld;
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.level = level;
        this.content = content;
        this.scatterSeed = scatterSeed;
    }

    private static List<EnemyMob> spawnEnemies(GameWorld gameWorld, WaveContent content, int baseHealth, int basePrice, int level, long scatterSeed) {
        List<EnemyMob> enemies = new ArrayList<>();
        int delay = 0;
        for (WaveSlot slot : content.spawnSequence()) {
            enemies.addAll(spawnSlot(slot, gameWorld, delay, baseHealth, basePrice, level, scatterSeed));
            delay++;
        }
        return List.copyOf(enemies);
    }

    private static List<EnemyMob> spawnSlot(WaveSlot slot, GameWorld gameWorld, int delay, int health, int price, int level, long scatterSeed) {
        return switch (slot) {
            case EnemySlot s -> spawnShaped(s, gameWorld, delay, health, price, level, scatterSeed);
            case EmptySlot ignored -> List.of();
        };
    }

    /**
     * Builds every member of one shaped slot: the shape's health and bounty multipliers are
     * arithmetic here, against the wave's own base health/price (bounty split exactly, via
     * {@link SpawnShape#bountyShares}); the size and speed multipliers pass straight through to
     * {@link SpawnParameters}, which folds them into the mob itself. A member's own slot
     * position is this slot's index plus its member index scaled by
     * {@link SpawnShape#delaySpacingSlots()} - zero for every shape but Column and Drip, so
     * every other shape's members still share the slot's own position. A member's formation
     * offset comes from {@link SpawnShape#spread()}, drawn from a {@link RandomSource} seeded
     * from this wave's own {@code scatterSeed} and the slot's index - deliberately not
     * {@code gameWorld.random()}, which tower targeting also draws from, so a formation's shape
     * would otherwise depend on how many towers happened to fire first. The offset itself is
     * relative to the mob's own spawn-facing direction, not world space -
     * {@link td.enemy.AbstractEnemyMob} is what fixes it into a world vector, once, at spawn.
     */
    private static List<EnemyMob> spawnShaped(EnemySlot enemySlot, GameWorld gameWorld, int delay, int baseHealth, int basePrice, int level, long scatterSeed) {
        EnemyDefinition definition = enemySlot.definition();
        SpawnShape shape = enemySlot.shape();
        int health = Math.max(1, Math.round(baseHealth * shape.healthMultiplier()));
        int[] bountyShares = shape.bountyShares(basePrice);
        double maxRadius = gameWorld.getBoard().scale() * PathCoverage.PATH_WIDTH_CELLS / 2.0 * PATH_WIDTH_MARGIN_FRACTION;
        RandomSource scatter = RandomSource.seeded(scatterSeed * 31 + delay);
        List<EnemyMob> members = new ArrayList<>(shape.members());
        for (int i = 0; i < shape.members(); i++) {
            double slotPosition = delay + i * shape.delaySpacingSlots();
            Vec2 localOffset = shape.spread().offsetFor(i, shape.members(), maxRadius, scatter);
            SpawnParameters spawnParameters = SpawnParameters.of(slotPosition, definition.baseSpeed(), health,
                    bountyShares[i], shape.sizeMultiplier(), shape.speedMultiplier(), localOffset);
            members.add(new DefinedEnemyMob(definition, gameWorld, spawnParameters, level));
        }
        return List.copyOf(members);
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
        return spawnEnemies(this.gameWorld, this.content, this.baseHealth, this.basePrice, this.level, this.scatterSeed)
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
