package td.wave;

import td.enemy.FlatResistTrait;
import td.enemy.IdentifiedTrait;
import td.util.GameStartupException;

import java.util.Optional;

/**
 * How one wave slot spawns - a value composed from independent mechanisms rather than one
 * implementation per shape: a per-mob multiplier set (size/speed/health/bounty), an optional
 * trait override, a {@link SpawnSpread} pattern, and inter-member delay spacing (in slot-widths,
 * added to a member's index before it is converted to ticks). {@link #normal()} is the identity:
 * one member, every multiplier 1, no trait override, no spread, no extra delay - today's spawn
 * behaviour exactly. See docs/features/FEATURE-enemy-spawn-types.md's "Three mechanisms" section
 * for why the formation shapes are built this way instead of as separate variants.
 * <p>
 * {@code traitOverride}, when present, is composed onto the spawned mob's definition via
 * {@code EnemyDefinition.withAdditionalTraits} - {@link #armored()} is the one shape that uses
 * this, attaching (or, on an already-armored enemy, replacing) a defensive trait it wouldn't
 * otherwise have. This is deliberately not a stat multiplier the way {@code boss}/{@code elite}
 * used to be: a spawn shape can modify what a spawned enemy <em>is</em>, the same identity-based
 * mechanism a rank ladder step uses to upgrade one trait without disturbing the rest - see
 * {@code td/wave/CLAUDE.md}.
 */
public record SpawnShape(int members, float sizeMultiplier, float speedMultiplier, float healthMultiplier,
                          float bountyMultiplier, Optional<IdentifiedTrait> traitOverride, SpawnSpread spread,
                          double delaySpacingSlots) {

    /**
     * One slot's member count is bounded so a typo like {@code swarm 300 c} fails to parse
     * instead of becoming a frame-rate bug discovered at runtime.
     */
    public static final int MAX_MEMBERS = 12;

    private static final double COLUMN_SPACING_SLOTS = 0.3;
    private static final double DRIP_SPACING_SLOTS = 2.0;
    // A placeholder demo value, like every other new-content number this feature introduces -
    // meaningful against an ordinary enemy's typical per-hit damage without trivializing it the
    // way the Warden's own, much larger, WARDEN_FLAT_RESIST is tuned for boss-scale hits.
    private static final int ARMORED_FLAT_RESIST = 30;
    // Named, not anonymous: an enemy that is armored twice (already carries its own "armor"
    // trait from a rank step) gets this trait *replacing* that one, not stacked alongside it -
    // the same identity mechanism a rank ladder step uses, applied from the spawn-shape side.
    private static final IdentifiedTrait ARMORED_TRAIT =
            IdentifiedTrait.named("armor", new FlatResistTrait(ARMORED_FLAT_RESIST));

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
     * Attaches a flat defensive trait to the spawned enemy - no size, speed, health or bounty
     * change survives from the old, removed Elite multiplier shape this replaces. Composed via
     * {@code EnemyDefinition.withAdditionalTraits} under the fixed id {@code "armor"}, so an
     * enemy that is already armored (its own rank ladder authored a trait under that same id)
     * gets this trait replacing that one, not a second instance stacked alongside it.
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
     * Splits {@code price} exactly across this shape's members: {@code round(price *
     * bountyMultiplier) / members} each, with the remainder handed to the first {@code
     * total % members} members, so the shares always sum to exactly the slot's total bounty and
     * no credit is lost to rounding. Which member carries the extra credit is fixed here, at
     * spawn - never recomputed at death, where kill order would otherwise change the payout.
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
