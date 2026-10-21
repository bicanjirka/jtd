package td.tower.splash;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Overload: an arc crit Dazes its target, and arcs into a fully Saturated enemy crit more often. */
public final class OverloadPerk implements SplashPerk {

    private static final float DAZE_SECONDS = 0.5f;
    private static final float SATURATED_CRIT_BONUS = 0.1f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withDaze(DAZE_SECONDS).withSaturatedCritBonus(SATURATED_CRIT_BONUS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public TermText describe() {
        return TermText.of("an ").plus(TowerTerm.ARC).plus(" that crits applies ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_SECONDS) + "; an ").plus(TowerTerm.ARC)
                .plus(" into an enemy at full ").plus(EffectKind.SATURATED)
                .plus(" gets " + TermNumbers.signedPercent(SATURATED_CRIT_BONUS) + " crit chance");
    }
}
