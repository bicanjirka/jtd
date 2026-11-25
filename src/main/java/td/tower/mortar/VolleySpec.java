package td.tower.mortar;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * How many shells one firing loads together, how they are laid, and how much longer the reload after it takes.
 *
 * @param shells       shells loaded together, fired one after another
 * @param gapTicks     ticks between one shell of a volley and the next
 * @param reloadFactor how many times as long the reload after it is
 * @param spacingCells how far apart along the path, centred on the target, each shell is laid; 0 for one target
 * @param normalsBetween whether a Normal shell, not taken from the magazine, flies between each two it loads
 */
public record VolleySpec(int shells, int gapTicks, float reloadFactor, float spacingCells, boolean normalsBetween) {

    public static VolleySpec single() {
        return new VolleySpec(1, 0, 1f, 0f, false);
    }

    /** {@code shells} at one target, each re-picking its target if that one died. */
    public static VolleySpec atOneTarget(int shells, int gapTicks, float reloadFactor) {
        return new VolleySpec(shells, gapTicks, reloadFactor, 0f, false);
    }

    /** {@code shells} laid {@code spacingCells} apart along the path, from the target back. */
    public static VolleySpec alongThePath(int shells, int gapTicks, float reloadFactor, float spacingCells) {
        return new VolleySpec(shells, gapTicks, reloadFactor, spacingCells, false);
    }

    /** The same volley with a Normal shell sneaked between each two it loads. */
    public VolleySpec withNormalsBetween() {
        return new VolleySpec(this.shells, this.gapTicks, this.reloadFactor, this.spacingCells, true);
    }

    /** The shells one firing sends: those it loads, and the Normals between them. */
    public int fired() {
        return this.normalsBetween ? 2 * this.shells - 1 : this.shells;
    }

    /**
     * The shells one firing sends, in order: {@code loaded}, a Normal between each two when it sneaks them in, each
     * sneaked one carrying the payload it flies right behind.
     */
    public List<LinedUpShell> sequence(List<HeldShell> loaded) {
        if (!this.normalsBetween) {
            return loaded.stream().map(LinedUpShell::held).toList();
        }
        return IntStream.range(0, 2 * loaded.size() - 1).mapToObj(i -> i % 2 == 0 ? LinedUpShell.held(loaded.get(i / 2))
                : LinedUpShell.sneaked(HeldShell.behind(ShellKind.NORMAL, Optional.of(loaded.get(i / 2).kind()))))
                .toList();
    }

    public boolean isVolley() {
        return this.shells > 1;
    }

    public boolean alongPath() {
        return this.spacingCells > 0f;
    }
}
