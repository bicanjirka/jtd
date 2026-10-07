package td.tower.splash;

/** Range III's Mines: every fourth blast leaves a mine for ten seconds, up to three at a time. */
public final class MinesPerk implements SplashPerk {

    private static final int EVERY = 4;
    private static final int MAX = 3;
    private static final int LIFETIME_TICKS = 200;
    private static final float RADIUS_CELLS = 0.5f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withMines(MineSpec.of(EVERY, MAX, LIFETIME_TICKS, RADIUS_CELLS));
    }
}
