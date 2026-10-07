package td.tower;

import td.damage.AttackProfile;
import td.stat.DisruptionPenalty;
import td.tower.buff.TowerBuff;

/**
 * A tower's current stats as one immutable value. They are correlated and recomputed on the EDT
 * while tick code reads them, so they are swapped as a whole through one volatile field.
 *
 * @param damage     damage per hit, in hundredths
 * @param range      range in cells, for display
 * @param fireRate   how many times faster than its base cadence the tower runs; {@code 1} is the base
 * @param rangeReal  range in pixels
 * @param rangeReal2 {@code rangeReal} squared
 * @param attack     what every hit carries: crit chance and multiplier, penetration
 * @param disruption the enemy disruption these stats already include
 * @param timedBuffLength the factor on how long the tower's own timed buffs last
 * @param effectLength the factor on how long the effects it puts on enemies last
 * @param witherTicks ticks between the hits that apply Vulnerable; {@code 0} for none
 */
public record TowerStats(int damage, float range, double fireRate, float rangeReal, float rangeReal2,
                         AttackProfile attack, DisruptionPenalty disruption, float reachReal, float timedBuffLength,
                         float effectLength, int witherTicks) {

    /**
     * Folds base stats and the total buff into one set; {@code scale} converts range from cells to
     * pixels.
     */
    public static TowerStats of(TowerBaseStats base, TowerBuff buff, DisruptionPenalty disruption, int scale) {
        float left = buff.disruptionLeft();
        TowerBuff total = buff.combine(TowerBuff.fireRate(-disruption.fireRate() * left)
                .withRange(-disruption.range() * left));
        return of(base, total, scale, disruption, buff.rangeFor(base.range()) * scale);
    }

    /** @param reachReal range in pixels before disruption: how far a tower sees for XP */
    private static TowerStats of(TowerBaseStats base, TowerBuff buff, int scale, DisruptionPenalty disruption,
                                 float reachReal) {
        float range = buff.rangeFor(base.range());
        float rangeReal = range * scale;
        AttackProfile attack = AttackProfile.critChance(buff.critChanceFor(base.critChanceBase()))
                .withCritMultiplier(base.critMultiplier() + buff.critDamageBonus())
                .withArmorPenetration(buff.armorPenetrationBonus(), 0f)
                .withMagicPenetration(buff.magicPenetrationBonus(), 0f);
        return new TowerStats(buff.damageFor(base.damage()), range, buff.fireRateMultiplier(),
                rangeReal, rangeReal * rangeReal, attack, disruption, reachReal, buff.timedBuffLength(),
                buff.effectLength(), Math.round(buff.witherTicks()));
    }

    /** Chance in {@code [0, 1]} that a hit is critical, before the target's own stats. */
    public float critChance() {
        return this.attack.critChance();
    }
}
