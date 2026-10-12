package td.tower.cinder;

/**
 * Stoke: a wave that reaches an enemy already burning from this Cinder raises its burn, one step more
 * each time, up to {@code maxSteps}. How much a step raises it, and how long an enemy keeps its steps
 * after its burn has ended, are tower stats.
 *
 * @param active     whether the Cinder stokes at all
 * @param maxSteps   the most steps an enemy can carry
 * @param alwaysFull whether an enemy carries every step from its first wave
 */
public record StokeSpec(boolean active, int maxSteps, boolean alwaysFull) {

    public static StokeSpec none() {
        return new StokeSpec(false, 0, false);
    }

    public static StokeSpec of(int maxSteps) {
        return new StokeSpec(true, maxSteps, false);
    }

    public StokeSpec full() {
        return new StokeSpec(this.active, this.maxSteps, true);
    }
}
