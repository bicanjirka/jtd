package td.tower.pulse;

import td.term.TermText;
import td.tower.TowerTerm;

/** Deep Shadow: the shadow's hit is Shadow damage. */
public final class DeepShadowMechanic implements PulseMechanic {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSundial(spec.sundial().asShadow());
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(TowerTerm.SUNDIAL).plus("'s hit is ").plus(TowerTerm.SHADOW).plus(" damage");
    }
}
