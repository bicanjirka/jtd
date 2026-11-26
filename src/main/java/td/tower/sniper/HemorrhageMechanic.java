package td.tower.sniper;

import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStats;
import td.tower.mortar.ShrapnelMechanic;
import td.util.TickRate;

import java.util.Set;

/** Hemorrhage: a crit makes its target Bleed, a share of the crit's damage for every cell it walks. */
public final class HemorrhageMechanic implements SniperMechanic {

    /** What a Bleed costs a cell walked, a share of what the crit deals. */
    private static final float SHARE_PER_CELL = 0.10f;
    /** As long as every bleed lasts. */
    private static final int BLEED_TICKS = ShrapnelMechanic.BLEED_TICKS;
    private static final String SHARE = TermNumbers.percent(SHARE_PER_CELL) + " of the crit's damage";

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical() && !result.killed()) {
            actions.bleed(result.target(), SHARE_PER_CELL, BLEED_TICKS);
        }
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
        float points = DamageUnits.inPoints(Math.round(stats.critDamage() * SHARE_PER_CELL));
        return this.bleeds(SHARE + " (about " + TermNumbers.number(points) + ") a cell");
    }

    private TermText bleeds(String perCell) {
        return TermText.of("a crit makes its target ").plus(EffectKind.BLEEDING).plus(": " + perCell + " for "
                + TermNumbers.seconds(BLEED_TICKS / TickRate.TICKS_PER_SECOND));
    }
}
