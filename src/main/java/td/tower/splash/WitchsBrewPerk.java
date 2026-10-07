package td.tower.splash;

/** Witch's Brew: adds Hex of Blight to the pool, and a cast curses up to three enemies. */
public final class WitchsBrewPerk implements SplashPerk {

    private static final int CURSES_PER_CAST = 3;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(new BlightHex()).withCursesPerCast(CURSES_PER_CAST));
    }
}
