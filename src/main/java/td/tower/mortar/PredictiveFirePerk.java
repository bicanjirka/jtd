package td.tower.mortar;

/** Predictive Fire: the Mortar aims where the enemy will be when the shell lands. */
public final class PredictiveFirePerk implements MortarPerk {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withLeadingTheTarget();
    }
}
