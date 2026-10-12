package td.tower.splash;

/**
 * What the Splash's blast does beyond its numbers, which are tower stats. Each {@code withX} is a copy.
 *
 * @param aimsAtCrowds whether it lands on the enemy with most neighbours inside the blast, not on a
 *                     random one
 * @param saturates    whether every blast that catches an enemy Saturates it
 */
public record BlastSpec(boolean aimsAtCrowds, boolean saturates) {

    /** A random target, no Saturation. */
    public static BlastSpec none() {
        return new BlastSpec(false, false);
    }

    public BlastSpec aimingAtCrowds() {
        return new BlastSpec(true, this.saturates);
    }

    public BlastSpec saturating() {
        return new BlastSpec(this.aimsAtCrowds, true);
    }
}
