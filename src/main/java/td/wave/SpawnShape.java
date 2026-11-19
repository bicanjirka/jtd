package td.wave;

import td.util.GameStartupException;

/**
 * How one wave slot spawns, composed from independent parts: per-mob multipliers, a
 * {@link SpawnSpread} formation, and delay spacing between members. {@link #normal()} is one plain
 * mob. A shape changes how a slot spawns, never what the enemy is: that is an affix.
 */
public record SpawnShape(int members, float sizeMultiplier, float speedMultiplier, float healthMultiplier,
                          float bountyMultiplier, SpawnSpread spread,
                          double delaySpacingSlots) {

    /** Catches a typo like {@code swarm 300 walker} at parse time. */
    public static final int MAX_MEMBERS = 12;

    private static final double COLUMN_SPACING_SLOTS = 0.3;
    private static final double DRIP_SPACING_SLOTS = 2.0;
    private static final float SWARM_HEALTH_EASE = 3f;
    private static final float SWARM_SMALLEST_SIZE = 0.5f;

    private static final SpawnShape NORMAL =
            new SpawnShape(1, 1f, 1f, 1f, 1f, SpawnSpread.NONE, 0.0);

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
     * A member's health falls slower than the swarm grows, so a bigger swarm is more health in all, and its body
     * shrinks with the root of its health: the area a player sees is the health there is to remove.
     */
    public static SpawnShape swarm(int members) {
        float health = (1f + SWARM_HEALTH_EASE) / (members + SWARM_HEALTH_EASE);
        float size = Math.max(SWARM_SMALLEST_SIZE, (float) Math.sqrt(health));
        return new SpawnShape(members, size, 1f, health, 1f, SpawnSpread.SCATTERED, 0.0);
    }

    public static SpawnShape line(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, SpawnSpread.EVEN, 0.0);
    }

    public static SpawnShape flank() {
        return new SpawnShape(2, 1f, 1f, 1f, 1f, SpawnSpread.EDGES, 0.0);
    }

    public static SpawnShape column(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, SpawnSpread.NONE, COLUMN_SPACING_SLOTS);
    }

    public static SpawnShape drip(int members) {
        return new SpawnShape(members, 1f, 1f, 1f, 1f, SpawnSpread.NONE, DRIP_SPACING_SLOTS);
    }

    /**
     * The slot pays for the health it sends: a member at full health is worth a full bounty, and a member sent
     * with a share of it is worth that share, the remainder going to the first members so the shares sum
     * exactly. Fixed at spawn, so kill order never changes the payout.
     */
    public int[] bountyShares(float bounty) {
        int total = Math.round(bounty * this.bountyMultiplier * this.members * this.healthMultiplier);
        int base = total / this.members;
        int remainder = total % this.members;
        int[] shares = new int[this.members];
        for (int i = 0; i < this.members; i++) {
            shares[i] = base + (i < remainder ? 1 : 0);
        }
        return shares;
    }
}
