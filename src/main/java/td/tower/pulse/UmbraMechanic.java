package td.tower.pulse;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Umbra: the shadow bites harder inside the field. */
public final class UmbraMechanic implements PulseMechanic {

    private static final float IN_FIELD_FACTOR = 2f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSundial(spec.sundial().withInFieldFactor(IN_FIELD_FACTOR)).withLook(StoneLook.UMBRA);
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(TowerTerm.SUNDIAL)
                .plus("'s hit deals " + TermNumbers.times(IN_FIELD_FACTOR) + " damage to an enemy in the field");
    }
}
