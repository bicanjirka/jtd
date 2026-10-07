package td.tower.pulse;

/** Event Horizon: each death inside adds a twentieth to the field's damage until the wave ends. */
public final class EventHorizonPerk implements PulsePerk {

    private static final float PER_DEATH = 0.05f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withField(spec.field().withPerDeathBonus(PER_DEATH));
    }
}
