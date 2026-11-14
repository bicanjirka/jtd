package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.AbilityUnits;

/** Feedback: a crit of the Arc gives back part of a Spark. */
public final class FeedbackMechanic implements SplashMechanic {

    private static final float REFUND = 0.5f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withCritRefund(AbilityUnits.of(REFUND)));
    }

    @Override
    public TermText describe() {
        return TermText.of("an ").plus(TowerTerm.ARC).plus(" crit gives half a ").plus(TowerTerm.SPARK);
    }
}
