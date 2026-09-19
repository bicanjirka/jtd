package td.wave;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
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
 * <p>
 * <strong>A wave belongs to exactly one of the level's paths</strong> ({@link #pathIndex}), and
 * carries its own {@link #speedMultiplier} - the product of that path's own
 * {@code PathDefinition.speedMultiplier()} and this wave's {@code WaveDefinition
 * .speedMultiplier()}, resolved once by {@code GameEngine.loadLevel}. See
 * {@code td/wave/CLAUDE.md}'s round model for how multiple paths' waves start together.
 */
public class Wave {

    // The footprint a shaped slot's formation fills is the path's own corridor width, pulled in
    // by this fraction so a member's body doesn't itself hang over the edge.
    private static final double PATH_WIDTH_MARGIN_FRACTION = 0.7;

    private final GameWorld gameWorld;
    private final int baseHealth;
    private final int basePrice;
    private final Rank rank;
    private final WaveContent content;
    private final long scatterSeed;
    private final int pathIndex;
    private final float speedMultiplier;

    /**
     * On path 0 at {@code 1x} speed - every pre-existing caller (every wave, before this
     * feature, belonged to the level's only path at its only pace) needs to name neither.
     */
    public Wave(GameWorld gameWorld, int baseHealth, int basePrice, Rank rank, WaveContent content, long scatterSeed) {
        this(gameWorld, baseHealth, basePrice, rank, content, scatterSeed, 0, 1f);
    }

    public Wave(GameWorld gameWorld, int baseHealth, int basePrice, Rank rank, WaveContent content, long scatterSeed,
                int pathIndex, float speedMultiplier) {
        this.gameWorld = gameWorld;
        this.baseHealth = baseHealth;
        this.basePrice = basePrice;
        this.rank = rank;
        this.content = content;
        this.scatterSeed = scatterSeed;
        this.pathIndex = pathIndex;
        this.speedMultiplier = speedMultiplier;
    }

    private static List<EnemyMob> spawnEnemies(GameWorld gameWorld, WaveContent content, int baseHealth, int basePrice,
                                                 Rank rank, long scatterSeed, int pathIndex, float speedMultiplier) {
        List<EnemyMob> enemies = new ArrayList<>();
        int delay = 0;
        for (WaveSlot slot : content.spawnSequence()) {
            enemies.addAll(spawnSlot(slot, gameWorld, delay, baseHealth, basePrice, rank, scatterSeed, pathIndex, speedMultiplier));
            delay++;
        }
        return List.copyOf(enemies);
    }

    private static List<EnemyMob> spawnSlot(WaveSlot slot, GameWorld gameWorld, int delay, int health, int price,
                                              Rank rank, long scatterSeed, int pathIndex, float speedMultiplier) {
        return switch (slot) {
            case EnemySlot s -> spawnShaped(s, gameWorld, delay, health, price, rank, scatterSeed, pathIndex, speedMultiplier);
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
     * <p>
     * The shape's own {@code speedMultiplier()} is composed with this wave's {@code
     * speedMultiplier} (its path's pace times its own) into the one value handed to
     * {@link SpawnParameters}, which is what lets a fast path or a called-out fast round affect
     * a mob's speed with no new mechanism: that composed value is stored once as
     * {@code DefinedEnemyMob.shapeSpeedMultiplier} and reused on every {@code doDamage} speed
     * recompute, the same way a boss's own 50% multiplier already survives combat.
     */
    private static List<EnemyMob> spawnShaped(EnemySlot enemySlot, GameWorld gameWorld, int delay, int baseHealth,
                                                int basePrice, Rank rank, long scatterSeed, int pathIndex, float speedMultiplier) {
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
                    bountyShares[i], shape.sizeMultiplier(), shape.speedMultiplier() * speedMultiplier,
                    shape.damageTakenMultiplier(), localOffset, pathIndex);
            members.add(new DefinedEnemyMob(definition, gameWorld, spawnParameters, rank));
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
        return spawnEnemies(this.gameWorld, this.content, this.baseHealth, this.basePrice, this.rank, this.scatterSeed,
                this.pathIndex, this.speedMultiplier)
                .toArray(new EnemyMob[0]);
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public Rank getRank() {
        return this.rank;
    }

    /**
     * Which of the level's paths this wave belongs to - what a wave-info panel resolves a
     * matching {@code PathColor} swatch from, via {@code GameWorld.level().paths()}.
     */
    public int getPathIndex() {
        return this.pathIndex;
    }

}
