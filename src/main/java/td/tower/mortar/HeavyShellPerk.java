package td.tower.mortar;

/** Heavy Shell: a bigger, slower shell, and what it lands within half a cell of is Dazed. */
public final class HeavyShellPerk implements MortarPerk {

    private static final float SPEED = 0.75f;
    private static final float SIZE = 1.3f;
    private static final int DAZE_TICKS = 10;
    private static final float DAZE_RADIUS_CELLS = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShellScaledBy(SPEED, SIZE).withDaze(DazeSpec.of(DAZE_TICKS, DAZE_RADIUS_CELLS));
    }
}
