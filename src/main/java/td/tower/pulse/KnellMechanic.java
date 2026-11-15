package td.tower.pulse;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Knell: while anyone at full Toll is inside, the stone rings once a second and hits everyone in the field. */
public final class KnellMechanic implements PulseMechanic {

    private static final float DAMAGE_POINTS = 20f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withKnell(DAMAGE_POINTS);
    }

    @Override
    public TermText describe() {
        return TermText.of("while an enemy with " + PulseSpec.TOLL_CAP + " or more stacks of ").plus(EffectKind.TOLL)
                .plus(" is inside, the stone rings once a second: every enemy in the field takes "
                        + TermNumbers.whole(DAMAGE_POINTS) + " magic damage");
    }
}
