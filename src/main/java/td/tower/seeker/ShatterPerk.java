package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Deep Freeze II: a frozen enemy the Seeker kills shatters, hurting what stands near it. */
public final class ShatterPerk implements SeekerPerk {

    private static final float SHARE = 0.5f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withShatter(spec.shatter().alsoOnKill(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy with ").plus(EffectKind.FREEZE).plus(" this tower kills breaks in a ")
                .plus(TowerTerm.SHATTER).plus(" of " + TermNumbers.percent(SHARE) + " of its damage around it");
    }
}
