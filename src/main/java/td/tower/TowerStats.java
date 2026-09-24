package td.tower;

import td.tower.buff.TowerBuff;

/**
 * A tower's current stats as one immutable value. They are correlated and recomputed on the EDT
 * while tick code reads them, so they are swapped as a whole through one volatile field.
 *
 * @param damage     damage per hit, in hundredths
 * @param range      range in cells, for display
 * @param coolDown   ticks between shots
 * @param rangeReal  range in pixels
 * @param rangeReal2 {@code rangeReal} squared
 * @param critChance chance in {@code [0, 1]} that a hit is critical
 */
public record TowerStats(int damage, float range, int coolDown, float rangeReal, float rangeReal2, float critChance) {

    /**
     * Folds base stats and the total buff into one set; {@code scale} converts range from cells to
     * pixels.
     */
    public static TowerStats of(int damageBase, float rangeBase, int coolDownMax, float critChanceBase,
                                TowerBuff buff, int scale) {
        float range = buff.rangeFor(rangeBase);
        float rangeReal = range * scale;
        return new TowerStats(buff.damageFor(damageBase), range, buff.fireRateFor(coolDownMax),
                rangeReal, rangeReal * rangeReal, buff.critChanceFor(critChanceBase));
    }
}
