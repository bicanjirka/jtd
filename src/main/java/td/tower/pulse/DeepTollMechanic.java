package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermText;

/** Deep Toll: this field builds Toll as deep as Toll goes. */
public final class DeepTollMechanic implements PulseMechanic {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withTollCap(PulseSpec.deepestToll());
    }

    @Override
    public TermText describe() {
        return TermText.of("this field builds ").plus(EffectKind.TOLL)
                .plus(" to " + PulseSpec.deepestToll() + ", from " + PulseSpec.TOLL_CAP);
    }
}
