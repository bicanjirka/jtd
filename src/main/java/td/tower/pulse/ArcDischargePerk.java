package td.tower.pulse;

/** Arc Discharge: once a second a zap hits the healthiest enemy in the field for fifteen field ticks' damage. */
public final class ArcDischargePerk implements PulsePerk {

    private static final float DAMAGE_FACTOR = 15f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(ZapSpec.of(DAMAGE_FACTOR));
    }
}
