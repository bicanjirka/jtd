package td.stat;

/**
 * What an enemy does to every tower within {@code radius} pixels of it: lowers fire rate and range
 * by the given fractions, added to the tower's other buffs.
 */
public record DisruptionAura(float radius, float fireRatePenalty, float rangePenalty) {

    public float radius2() {
        return this.radius * this.radius;
    }
}
