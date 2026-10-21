package td.tower.splash;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/**
 * Fire Control: the blast lands on the enemy with most neighbours inside it, and every blast that
 * catches an enemy Saturates it.
 */
public final class FireControlPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withBlast(spec.blast().aimingAtCrowds().saturating());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SATURATED);
    }

    @Override
    public TermText describe() {
        return TermText.of("aims at the enemy with the most neighbours in the blast, and each blast that catches an enemy applies ")
                .plus(EffectKind.SATURATED).plus(" to it");
    }
}
