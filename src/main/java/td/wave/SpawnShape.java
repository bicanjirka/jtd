package td.wave;

import td.util.GameStartupException;

/**
 * How one wave slot spawns - a value composed from independent mechanisms rather than one
 * implementation per shape: a per-mob multiplier set (size/speed/health/bounty/damage-taken), a
 * {@link SpawnSpread} pattern, and inter-member delay spacing (in slot-widths, added to a
 * member's index before it is converted to ticks). {@link #normal()} is the identity: one
 * member, every multiplier 1, no spread, no extra delay - today's spawn behaviour exactly. See
 * docs/features/FEATURE-enemy-spawn-types.md's "Three mechanisms" section for why eight shapes
 * are built this way instead of as eight variants.
 * <p>
 * {@code damageTakenMultiplier} is a permanent per-hit reduction, not the timed
 * {@code ShieldTemplate}/{@code EffectKind.SHIELD} effect - deliberately: an effect expires and
 * only an {@link td.enemy.EnemyDefinition}'s own abilities can apply one, but a spawn shape
 * needs to modify *any* definition, permanently, for as long as the mob lives. {@code Trait} is
 * this codebase's existing home for "permanent, always-on" (see {@code PercentResistTrait}); this
 * is that same idea attached to the shape instead of the definition, folded into
 * {@code DefinedEnemyMob.absorb} alongside whatever traits the definition already has.
 */
public record SpawnShape(int members, float sizeMultiplier, float speedMultiplier, float healthMultiplier,
                          float bountyMultiplier, float damageTakenMultiplier, SpawnSpread spread,
                          double delaySpacingSlots) {

    /**
     * One slot's member count is bounded so a typo like {@code swarm 300 c} fails to parse
     * instead of becoming a frame-rate bug discovered at runtime.
     */
    public static final int MAX_MEMBERS = 12;

    private static final double COLUMN_SPACING_SLOTS = 0.3;
    private static final double DRIP_SPACING_SLOTS = 2.0;

    private static final SpawnShape NORMAL = new SpawnShape(1, 1f, 1f, 1f, 1f, 1f, SpawnSpread.NONE, 0.0);
    private static final SpawnShape BOSS = new SpawnShape(1, 2.0f, 0.5f, 1f, 2.0f, 1f, SpawnSpread.NONE, 0.0);
    private static final SpawnShape ELITE = new SpawnShape(1, 1.5f, 1f, 2.0f, 1.5f, 0.5f, SpawnSpread.NONE, 0.0);

    public SpawnShape {
        if (members < 1 || members > MAX_MEMBERS) {
            throw new GameStartupException(
                    "A spawn shape's member count must be between 1 and " + MAX_MEMBERS + ", was " + members);
        }
    }

    public static SpawnShape normal() {
        return NORMAL;
    }

    public static SpawnShape boss() {
        return BOSS;
    }

    public static SpawnShape elite() {
        return ELITE;
    }

    public static SpawnShape swarm(int members) {
        return new SpawnShape(members, 0.5f, 1f, 1f / members, 1f, 1f, SpawnSpread.SCATTERED, 0.0);
    }

    public static SpawnShape line(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, 1f, SpawnSpread.EVEN, 0.0);
    }

    public static SpawnShape flank() {
        return new SpawnShape(2, 1f, 1f, 1f, 1f, 1f, SpawnSpread.EDGES, 0.0);
    }

    public static SpawnShape column(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, 1f, SpawnSpread.NONE, COLUMN_SPACING_SLOTS);
    }

    public static SpawnShape drip(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, 1f, SpawnSpread.NONE, DRIP_SPACING_SLOTS);
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
