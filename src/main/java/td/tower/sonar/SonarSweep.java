package td.tower.sonar;


import td.util.ThreadConfined;

/**
 * A beam sweeping the full circle at a constant rate, and the test for whether it crossed a bearing
 * during the last step.
 * <p>
 * Hits are tested against the arc swept since the previous tick, not the beam's angle: a beam
 * moving a fifth of a radian per tick is almost never exactly on an enemy.
 * <p>
 * Rotation is counterclockwise on screen, so the angle decreases (the board's y axis points down).
 * {@link #advance()} moves one tick, so the scan follows simulation speed.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class SonarSweep {

    private static final double TWO_PI = Math.PI * 2;

    private final double radiansPerTick;
    private double previousRadians;
    private double currentRadians;
    /** How far round the current revolution the beam is, in {@code [0, 2PI)}. */
    private double swept;
    private boolean completedRevolution;

    private SonarSweep(double radiansPerTick, double previousRadians, double currentRadians, double swept) {
        this.radiansPerTick = radiansPerTick;
        this.previousRadians = previousRadians;
        this.currentRadians = currentRadians;
        this.swept = swept;
    }

    /** One revolution every {@code secondsPerRevolution} of simulation time. */
    public static SonarSweep perRevolution(double secondsPerRevolution, double ticksPerSecond) {
        if (secondsPerRevolution <= 0) {
            throw new IllegalArgumentException("secondsPerRevolution must be positive: " + secondsPerRevolution);
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("ticksPerSecond must be positive: " + ticksPerSecond);
        }
        return new SonarSweep(TWO_PI / (secondsPerRevolution * ticksPerSecond), 0, 0, 0);
    }

    /**
     * A sweep at another speed that carries on from where this one points, mid-revolution; its next
     * {@link #advance()} sweeps on from there.
     */
    public SonarSweep retimed(double secondsPerRevolution, double ticksPerSecond) {
        SonarSweep faster = perRevolution(secondsPerRevolution, ticksPerSecond);
        return new SonarSweep(faster.radiansPerTick, this.currentRadians, this.currentRadians, this.swept);
    }

    /** Wraps to {@code [0, 2PI)}. */
    private static double normalizeToCircle(double radians) {
        double wrapped = radians % TWO_PI;
        return wrapped < 0 ? wrapped + TWO_PI : wrapped;
    }

    /** Wraps to {@code [-PI, PI)}, keeping the stored angle bounded. */
    private static double normalizeSigned(double radians) {
        double wrapped = radians % TWO_PI;
        if (wrapped < -Math.PI) {
            wrapped += TWO_PI;
        } else if (wrapped >= Math.PI) {
            wrapped -= TWO_PI;
        }
        return wrapped;
    }

    public void advance() {
        this.previousRadians = this.currentRadians;
        this.currentRadians = normalizeSigned(this.currentRadians - this.radiansPerTick);
        this.swept += this.radiansPerTick;
        this.completedRevolution = this.swept >= TWO_PI - 1e-9;
        if (this.completedRevolution) {
            this.swept -= TWO_PI;
        }
    }

    /** Whether the last {@link #advance()} finished a full turn. */
    public boolean completedRevolution() {
        return this.completedRevolution;
    }

    /**
     * Whether {@code bearing} lies in the arc covered by the last {@link #advance()}. Half-open, so
     * a still bearing is crossed exactly once per revolution.
     */
    public boolean sweptThisTick(double bearing) {
        return normalizeToCircle(this.previousRadians - bearing) < this.radiansPerTick;
    }

    /**
     * The heading between two ticks, measured forward from the tick's start so it never blends
     * across the wrap point.
     */
    public double radiansAt(double interpolationAlpha) {
        return this.previousRadians - this.radiansPerTick * interpolationAlpha;
    }

    public double radiansPerTick() {
        return this.radiansPerTick;
    }
}
