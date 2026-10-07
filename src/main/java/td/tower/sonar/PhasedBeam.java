package td.tower.sonar;

import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.OptionalDouble;

/**
 * A beam that stops spinning and holds on one bearing, hitting what lies along it a few times every
 * revolution it used to take.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class PhasedBeam implements SonarBeam {

    /** How wide the beam is, as an angle either side of its heading. */
    private static final double HALF_WIDTH_RADIANS = 0.12;
    private static final int HITS_PER_REVOLUTION = 3;

    /** A tick count within this of a whole interval counts as it, which absorbs float error in a rate. */
    private static final double EPSILON = 1e-6;

    private final BeamSpec spec;
    private final double revolutionTicks;
    private final double hitIntervalTicks;
    private double heading;
    private double revolutionTick;
    private double hitTick;
    private boolean hitting;
    private boolean completedRevolution;

    PhasedBeam(double heading, BeamSpec spec) {
        this.heading = heading;
        this.spec = spec;
        this.revolutionTicks = Math.max(1.0, spec.secondsPerRevolution() * TickRate.TICKS_PER_SECOND);
        this.hitIntervalTicks = Math.max(1.0, this.revolutionTicks / HITS_PER_REVOLUTION);
    }

    /** The remainder of an interval carries over, so a fire rate that is no whole number of ticks stays exact. */
    @Override
    public void advance(OptionalDouble focusBearing) {
        focusBearing.ifPresent(bearing -> this.heading = bearing);
        this.hitTick++;
        this.hitting = this.hitTick >= this.hitIntervalTicks - EPSILON;
        if (this.hitting) {
            this.hitTick -= this.hitIntervalTicks;
        }
        this.revolutionTick++;
        this.completedRevolution = this.revolutionTick >= this.revolutionTicks - EPSILON;
        if (this.completedRevolution) {
            this.revolutionTick -= this.revolutionTicks;
        }
    }

    @Override
    public float strikeShare(double bearing) {
        return this.hitting && angularDistance(bearing, this.heading) <= HALF_WIDTH_RADIANS ? 1f : 0f;
    }

    private static double angularDistance(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
    }

    @Override
    public boolean completedRevolution() {
        return this.completedRevolution;
    }

    @Override
    public double headingAt(double interpolationAlpha) {
        return this.heading;
    }

    @Override
    public SonarBeam reshaped(BeamSpec next) {
        if (next.equals(this.spec)) {
            return this;
        }
        return next.phased() ? new PhasedBeam(this.heading, next) : SpinningBeam.of(next);
    }
}
