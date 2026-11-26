package td.tower.mortar;

import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerStats;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/**
 * Shrapnel: a Heavy shell makes everything its blast reaches Bleed, a share a cell of what the blast dealt it where it
 * stood, its core or its edge.
 */
public final class ShrapnelMechanic implements MortarMechanic {

    /** What a Bleed costs a cell walked, a share of what the Heavy's blast deals where the enemy stood. */
    public static final float SHARE_PER_CELL = 0.18f;
    public static final int BLEED_TICKS = Math.round(4 * TickRate.TICKS_PER_SECOND);
    private static final String SHARE = TermNumbers.percent(SHARE_PER_CELL) + " of the damage its blast deals it";

    /** What a Bleed costs a cell walked, in hundredths, for an enemy a blast of {@code blastDamage} reached. */
    public static int bleedPerCell(float blastDamage) {
        return Math.round(blastDamage * SHARE_PER_CELL);
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withHeavy(HeavyRider.SHRAPNEL);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BLEEDING);
    }

    @Override
    public TermText describe() {
        return this.bleeds(SHARE + " a cell");
    }

    @Override
    public TermText describe(TowerStats stats) {
        int heavy = ShellKind.HEAVY.damage(stats.damage(), stats.value(TowerStat.NORMAL_DAMAGE),
                stats.value(TowerStat.HEAVY_DAMAGE));
        return this.bleeds(SHARE + " (about " + points(bleedPerCell(heavy)) + " at full damage, "
                + points(bleedPerCell(heavy * ShellKind.HEAVY.blast().edgeShare())) + " at its edge) a cell");
    }

    private TermText bleeds(String perCell) {
        return TermText.of("a ").plus(TowerTerm.HEAVY_SHELL).plus(" shell applies ").plus(EffectKind.BLEEDING)
                .plus(" to every enemy its blast reaches: " + perCell + ", for "
                        + TermNumbers.seconds(BLEED_TICKS / TickRate.TICKS_PER_SECOND));
    }

    private static String points(int hundredths) {
        return TermNumbers.number(DamageUnits.inPoints(hundredths));
    }
}
