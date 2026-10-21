package td.tower.splash;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/**
 * A node that adds a hex to the Hexer's pool, and may also make a cast curse more enemies at once.
 */
public final class HexMechanic implements SplashMechanic {

    private final Hex hex;
    private final int cursesPerCast;
    private final boolean founding;

    private HexMechanic(Hex hex, int cursesPerCast, boolean founding) {
        this.hex = hex;
        this.cursesPerCast = cursesPerCast;
        this.founding = founding;
    }

    /** Adds {@code hex} to the pool. */
    public static HexMechanic adding(Hex hex) {
        return new HexMechanic(hex, 1, false);
    }

    /** Starts casting, with {@code hex} the first in the pool. */
    public static HexMechanic founding(Hex hex) {
        return new HexMechanic(hex, 1, true);
    }

    /** This mechanic's hex, and a cast curses up to {@code cursesPerCast} enemies. */
    public HexMechanic andCursing(int cursesPerCast) {
        return new HexMechanic(this.hex, cursesPerCast, this.founding);
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(this.hex).withCursesPerCast(this.cursesPerCast));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(this.hex.kind());
    }

    @Override
    public TermText describe() {
        TermText cast = this.founding
                ? TermText.of("every " + TermNumbers.ordinal(HexSpec.CAST_EVERY) + " shot casts a ").plus(TowerTerm.HEX)
                        .plus(" instead of a blast; ")
                : TermText.empty();
        TermText reach = this.cursesPerCast > 1
                ? TermText.of("; a cast reaches up to " + this.cursesPerCast + " enemies") : TermText.empty();
        return cast.plus("adds ").plus(this.hex.describe()).plus(" to the hexes it casts").plus(reach);
    }
}
