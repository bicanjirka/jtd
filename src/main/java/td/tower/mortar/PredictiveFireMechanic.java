package td.tower.mortar;

import td.term.TermText;

/** Predictive Fire: the Mortar aims where the enemy will be when the shell lands. */
public final class PredictiveFireMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withLeadingTheTarget();
    }

    @Override
    public TermText describe() {
        return TermText.of("aims where the enemy will be when the shell lands");
    }
}
