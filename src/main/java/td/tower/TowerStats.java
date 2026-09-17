package td.tower;

import td.tower.buff.TowerBuff;

/**
 * A tower's live, buffed combat stats as one immutable value: what it currently hits for, how
 * far it currently reaches, how long it currently waits between shots, and how likely its next
 * hit is to land as a critical one.
 * <p>
 * These six numbers are <strong>correlated</strong> - {@code rangeReal} and
 * {@code rangeReal2} are two forms of the same reach, and a tower firing with this tick's
 * damage but last tick's cooldown is not a state the simulation ever produced. They are also
 * recomputed on the Event Dispatch Thread (an aura tower registering, an upgrade path being
 * bought) and read by tick code on the {@code game-loop} thread. Grouping them into one value
 * that is swapped through a single volatile field is what makes that crossing coherent:
 * marking six separate fields {@code volatile} would make each read fresh but would still
 * let a tick observe a half-applied recalculation. See CLAUDE.md 3.
 *
 * @param damage     current damage per hit, in hundredths (the same scale {@code damageBase} uses)
 * @param range      current range in cells, for display
 * @param coolDown   current ticks between shots, after any fire-rate bonus
 * @param rangeReal  current range in pixels
 * @param rangeReal2 {@code rangeReal} squared, so a per-tick scan never calls {@code Math.sqrt}
 * @param critChance current chance, in {@code [0, 1]}, that this tower's next hit rolls
 *                   critical - {@code 0} for every tower until an upgrade path grants some
 *                   (see {@code td.tower.buff.TowerBuff.critChanceBonus}), since no tower has
 *                   any innate crit chance of its own
 */
public record TowerStats(int damage, float range, int coolDown, float rangeReal, float rangeReal2, float critChance) {

    /**
     * Folds a tower's base stats and the total buff acting on it into one coherent set.
     * {@code scale} is the board's pixels-per-cell, which is what turns a range in cells into
     * the pixel range every distance check actually uses.
     */
    public static TowerStats of(int damageBase, float rangeBase, int coolDownMax, TowerBuff buff, int scale) {
        float range = buff.rangeFor(rangeBase);
        float rangeReal = range * scale;
        return new TowerStats(buff.damageFor(damageBase), range, buff.fireRateFor(coolDownMax),
                rangeReal, rangeReal * rangeReal, buff.critChanceFor(0f));
    }
}
