package td.tower;


import td.util.ThreadConfined;

/**
 * A beam sweeping the full circle at a constant rate, and the test for whether a given bearing
 * was crossed during the last step - the "sonar scan" behind {@code SonarTower}.
 * <p>
 * The crossing test is an arc, not a point comparison, and that is the whole point of this
 * class: a beam advancing a fifth of a radian per tick is never <em>exactly</em> on an enemy
 * when the tick is sampled, so asking "is this enemy at the beam's angle" would miss almost
 * everything. {@link #sweptThisTick(double)} instead asks whether the bearing lies in the arc
 * the beam passed over since the previous tick, which catches every enemy the beam went by,
 * however fast the beam is turning or the enemy is moving.
 * <p>
 * Rotation is counterclockwise <em>on screen</em>, which means the angle decreases: the board's
 * y axis points down, so {@code Math.atan2}'s angle grows clockwise (see {@link TurretAim} for
 * the same convention).
 * <p>
 * Headless and clock-free like {@link TurretAim} and {@code td.TickAccumulator}:
 * {@link #advance()} moves exactly one tick's worth per call, so the scan speeds up and slows
 * down with the simulation rather than with wall-clock time.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class SonarSweep {

    private static final double TWO_PI = Math.PI * 2;

    private final double radiansPerTick;
    private double previousRadians = 0;
    private double currentRadians = 0;

    private SonarSweep(double radiansPerTick) {
        this.radiansPerTick = radiansPerTick;
    }

    /**
     * A sweep completing one revolution every {@code secondsPerRevolution} seconds of
     * simulation time, given how many ticks a second holds.
     */
    public static SonarSweep perRevolution(double secondsPerRevolution, double ticksPerSecond) {
        if (secondsPerRevolution <= 0) {
            throw new IllegalArgumentException("secondsPerRevolution must be positive: " + secondsPerRevolution);
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("ticksPerSecond must be positive: " + ticksPerSecond);
        }
        return new SonarSweep(TWO_PI / (secondsPerRevolution * ticksPerSecond));
    }

    /**
     * Wraps to {@code [0, 2PI)} - "how far counterclockwise from here to there".
     */
    private static double normalizeToCircle(double radians) {
        double wrapped = radians % TWO_PI;
        return wrapped < 0 ? wrapped + TWO_PI : wrapped;
    }

    /**
     * Wraps to {@code [-PI, PI)}, keeping the stored angle bounded over a long game.
     */
    private static double normalizeSigned(double radians) {
        double wrapped = radians % TWO_PI;
        if (wrapped < -Math.PI) {
            wrapped += TWO_PI;
        } else if (wrapped >= Math.PI) {
            wrapped -= TWO_PI;
        }
        return wrapped;
    }

    /**
     * Moves the beam one tick's worth counterclockwise.
     */
    public void advance() {
        this.previousRadians = this.currentRadians;
        this.currentRadians = normalizeSigned(this.currentRadians - this.radiansPerTick);
    }

    /**
     * Whether {@code bearing} lies in the arc the beam covered during the last {@link #advance()}.
     * The arc is half-open - it includes the angle the beam started the tick on and excludes the
     * one it ended on - so a stationary bearing is crossed exactly once per revolution rather
     * than twice at the boundary.
     */
    public boolean sweptThisTick(double bearing) {
        return normalizeToCircle(this.previousRadians - bearing) < this.radiansPerTick;
    }

    /**
     * The beam's heading for a render landing between two ticks - same {@code interpolationAlpha}
     * contract as {@link TurretAim#radiansAt(double)}. Measured forward from where the beam
     * started the tick, so it never has to blend across the wrap point.
     */
    public double radiansAt(double interpolationAlpha) {
        return this.previousRadians - this.radiansPerTick * interpolationAlpha;
    }

    public double radiansPerTick() {
        return this.radiansPerTick;
    }
}
