package td.tower.mortar;

/**
 * How many shells one firing sends, how far apart, and how much longer the reload after it takes.
 *
 * @param shells       shells a salvo sends, the first at once
 * @param gapTicks     ticks between one shell of a salvo and the next
 * @param reloadFactor how many times as long the reload is
 */
public record SalvoSpec(int shells, int gapTicks, float reloadFactor) {

    public static SalvoSpec single() {
        return new SalvoSpec(1, 0, 1f);
    }

    public static SalvoSpec of(int shells, int gapTicks, float reloadFactor) {
        return new SalvoSpec(shells, gapTicks, reloadFactor);
    }

    public boolean isSalvo() {
        return this.shells > 1;
    }
}
