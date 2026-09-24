package td.wave;

import td.enemy.FlatResistTrait;
import td.enemy.IdentifiedTrait;
import td.util.GameStartupException;

import java.util.Optional;

/**
 * How one wave slot spawns, composed from independent parts: per-mob multipliers, an optional
 * trait, a {@link SpawnSpread} formation, and delay spacing between members. {@link #normal()} is
 * one plain mob.
 * <p>
 * {@code traitOverride} is added to the spawned definition by id, so a shape can change what an
 * enemy is, not just scale it.
 */
public record SpawnShape(int members, float sizeMultiplier, float speedMultiplier, float healthMultiplier,
                          float bountyMultiplier, Optional<IdentifiedTrait> traitOverride, SpawnSpread spread,
                          double delaySpacingSlots) {

    /** Catches a typo like {@code swarm 300 c} at parse time. */
    public static final int MAX_MEMBERS = 12;

    private static final double COLUMN_SPACING_SLOTS = 0.3;
    private static final double DRIP_SPACING_SLOTS = 2.0;
    private static final int ARMORED_FLAT_RESIST = 30;
    // Named, so it replaces an existing armor trait rather than stacking with it.
    private static final IdentifiedTrait ARMORED_TRAIT =
            IdentifiedTrait.named("armor", FlatResistTrait.physicalOnly(ARMORED_FLAT_RESIST));

    private static final SpawnShape NORMAL =
            new SpawnShape(1, 1f, 1f, 1f, 1f, Optional.empty(), SpawnSpread.NONE, 0.0);
    private static final SpawnShape ARMORED =
            new SpawnShape(1, 1f, 1f, 1f, 1f, Optional.of(ARMORED_TRAIT), SpawnSpread.NONE, 0.0);

    public SpawnShape {
        if (members < 1 || members > MAX_MEMBERS) {
            throw new GameStartupException(
                    "A spawn shape's member count must be between 1 and " + MAX_MEMBERS + ", was " + members);
        }
    }

    public static SpawnShape normal() {
        return NORMAL;
    }

    /**
     * Adds a flat armor trait under the id {@code "armor"}, replacing an existing armor trait
     * rather than stacking.
     */
    public static SpawnShape armored() {
        return ARMORED;
    }

    public static SpawnShape swarm(int members) {
        return new SpawnShape(members, 0.5f, 1f, 1f / members, 1f, Optional.empty(), SpawnSpread.SCATTERED, 0.0);
    }

    public static SpawnShape line(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, Optional.empty(), SpawnSpread.EVEN, 0.0);
    }

    public static SpawnShape flank() {
        return new SpawnShape(2, 1f, 1f, 1f, 1f, Optional.empty(), SpawnSpread.EDGES, 0.0);
    }

    public static SpawnShape column(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, Optional.empty(), SpawnSpread.NONE, COLUMN_SPACING_SLOTS);
    }

    public static SpawnShape drip(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, Optional.empty(), SpawnSpread.NONE, DRIP_SPACING_SLOTS);
    }

    /**
     * Splits the slot's bounty across members so the shares sum exactly, the remainder going to the
     * first members. Fixed at spawn, so kill order never changes the payout.
     */
    public int[] bountyShares(int price) {
        int total = Math.round(price * this.bountyMultiplier);
        int base = total / this.members;
        int remainder = total % this.members;
        int[] shares = new int[this.members];
        for (int i = 0; i < this.members; i++) {
            shares[i] = base + (i < remainder ? 1 : 0);
        }
        return shares;
    }
}
