package td.tower.splash;

/**
 * What the Splash's blast is like. Each {@code withX} is a copy.
 *
 * @param radiusBonus  extra blast radius, a fraction of the base; every distance the payload uses
 *                     grows with it too
 * @param edgeFloor    the share of damage the blast's edge still deals
 * @param aimsAtCrowds whether it lands on the enemy with most neighbours inside the blast, not on a
 *                     random one
 * @param saturation   how it builds Saturation
 */
public record BlastSpec(float radiusBonus, float edgeFloor, boolean aimsAtCrowds, SaturationRule saturation) {

    /** A random target, no radius bonus, falloff to nothing at the edge, no Saturation. */
    public static BlastSpec base() {
        return new BlastSpec(0f, 0f, false, SaturationRule.none());
    }

    /** {@code bonus} more radius on top of what it has. */
    public BlastSpec withRadiusBonus(float bonus) {
        return new BlastSpec(this.radiusBonus + bonus, this.edgeFloor, this.aimsAtCrowds, this.saturation);
    }

    public BlastSpec withEdgeFloor(float edgeFloor) {
        return new BlastSpec(this.radiusBonus, Math.max(this.edgeFloor, edgeFloor), this.aimsAtCrowds, this.saturation);
    }

    public BlastSpec aimingAtCrowds() {
        return new BlastSpec(this.radiusBonus, this.edgeFloor, true, this.saturation);
    }

    public BlastSpec withSaturation(SaturationRule saturation) {
        return new BlastSpec(this.radiusBonus, this.edgeFloor, this.aimsAtCrowds, saturation);
    }

    /** How much further every distance the payload uses reaches: the blast radius bonus. */
    public float distanceScale() {
        return 1f + this.radiusBonus;
    }

    /**
     * The share of the blast's damage an enemy {@code distanceShare} of the radius from its centre
     * takes: {@code 1 - share²}, never under the edge floor.
     */
    public float damageShareAt(float distanceShare) {
        float share = Math.min(1f, distanceShare);
        return Math.max(this.edgeFloor, 1f - share * share);
    }
}
