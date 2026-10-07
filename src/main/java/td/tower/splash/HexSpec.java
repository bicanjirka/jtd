package td.tower.splash;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hexer's hexes and how hard they bite. Each {@code withX} is a copy.
 *
 * @param pool          every hex it owns, in the order bought: the order it takes them in
 * @param cursesPerCast how many enemies one cast curses
 * @param doomShare     the share of the damage taken under Doom it pays out
 */
public record HexSpec(List<Hex> pool, int cursesPerCast, float doomShare) {

    private static final float DOOM_SHARE = 0.3f;

    public HexSpec {
        pool = List.copyOf(pool);
    }

    /** No hexes: the Splash isn't a Hexer. */
    public static HexSpec none() {
        return new HexSpec(List.of(), 1, DOOM_SHARE);
    }

    public boolean isActive() {
        return !this.pool.isEmpty();
    }

    /** The pool with {@code hex} added after the others. */
    public HexSpec withHex(Hex hex) {
        List<Hex> grown = new ArrayList<>(this.pool);
        grown.add(hex);
        return new HexSpec(grown, this.cursesPerCast, this.doomShare);
    }

    public HexSpec withCursesPerCast(int cursesPerCast) {
        return new HexSpec(this.pool, Math.max(this.cursesPerCast, cursesPerCast), this.doomShare);
    }

    public HexSpec withDoomShareAtLeast(float doomShare) {
        return new HexSpec(this.pool, this.cursesPerCast, Math.max(this.doomShare, doomShare));
    }
}
