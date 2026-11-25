package td.tower.splash;

import td.tower.ability.AbilityUnits;

/**
 * How a charged stone's Arc runs. Each {@code withX} is a copy, and every change holds whatever order the mechanics
 * were bought in: jumps and refunds add up, the damage, the crit chance and the crit multiplier only grow.
 *
 * @param jumps          how many times it jumps
 * @param points         each jump's magic damage, in points, before the hit's adjustments
 * @param revisits       whether it may jump to an enemy it already hit
 * @param critChance     the chance each jump crits
 * @param critMultiplier what a crit of a jump multiplies its damage by, at least; 0 for the tower's own
 * @param jolt   whether a jump leaves its enemy Vulnerable
 * @param critRefund     what a jump's crit gives back of the tower's currency
 */
public record ArcSpec(int jumps, float points, boolean revisits, float critChance, float critMultiplier,
                      boolean jolt, AbilityUnits critRefund) {

    public static final int JUMPS = 3;
    public static final float POINTS = 12f;
    /** How far a jump reaches from where it is, in cells. */
    public static final float REACH_CELLS = 1.5f;
    /** How long a jump with Jolt leaves its enemy Vulnerable, in seconds. */
    public static final float JOLT_SECONDS = 5f;

    /** The Arc as its node gives it. */
    public static ArcSpec bare() {
        return new ArcSpec(JUMPS, POINTS, false, 0f, 0f, false, AbilityUnits.none());
    }

    public ArcSpec withMoreJumps(int more) {
        return new ArcSpec(this.jumps + more, this.points, this.revisits, this.critChance, this.critMultiplier,
                this.jolt, this.critRefund);
    }

    public ArcSpec withPointsAtLeast(float atLeast) {
        return new ArcSpec(this.jumps, Math.max(this.points, atLeast), this.revisits, this.critChance,
                this.critMultiplier, this.jolt, this.critRefund);
    }

    public ArcSpec revisiting() {
        return new ArcSpec(this.jumps, this.points, true, this.critChance, this.critMultiplier, this.jolt,
                this.critRefund);
    }

    public ArcSpec withCritChanceAtLeast(float chance) {
        return new ArcSpec(this.jumps, this.points, this.revisits, Math.max(this.critChance, chance),
                this.critMultiplier, this.jolt, this.critRefund);
    }

    public ArcSpec withCritMultiplierAtLeast(float multiplier) {
        return new ArcSpec(this.jumps, this.points, this.revisits, this.critChance,
                Math.max(this.critMultiplier, multiplier), this.jolt, this.critRefund);
    }

    public ArcSpec jolting() {
        return new ArcSpec(this.jumps, this.points, this.revisits, this.critChance, this.critMultiplier, true,
                this.critRefund);
    }

    public ArcSpec withCritRefund(AbilityUnits refund) {
        return new ArcSpec(this.jumps, this.points, this.revisits, this.critChance, this.critMultiplier,
                this.jolt, this.critRefund.plus(refund));
    }
}
