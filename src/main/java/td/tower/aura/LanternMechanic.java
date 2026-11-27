package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Lantern: the light reveals and shields. Without it a Lit tower gains nothing and nothing is Revealed. */
public final class LanternMechanic implements AuraMechanic {

    /** The share of the jam and the blinding a Lit tower takes. */
    public static final float JAM_TAKEN = 0.66f;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withReveal().withJamTakenTimes(JAM_TAKEN);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.REVEALED, TowerTerm.LIT);
    }

    @Override
    public TermText describe() {
        return TermText.of("Lantern: the ").plus(TowerTerm.LIGHT).plus(" gives every enemy inside ")
                .plus(EffectKind.REVEALED).plus(" and makes every other tower in it ").plus(TowerTerm.LIT).plus(": it takes "
                        + JAM_TAKEN + " of the jam and the blinding. Without the Lantern ").plus(TowerTerm.LIT)
                .plus(" towers gain nothing and no enemy gets ").plus(EffectKind.REVEALED);
    }
}
