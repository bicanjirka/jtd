package td.tower.mortar;

/** Barrage: the reload takes twice as long, but every salvo is three shells half a second apart. */
public final class BarragePerk implements MortarPerk {

    private static final int SHELLS = 3;
    private static final int GAP_TICKS = 10;
    private static final float RELOAD_FACTOR = 2f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withSalvo(SalvoSpec.of(SHELLS, GAP_TICKS, RELOAD_FACTOR));
    }
}
