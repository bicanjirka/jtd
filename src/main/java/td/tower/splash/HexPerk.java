package td.tower.splash;

/**
 * A node that adds a hex to the Hexer's pool, and may also make a cast curse more enemies at once.
 */
public final class HexPerk implements SplashPerk {

    private final Hex hex;
    private final int cursesPerCast;

    private HexPerk(Hex hex, int cursesPerCast) {
        this.hex = hex;
        this.cursesPerCast = cursesPerCast;
    }

    /** Adds {@code hex} to the pool. */
    public static HexPerk adding(Hex hex) {
        return new HexPerk(hex, 1);
    }

    /** This perk's hex, and a cast curses up to {@code cursesPerCast} enemies. */
    public HexPerk andCursing(int cursesPerCast) {
        return new HexPerk(this.hex, cursesPerCast);
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(this.hex).withCursesPerCast(this.cursesPerCast));
    }
}
