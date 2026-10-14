package td.tower.pulse;

import td.tower.upgrade.PerkText;

/** Event Horizon: each death inside widens the field by a tenth of its range until the wave ends. */
public final class EventHorizonPerk implements PulsePerk {

    private static final float PER_DEATH = 0.1f;
    private static final float MAX = 1f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withField(spec.field().withPerDeathGrowth(PER_DEATH, MAX));
    }

    @Override
    public String describe() {
        return "each death inside widens the field by " + PerkText.percent(PER_DEATH) + " of its range until the wave ends, up to "
                + PerkText.times(1f + MAX) + " its range";
    }
}
