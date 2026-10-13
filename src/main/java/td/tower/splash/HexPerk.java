package td.tower.splash;

import td.tower.upgrade.PerkText;

/**
 * A node that adds a hex to the Hexer's pool, and may also make a cast curse more enemies at once.
 */
public final class HexPerk implements SplashPerk {

    private final Hex hex;
    private final int cursesPerCast;
    private final boolean founding;

    private HexPerk(Hex hex, int cursesPerCast, boolean founding) {
        this.hex = hex;
        this.cursesPerCast = cursesPerCast;
        this.founding = founding;
    }

    /** Adds {@code hex} to the pool. */
    public static HexPerk adding(Hex hex) {
        return new HexPerk(hex, 1, false);
    }

    /** Starts casting, with {@code hex} the first in the pool. */
    public static HexPerk founding(Hex hex) {
        return new HexPerk(hex, 1, true);
    }

    /** This perk's hex, and a cast curses up to {@code cursesPerCast} enemies. */
    public HexPerk andCursing(int cursesPerCast) {
        return new HexPerk(this.hex, cursesPerCast, this.founding);
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(this.hex).withCursesPerCast(this.cursesPerCast));
    }

    @Override
    public String describe() {
        return (this.founding ? "every " + PerkText.ordinal(HexSpec.CAST_EVERY) + " shot casts a hex instead of blasting; " : "")
                + "adds " + this.hex.describe() + (this.cursesPerCast > 1 ? "; a cast curses up to " + this.cursesPerCast + " enemies" : "");
    }
}
