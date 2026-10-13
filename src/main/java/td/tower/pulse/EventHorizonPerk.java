package td.tower.pulse;

import td.tower.upgrade.PerkText;

/** Event Horizon: each death inside adds a twentieth to the field's damage until the wave ends. */
public final class EventHorizonPerk implements PulsePerk {

    private static final float PER_DEATH = 0.05f;
    private static final float MAX = 1f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withField(spec.field().withPerDeathBonus(PER_DEATH, MAX));
    }

    @Override
    public String describe() {
        return "each death inside adds +" + PerkText.percent(PER_DEATH) + " field damage until the wave ends, up to +"
                + PerkText.percent(MAX);
    }
}
