package td.tower.mortar;

/**
 * How many shells one firing loads together, how they are laid, and how much longer the reload after it takes.
 *
 * @param shells       shells loaded together, fired one after another
 * @param gapTicks     ticks between one shell of a volley and the next
 * @param reloadFactor how many times as long the reload after it is
 * @param spacingCells how far apart along the path, back from the target, each shell is laid; 0 for one target
 */
public record VolleySpec(int shells, int gapTicks, float reloadFactor, float spacingCells) {

    public static VolleySpec single() {
        return new VolleySpec(1, 0, 1f, 0f);
    }

    /** {@code shells} at one target, each re-picking its target if that one died. */
    public static VolleySpec atOneTarget(int shells, int gapTicks, float reloadFactor) {
        return new VolleySpec(shells, gapTicks, reloadFactor, 0f);
    }

    /** {@code shells} laid {@code spacingCells} apart along the path, from the target back. */
    public static VolleySpec alongThePath(int shells, int gapTicks, float reloadFactor, float spacingCells) {
        return new VolleySpec(shells, gapTicks, reloadFactor, spacingCells);
    }

    public boolean isVolley() {
        return this.shells > 1;
    }

    public boolean alongPath() {
        return this.spacingCells > 0f;
    }
}
