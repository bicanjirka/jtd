package td.tower.sonar;

import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.OptionalDouble;

/** A beam that sweeps the full circle counterclockwise, with an optional second beam opposite. */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class SpinningBeam implements SonarBeam {

    private final SonarSweep sweep;
    private final BeamSpec spec;

    private SpinningBeam(SonarSweep sweep, BeamSpec spec) {
        this.sweep = sweep;
        this.spec = spec;
    }

    public static SpinningBeam of(BeamSpec spec) {
        return new SpinningBeam(SonarSweep.perRevolution(spec.secondsPerRevolution(), TickRate.TICKS_PER_SECOND), spec);
    }

    @Override
    public void advance(OptionalDouble focusBearing) {
        this.sweep.advance();
    }

    @Override
    public float strikeShare(double bearing) {
        if (this.sweep.sweptThisTick(bearing)) {
            return 1f;
        }
        return this.spec.hasTwinBeam() && this.sweep.sweptThisTick(bearing + Math.PI) ? this.spec.twinShare() : 0f;
    }

    @Override
    public boolean completedRevolution() {
        return this.sweep.completedRevolution();
    }

    @Override
    public double headingAt(double interpolationAlpha) {
        return this.sweep.radiansAt(interpolationAlpha);
    }

    @Override
    public SonarBeam reshaped(BeamSpec next) {
        if (next.equals(this.spec)) {
            return this;
        }
        if (next.phased()) {
            return new PhasedBeam(this.sweep.radiansAt(1d), next);
        }
        return new SpinningBeam(this.sweep.retimed(next.secondsPerRevolution(), TickRate.TICKS_PER_SECOND), next);
    }
}
