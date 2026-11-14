package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;

/** Twin Hex: each hex curses more enemies. */
public final class TwinHexMechanic implements SplashMechanic {

    private static final int CURSES = 2;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withCursesPerCastAtLeast(CURSES));
    }

    @Override
    public TermText describe() {
        return TermText.of("each ").plus(TowerTerm.HEX).plus(" curses " + CURSES + " enemies");
    }
}
