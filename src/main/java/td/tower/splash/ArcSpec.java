package td.tower.splash;

import td.tower.ability.AbilityUnits;

/**
 * How a charged stone's Arc runs. Each {@code withX} is a copy, and every change holds whatever order the mechanics
 * were bought in: jumps and refunds add up, the share and the crit chance only grow.
 *
 * @param jumps        how many times it jumps
 * @param share        each jump's damage, a share of base damage
 * @param revisits     whether it may jump to an enemy it already hit
 * @param critChance   the chance each jump crits
 * @param shortCircuit whether the enemy's armor raises a jump's damage
 * @param critRefund   what a jump's crit gives back of the tower's currency
 */
public record ArcSpec(int jumps, float share, boolean revisits, float critChance, boolean shortCircuit,
                      AbilityUnits critRefund) {

    public static final int JUMPS = 3;
    public static final float SHARE = 0.5f;
    /** How far a jump reaches from where it is, in cells. */
    public static final float REACH_CELLS = 1.5f;

    /** The Arc as its node gives it. */
    public static ArcSpec bare() {
        return new ArcSpec(JUMPS, SHARE, false, 0f, false, AbilityUnits.none());
    }

    public ArcSpec withMoreJumps(int more) {
        return new ArcSpec(this.jumps + more, this.share, this.revisits, this.critChance, this.shortCircuit,
                this.critRefund);
    }

    public ArcSpec withShareAtLeast(float atLeast) {
        return new ArcSpec(this.jumps, Math.max(this.share, atLeast), this.revisits, this.critChance,
                this.shortCircuit, this.critRefund);
    }

    public ArcSpec revisiting() {
        return new ArcSpec(this.jumps, this.share, true, this.critChance, this.shortCircuit, this.critRefund);
    }

    public ArcSpec withCritChanceAtLeast(float chance) {
        return new ArcSpec(this.jumps, this.share, this.revisits, Math.max(this.critChance, chance),
                this.shortCircuit, this.critRefund);
    }

    public ArcSpec shortCircuiting() {
        return new ArcSpec(this.jumps, this.share, this.revisits, this.critChance, true, this.critRefund);
    }

    public ArcSpec withCritRefund(AbilityUnits refund) {
        return new ArcSpec(this.jumps, this.share, this.revisits, this.critChance, this.shortCircuit,
                this.critRefund.plus(refund));
    }

    /**
     * How much harder a jump hits an enemy that cuts {@code physicalReduction} from a physical hit: by that share with
     * Short Circuit, so never double; not at all without it.
     */
    public float factorAgainst(float physicalReduction) {
        return this.shortCircuit ? 1f + Math.max(0f, physicalReduction) : 1f;
    }
}
