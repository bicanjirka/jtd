package td.tower.pulse;


import td.tower.TurretAim;
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

    private SonarSweep(double radiansPerTick, double previousRadians, double currentRadians) {
        this.radiansPerTick = radiansPerTick;
        this.previousRadians = previousRadians;
        this.currentRadians = currentRadians;
    }

    /** One revolution every {@code secondsPerRevolution} of simulation time. */
    public static SonarSweep perRevolution(double secondsPerRevolution, double ticksPerSecond) {
        if (secondsPerRevolution <= 0) {
            throw new IllegalArgumentException("secondsPerRevolution must be positive: " + secondsPerRevolution);
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("ticksPerSecond must be positive: " + ticksPerSecond);
        }
        return new SonarSweep(TWO_PI / (secondsPerRevolution * ticksPerSecond), 0, 0);
    }

    /** This sweep pointing at {@code radians}, at the start of a revolution. */
    public SonarSweep startingAt(double radians) {
        return new SonarSweep(this.radiansPerTick, radians, radians);
    }

    /** Wraps to {@code [0, 2PI)}. */
    private static double normalizeToCircle(double radians) {
        double wrapped = radians % TWO_PI;
        return wrapped < 0 ? wrapped + TWO_PI : wrapped;
    }


    public void advance() {
        this.previousRadians = this.currentRadians;
        this.currentRadians = TurretAim.normalizeRadians(this.currentRadians - this.radiansPerTick);
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
