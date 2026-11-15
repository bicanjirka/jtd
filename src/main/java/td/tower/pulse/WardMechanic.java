package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermText;

/** Ward: an enemy in the field is Silenced while inside. */
public final class WardMechanic implements PulseMechanic {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSilence();
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy in the field has ").plus(EffectKind.SILENCED).plus(" while inside");
    }
}
