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
 * Each slot's delay comes from its position, and every {@link EnemySlot} already carries its
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

    /** On path 0 at normal speed. */
    public Wave(GameWorld gameWorld, WaveContent content, long scatterSeed) {
        this(gameWorld, content, scatterSeed, 0, 1f);
    }

    public Wave(GameWorld gameWorld, WaveContent content, long scatterSeed, int pathIndex, float speedMultiplier) {
        this.gameWorld = gameWorld;
        this.content = content;
        this.scatterSeed = scatterSeed;
        this.pathIndex = pathIndex;
        this.speedMultiplier = speedMultiplier;
    }

    private static List<EnemyMob> spawnEnemies(GameWorld gameWorld, WaveContent content, long scatterSeed,
                                                 int pathIndex, float speedMultiplier) {
        List<EnemyMob> enemies = new ArrayList<>();
        int delay = 0;
        for (WaveSlot slot : content.spawnSequence()) {
            enemies.addAll(spawnSlot(slot, gameWorld, delay, content.delayTicksPerSlot(), scatterSeed, pathIndex,
                    speedMultiplier));
            delay++;
        }
        return List.copyOf(enemies);
    }

    private static List<EnemyMob> spawnSlot(WaveSlot slot, GameWorld gameWorld, int delay, float delayTicksPerSlot,
                                              long scatterSeed, int pathIndex, float speedMultiplier) {
        return switch (slot) {
            case EnemySlot s -> spawnShaped(s, gameWorld, delay, delayTicksPerSlot, scatterSeed, pathIndex,
                    speedMultiplier);
            case EmptySlot ignored -> List.of();
        };
    }

    /**
     * Builds every member of one shaped slot: health and bounty scaled and split here, size and
     * speed passed on to the mob, and the shape's trait applied to the definition first. Members
     * are spaced by {@link SpawnShape#delaySpacingSlots()}.
     * <p>
     * Formation offsets draw from a random source seeded by the wave and slot, not the world's,
     * which tower targeting also uses - otherwise a formation would depend on firing order.
     */
    private static List<EnemyMob> spawnShaped(EnemySlot enemySlot, GameWorld gameWorld, int delay,
                                                float delayTicksPerSlot, long scatterSeed, int pathIndex,
                                                float speedMultiplier) {
        EnemyDefinition definition = enemySlot.shape().traitOverride()
                .map(trait -> enemySlot.definition().withAdditionalTraits(List.of(trait)))
                .orElse(enemySlot.definition());
        Rank rank = enemySlot.rank();
        SpawnShape shape = enemySlot.shape();
        int health = Math.max(1, Math.round(definition.baseHealth() * shape.healthMultiplier()));
        int[] bountyShares = shape.bountyShares(definition.price());
        double maxRadius = gameWorld.getBoard().scale() * PathCoverage.PATH_WIDTH_CELLS / 2.0 * PATH_WIDTH_MARGIN_FRACTION;
        RandomSource scatter = RandomSource.seeded(scatterSeed * 31 + delay);
        List<EnemyMob> members = new ArrayList<>(shape.members());
        for (int i = 0; i < shape.members(); i++) {
            double slotPosition = delay + i * shape.delaySpacingSlots();
            Vec2 localOffset = shape.spread().offsetFor(i, shape.members(), maxRadius, scatter);
            SpawnParameters spawnParameters = SpawnParameters.of(slotPosition, delayTicksPerSlot,
                    definition.baseSpeed(), health, bountyShares[i], shape.sizeMultiplier(),
                    shape.speedMultiplier() * speedMultiplier, localOffset, pathIndex);
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

}
