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

    private final BeamSpec spec;
    private final int revolutionTicks;
    private final int hitIntervalTicks;
    private double heading;
    private int revolutionTick;
    private int hitTick;
    private boolean hitting;
    private boolean completedRevolution;

    PhasedBeam(double heading, BeamSpec spec) {
        this.heading = heading;
        this.spec = spec;
        this.revolutionTicks = Math.max(1, Math.round(spec.secondsPerRevolution() * TickRate.TICKS_PER_SECOND));
        this.hitIntervalTicks = Math.max(1, this.revolutionTicks / HITS_PER_REVOLUTION);
    }

    @Override
    public void advance(OptionalDouble focusBearing) {
        focusBearing.ifPresent(bearing -> this.heading = bearing);
        this.hitTick++;
        this.hitting = this.hitTick >= this.hitIntervalTicks;
        if (this.hitting) {
            this.hitTick = 0;
        }
        this.revolutionTick++;
        this.completedRevolution = this.revolutionTick >= this.revolutionTicks;
        if (this.completedRevolution) {
            this.revolutionTick = 0;
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
