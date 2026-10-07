package td.tower.mortar;

/** Rifled Barrel: the shell flies 40% faster and is drawn smaller, with a streak. */
public final class RifledBarrelPerk implements MortarPerk {

    private static final float SPEED = 1.4f;
    private static final float SIZE = 0.7f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShellScaledBy(SPEED, SIZE);
    }
}
