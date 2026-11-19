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
 * One path's wave: its parsed {@link WaveContent}, turned into live enemies by {@link #spawn()}.
 * Each slot's delay comes from the slots before it, and every {@link EnemySlot} already carries its
 * resolved definition and rank.
 * <p>
 * <strong>Spawning waits for {@link #spawn()}</strong>, because a mob binds to the installed path
 * when built; this is what lets a level install in one write. Carries its path index and its
 * combined path and wave speed multiplier.
 */
public class Wave {

    // The footprint a shaped slot's formation fills is the path's own corridor width, pulled in
    // by this fraction so a member's body doesn't itself hang over the edge.
    private static final double PATH_WIDTH_MARGIN_FRACTION = 0.7;

    private final GameWorld gameWorld;
    private final WaveContent content;
    private final long scatterSeed;
    private final int pathIndex;
    private final float speedMultiplier;
    private final int clearBonus;

    /** On path 0 at normal speed. */
    public Wave(GameWorld gameWorld, WaveContent content, long scatterSeed) {
        this(gameWorld, content, scatterSeed, 0, 1f);
    }

    /** With no clear bonus. */
    public Wave(GameWorld gameWorld, WaveContent content, long scatterSeed, int pathIndex, float speedMultiplier) {
        this(gameWorld, content, scatterSeed, pathIndex, speedMultiplier, 0);
    }

    private Wave(GameWorld gameWorld, WaveContent content, long scatterSeed, int pathIndex, float speedMultiplier,
                 int clearBonus) {
        this.gameWorld = gameWorld;
        this.content = content;
        this.scatterSeed = scatterSeed;
        this.pathIndex = pathIndex;
        this.speedMultiplier = speedMultiplier;
        this.clearBonus = clearBonus;
    }

    /** This wave paying {@code bonus} credits when its round clears with no enemy leaked. */
    public Wave withClearBonus(int bonus) {
        return new Wave(this.gameWorld, this.content, this.scatterSeed, this.pathIndex, this.speedMultiplier, bonus);
    }

    /**
     * Each slot enters once the enemy before it has walked one slot of spacing, so the script's order is the order
     * they enter in, and any two neighbours enter the same distance apart whatever their speeds. A spacer is one
     * more slot that enemy walks.
     */
    private static List<EnemyMob> spawnEnemies(GameWorld gameWorld, WaveContent content, long scatterSeed,
                                                 int pathIndex, float speedMultiplier) {
        List<EnemyMob> enemies = new ArrayList<>();
        float delayTicksPerSlot = content.delayTicksPerSlot() * gameWorld.playtestRules().spawnSpacingScale();
        double startTicks = 0;
        float previousSpeed = 1f;
        int slotIndex = 0;
        for (WaveSlot slot : content.spawnSequence()) {
            previousSpeed = switch (slot) {
                case EnemySlot enemySlot -> {
                    float speed = walkingSpeed(enemySlot, speedMultiplier);
                    enemies.addAll(spawnShaped(enemySlot, gameWorld, slotIndex, startTicks, delayTicksPerSlot / speed,
                            scatterSeed, pathIndex, speedMultiplier));
                    yield speed;
                }
                case EmptySlot _ -> previousSpeed;
            };
            startTicks += delayTicksPerSlot / previousSpeed;
            slotIndex++;
        }
        return List.copyOf(enemies);
    }

    /** A still enemy holds the one after it back by one slot, as an enemy of speed 1 would. */
    private static float walkingSpeed(EnemySlot slot, float speedMultiplier) {
        float speed = slot.definition().baseSpeed() * slot.shape().speedMultiplier() * speedMultiplier;
        return speed > 0 ? speed : 1f;
    }

    /**
     * Builds every member of one shaped slot: health and bounty scaled and split here, size (the
     * shape's and the affixes') and speed passed on to the mob. Members are spaced by
     * {@link SpawnShape#delaySpacingSlots()}, each slot of it {@code ticksPerSlot} at their speed.
     * <p>
     * Formation offsets draw from a random source seeded by the wave and slot, not the world's,
     * which tower targeting also uses - otherwise a formation would depend on firing order.
     */
    private static List<EnemyMob> spawnShaped(EnemySlot enemySlot, GameWorld gameWorld, int slotIndex,
                                                double startTicks, double ticksPerSlot, long scatterSeed,
                                                int pathIndex, float speedMultiplier) {
        EnemyDefinition definition = enemySlot.definition();
        Rank rank = enemySlot.rank();
        SpawnShape shape = enemySlot.shape();
        int health = Math.max(1, Math.round(definition.baseHealth() * shape.healthMultiplier()));
        int[] bountyShares = shape.bountyShares(definition.bounty());
        double maxRadius = gameWorld.getBoard().scale() * PathCoverage.PATH_WIDTH_CELLS / 2.0 * PATH_WIDTH_MARGIN_FRACTION;
        RandomSource scatter = RandomSource.seeded(scatterSeed * 31 + slotIndex);
        List<EnemyMob> members = new ArrayList<>(shape.members());
        for (int i = 0; i < shape.members(); i++) {
            double delayTicks = startTicks + i * shape.delaySpacingSlots() * ticksPerSlot;
            Vec2 localOffset = shape.spread().offsetFor(i, shape.members(), maxRadius, scatter);
            SpawnParameters spawnParameters = SpawnParameters.afterTicks(delayTicks, health, bountyShares[i],
                    shape.sizeMultiplier() * definition.sizeMultiplier(), shape.speedMultiplier() * speedMultiplier,
                    localOffset, pathIndex);
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

    /** The resolved rank of the slot spawning {@code definition}. */
    public Rank rankFor(EnemyDefinition definition) {
        return this.content.rankFor(definition);
    }

    public Rank highestRank() {
        return this.content.highestRank();
    }

    public boolean hasUnique() {
        return this.content.hasUnique();
    }

    /** What every enemy of this wave's script pays when killed, before anything they spawn in play. */
    public int bounty() {
        return this.content.bounty();
    }

    /**
     * Builds this wave's enemies against the currently installed path. Call once, when the wave
     * starts: each call makes a fresh set.
     */
    public EnemyMob[] spawn() {
        return spawnEnemies(this.gameWorld, this.content, this.scatterSeed, this.pathIndex, this.speedMultiplier)
                .toArray(new EnemyMob[0]);
    }

    public int getPathIndex() {
        return this.pathIndex;
    }

    /** Credits this wave adds when its round clears with no enemy leaked. */
    public int clearBonus() {
        return this.clearBonus;
    }

}
