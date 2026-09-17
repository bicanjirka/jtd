package td.tower;


import td.util.ThreadConfined;
/**
 * Chases a desired heading at a capped angular speed, always turning the shorter way around the
 * circle - lets an aiming tower's turret head visibly sweep toward a target instead of snapping
 * to face it instantly. Headless and clock-free, like the rest of this codebase's simulation
 * state (see {@link td.TickAccumulator} for the same shape of small, independently-tested
 * helper): {@link #tick(double)} advances exactly one step per call, with no wall-clock or Swing
 * dependency. {@code td.ui} interpolates between two ticks' headings for a smooth 60fps render
 * the same way {@code EnemyFrameBuilder} does for enemy position - see {@link #radiansAt(double)}.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)  // owned by the tower that turns it, so game-loop in practice
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
     * The angle from {@code (fromX, fromY)} to {@code (toX, toY)}, in the convention
     * {@code Graphics2D.rotate(double)} itself uses: {@code 0} points along {@code +X},
     * increasing toward {@code +Y}.
     */
    public static double angleTo(double fromX, double fromY, double toX, double toY) {
        return Math.atan2(toY - fromY, toX - fromX);
    }

    /**
     * Advances the heading one tick's worth toward {@code desiredRadians}, turning at most
     * {@code maxTurnRadiansPerTick}, the shorter way around the circle.
     */
    public void tick(double desiredRadians) {
        this.previousRadians = this.currentRadians;
        double step = clamp(normalizeRadians(desiredRadians - this.currentRadians), this.maxTurnRadiansPerTick);
        this.currentRadians = this.currentRadians + step;
    }

    /**
     * This turret's heading interpolated between the last two ticks, for a render landing
     * between them - same {@code interpolationAlpha} contract as {@code EnemyFrameBuilder}'s
     * position lerp, just for an angle (shortest way around, not a plain linear blend).
     */
    public double radiansAt(double interpolationAlpha) {
        double diff = normalizeRadians(this.currentRadians - this.previousRadians);
        return this.previousRadians + diff * interpolationAlpha;
    }

    /**
     * This turret's current heading, as of the last {@link #tick(double)} call - not wrapped
     * to any particular range, since it accumulates every turn ever taken rather than resetting
     * each revolution. A cone tower tests wedge membership against this (via
     * {@link #normalizeRadians(double)}), the same heading {@link #radiansAt} renders at
     * {@code interpolationAlpha == 1.0}.
     */
    public double currentRadians() {
        return this.currentRadians;
    }

    private static double clamp(double value, double bound) {
        return Math.max(-bound, Math.min(bound, value));
    }

    /** Wraps to {@code [-PI, PI)} - the shortest-path representation of an angular difference. */
    public static double normalizeRadians(double radians) {
        double wrapped = radians % TWO_PI;
        if (wrapped < -Math.PI) {
            wrapped += TWO_PI;
        } else if (wrapped >= Math.PI) {
            wrapped -= TWO_PI;
        }
        return wrapped;
    }
}
