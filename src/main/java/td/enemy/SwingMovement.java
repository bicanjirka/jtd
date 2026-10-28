package td.enemy;

/**
 * Starts facing its normal way and swings out to {@code swingRadians} one side, back through the middle to the same
 * angle on the other side, and back to the middle, and repeats. Negative radians swing the other side first.
 *
 * @param swingRadians how far it turns to each side of its normal facing
 * @param swingTicks   the length of one leg, from the middle to an extreme (or back)
 * @param curve        how the speed of the outward legs is spread over their length; the inward ones mirror it
 */
public record SwingMovement(float swingRadians, int swingTicks, SwingCurve curve) implements MovementBehavior {

    private static final float DEFAULT_SWING_RADIANS = (float) Math.toRadians(40.0);
    private static final int DEFAULT_SWING_TICKS = 40;
    private static final SwingCurve DEFAULT_CURVE = SwingCurve.DECELERATING;

    public SwingMovement {
        if (swingTicks < 1) {
            throw new IllegalArgumentException("A swing needs at least one tick");
        }
    }

    /** A gentle swing that slows towards each extreme. */
    public SwingMovement() {
        this(DEFAULT_SWING_RADIANS, DEFAULT_SWING_TICKS);
    }

    public SwingMovement(float swingRadians, int swingTicks) {
        this(swingRadians, swingTicks, DEFAULT_CURVE);
    }

    /** The angle off its normal facing {@code ticks} after the swinging began. */
    public float angleAt(int ticks) {
        int phase = ticks % (4 * this.swingTicks);
        int leg = phase / this.swingTicks;
        int into = phase % this.swingTicks;
        int along = leg % 2 == 0 ? into : this.swingTicks - into;
        float side = leg < 2 ? 1f : -1f;
        return side * this.swingRadians * this.curve.angleFraction((float) along / this.swingTicks);
    }
}
