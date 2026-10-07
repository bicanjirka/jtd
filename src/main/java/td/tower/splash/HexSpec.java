package td.tower.splash;

import td.effect.EffectKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Hexer's hexes and how hard they bite. Each {@code withX} is a copy, and every change holds
 * whatever order the perks were bought in.
 *
 * @param pool          every hex it owns, in the order bought: the order it takes them in
 * @param cursesPerCast how many enemies one cast curses
 * @param doomShare     the share of the damage taken under Doom it pays out
 * @param blightShare   Blight's poison each tick, a share of the Hexer's damage
 */
public record HexSpec(List<Hex> pool, int cursesPerCast, float doomShare, float blightShare) {

    /** How long a hex lasts unless it says otherwise. */
    public static final float HEX_SECONDS = 6f;
    private static final float DOOM_SHARE = 0.3f;
    private static final float BLIGHT_SHARE = 0.04f;

    public HexSpec {
        pool = List.copyOf(pool);
    }

    /** No hexes: the Splash isn't a Hexer. */
    public static HexSpec none() {
        return new HexSpec(List.of(), 1, DOOM_SHARE, BLIGHT_SHARE);
    }

    public boolean isActive() {
        return !this.pool.isEmpty();
    }

    /** The hex in the pool that marks an enemy with {@code kind}; empty when it owns none. */
    public Optional<Hex> hexOf(EffectKind kind) {
        return this.pool.stream().filter(hex -> hex.kind() == kind).findFirst();
    }

    /** The pool with {@code hex} added after the others. */
    public HexSpec withHex(Hex hex) {
        List<Hex> grown = new ArrayList<>(this.pool);
        grown.add(hex);
        return new HexSpec(grown, this.cursesPerCast, this.doomShare, this.blightShare);
    }

    public HexSpec withCursesPerCast(int cursesPerCast) {
        return new HexSpec(this.pool, Math.max(this.cursesPerCast, cursesPerCast), this.doomShare, this.blightShare);
    }

    public HexSpec withDoomShareAtLeast(float doomShare) {
        return new HexSpec(this.pool, this.cursesPerCast, Math.max(this.doomShare, doomShare), this.blightShare);
    }

    public HexSpec withBlightShareAtLeast(float blightShare) {
        return new HexSpec(this.pool, this.cursesPerCast, this.doomShare, Math.max(this.blightShare, blightShare));
    }
}
