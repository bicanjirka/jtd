package td.enemy;

import td.util.GameStartupException;

import java.util.Optional;

/**
 * How one {@link SpawnEnemiesAction} spawns - a value composed the same way {@code td.wave.SpawnShape}
 * is (member count, per-member multipliers, an optional trait override), but deliberately not that
 * type: {@code SpawnShape} also carries a {@code SpawnSpread} and slot-relative delay spacing, both
 * defined against a wave slot's position on the path. An ability spawn has no slot - it places every
 * member at the caster's own live position ({@code DefinedEnemyMob.spawnAtSamePositionAs}), so a
 * path-relative formation offset has nothing to rotate against. {@link #normal()} is the identity -
 * one member, every multiplier {@code 1f}, no delay, no trait override - today's single-spawn
 * behaviour exactly.
 * <p>
 * {@code healthMultiplier}/{@code bountyMultiplier} are per-member, not a split the way
 * {@code SpawnShape.bountyShares} divides one authored slot price across its members - an ability
 * spawn creates independent reinforcements that each already carry their own definition's price.
 * <p>
 * {@code delaySpacingSlots} spaces members out in time rather than in space: member {@code i}'s
 * slot position is {@code i * delaySpacingSlots}, converted to a tick delay the same way a wave
 * slot's position is (see {@code SpawnParameters.atSlot}/{@code .of}), which is what keeps
 * multiple members from arriving stacked on the exact same pixel.
 */
public record AbilitySpawnShape(int members, float sizeMultiplier, float healthMultiplier,
                                 float bountyMultiplier, double delaySpacingSlots,
                                 Optional<IdentifiedTrait> traitOverride) {

    /**
     * Same typo-guard rationale as {@code SpawnShape.MAX_MEMBERS} - a large count authored by
     * accident fails to construct instead of becoming a frame-rate bug discovered at runtime.
     */
    public static final int MAX_MEMBERS = 12;

    private static final AbilitySpawnShape NORMAL =
            new AbilitySpawnShape(1, 1f, 1f, 1f, 0.0, Optional.empty());

    public AbilitySpawnShape {
        if (members < 1 || members > MAX_MEMBERS) {
            throw new GameStartupException(
                    "An ability spawn shape's member count must be between 1 and " + MAX_MEMBERS + ", was " + members);
        }
    }

    public static AbilitySpawnShape normal() {
        return NORMAL;
    }

    /**
     * {@code members} members, spaced {@code delaySpacingSlots} apart, every multiplier {@code
     * 1f} and no trait override - the narrow shape a caller that only wants temporal spacing
     * reaches for, rather than spelling out every component.
     */
    public AbilitySpawnShape(int members, double delaySpacingSlots) {
        this(members, 1f, 1f, 1f, delaySpacingSlots, Optional.empty());
    }

    /**
     * Attaches a trait to every member this shape spawns, composed onto the spawned mob's
     * definition via {@code EnemyDefinition.withAdditionalTraits} - the same identity-based
     * mechanism {@code SpawnShape.armored()} uses, reached here as a fluent copy rather than a
     * named factory since a caller, not this type, knows which trait it wants.
     */
    public AbilitySpawnShape withTraitOverride(IdentifiedTrait trait) {
        return new AbilitySpawnShape(this.members, this.sizeMultiplier, this.healthMultiplier,
                this.bountyMultiplier, this.delaySpacingSlots, Optional.of(trait));
    }
}
