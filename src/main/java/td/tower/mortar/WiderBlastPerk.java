package td.tower.mortar;

/** Siege Rounds II: the blast is a quarter wider. */
public final class WiderBlastPerk implements MortarPerk {

    private static final float FACTOR = 1.25f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBlastScaledBy(FACTOR);
    }
}
