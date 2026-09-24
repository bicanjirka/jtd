package td.tower;


import td.util.ThreadConfined;

/**
 * Turns toward a desired heading at a capped angular speed, always the shorter way, so a turret
 * visibly sweeps instead of snapping. {@link #tick(double)} advances one step;
 * {@link #radiansAt(double)} interpolates for rendering.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class TurretAim {

    private static final double TWO_PI = Math.PI * 2;

    private final double maxTurnRadiansPerTick;
    private double previousRadians = 0;
    private double currentRadians = 0;

    public TurretAim(double maxTurnRadiansPerTick) {
        if (maxTurnRadiansPerTick <= 0) {
            throw new IllegalArgumentException("maxTurnRadiansPerTick must be positive: " + maxTurnRadiansPerTick);
        }
        this.maxTurnRadiansPerTick = maxTurnRadiansPerTick;
    }

    /**
     * The angle from one point to another, in {@code Graphics2D.rotate} convention: {@code 0} along
     * {@code +X}, increasing toward {@code +Y}.
     */
    public static double angleTo(double fromX, double fromY, double toX, double toY) {
        return Math.atan2(toY - fromY, toX - fromX);
    }

    private static double clamp(double value, double bound) {
        return Math.max(-bound, Math.min(bound, value));
    }

    /** Wraps to {@code [-PI, PI)}. */
    public static double normalizeRadians(double radians) {
        double wrapped = radians % TWO_PI;
        if (wrapped < -Math.PI) {
            wrapped += TWO_PI;
        } else if (wrapped >= Math.PI) {
            wrapped -= TWO_PI;
        }
        return wrapped;
    }

    /**
     * Turns at most {@code maxTurnRadiansPerTick} toward {@code desiredRadians}, the shorter way.
     */
    public void tick(double desiredRadians) {
        this.previousRadians = this.currentRadians;
        double step = clamp(normalizeRadians(desiredRadians - this.currentRadians), this.maxTurnRadiansPerTick);
        this.currentRadians = this.currentRadians + step;
    }

    /** The heading between the last two ticks, blended the shorter way around. */
    public double radiansAt(double interpolationAlpha) {
        double diff = normalizeRadians(this.currentRadians - this.previousRadians);
        return this.previousRadians + diff * interpolationAlpha;
    }

    /** The heading after the last tick, unwrapped: it accumulates every turn taken. */
    public double currentRadians() {
        return this.currentRadians;
    }
}
