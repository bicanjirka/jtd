package td.enemy;

import td.util.GameStartupException;

import java.util.Optional;

/**
 * How a {@link SpawnEnemiesAction} spawns: member count, per-member multipliers, time spacing and
 * an optional trait. Not the wave's spawn shape, because an ability spawn has no slot on the path
 * to lay a formation out against. {@link #normal()} is a single ordinary spawn.
 * <p>
 * Health and bounty multipliers apply per member rather than splitting a total. Members are spaced
 * in time by {@code delaySpacingSlots}, so they don't arrive stacked on one pixel.
 */
public record AbilitySpawnShape(int members, float sizeMultiplier, float healthMultiplier,
                                 float bountyMultiplier, double delaySpacingSlots,
                                 Optional<IdentifiedTrait> traitOverride) {

    /** Catches a mistyped count at construction. */
    public static final int MAX_MEMBERS = 12;

    // Visibly smaller than a normal reinforcement.
    private static final float BROOD_SIZE_MULTIPLIER = 0.7f;
    // Loose enough that members visibly trail one another.
    private static final double BROOD_DELAY_SPACING_SLOTS = 2.0;

    private static final AbilitySpawnShape NORMAL =
            new AbilitySpawnShape(1, 1f, 1f, 1f, 0.0, Optional.empty());

    public AbilitySpawnShape {
        if (members < 1 || members > MAX_MEMBERS) {
            throw new GameStartupException(
                    "An ability spawn shape's member count must be between 1 and " + MAX_MEMBERS + ", was " + members);
        }
    }

    /** Only time spacing; every multiplier {@code 1f}, no trait. */
    public AbilitySpawnShape(int members, double delaySpacingSlots) {
        this(members, 1f, 1f, 1f, delaySpacingSlots, Optional.empty());
    }

    public static AbilitySpawnShape normal() {
        return NORMAL;
    }

    /** A shrunken, time-spaced brood of reinforcements. */
    public static AbilitySpawnShape brood(int members, float healthMultiplier, float bountyMultiplier) {
        return new AbilitySpawnShape(members, BROOD_SIZE_MULTIPLIER, healthMultiplier, bountyMultiplier,
                BROOD_DELAY_SPACING_SLOTS, Optional.empty());
    }

    /** Adds a trait to every spawned member, replacing a trait with the same id. */
    public AbilitySpawnShape withTraitOverride(IdentifiedTrait trait) {
        return new AbilitySpawnShape(this.members, this.sizeMultiplier, this.healthMultiplier,
                this.bountyMultiplier, this.delaySpacingSlots, Optional.of(trait));
    }
}
