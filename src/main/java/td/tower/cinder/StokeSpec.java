package td.tower.cinder;

/**
 * Stoke: a wave that reaches an enemy already burning from this Cinder raises its burn, one step more
 * each time, up to {@code maxSteps}. The steps go when the burn has ended and {@code graceTicks} more
 * have passed.
 *
 * @param active     whether the Cinder stokes at all
 * @param maxSteps   the most steps an enemy can carry
 * @param step       how much one step raises a burn, as a fraction
 * @param graceTicks how long an enemy keeps its steps after its burn has ended
 * @param alwaysFull whether an enemy carries every step from its first wave
 */
public record StokeSpec(boolean active, int maxSteps, float step, int graceTicks, boolean alwaysFull) {

    public static StokeSpec none() {
        return new StokeSpec(false, 0, 0f, 0, false);
    }

    public static StokeSpec of(int maxSteps, float step) {
        return new StokeSpec(true, maxSteps, step, 0, false);
    }

    public StokeSpec withStep(float step) {
        return new StokeSpec(this.active, this.maxSteps, step, this.graceTicks, this.alwaysFull);
    }

    public StokeSpec withGraceTicks(int graceTicks) {
        return new StokeSpec(this.active, this.maxSteps, this.step, graceTicks, this.alwaysFull);
    }

    public StokeSpec full() {
        return new StokeSpec(this.active, this.maxSteps, this.step, this.graceTicks, true);
    }
}
