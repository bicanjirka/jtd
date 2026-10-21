package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

/** Attune's Stoke: a wave on an enemy already burning from this Cinder raises its burn, up to three times. */
public final class StokeMechanic implements CinderMechanic {

    private static final int MAX_STEPS = 3;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(StokeSpec.of(MAX_STEPS));
    }

    @Override
    public TermText describe() {
        return TermText.of("a wave on an enemy with this tower's ").plus(EffectKind.BURN).plus(" raises its ")
                .plus(TowerTerm.STOKE).plus(" a step, up to " + MAX_STEPS + "; ").plus(TowerTerm.STOKE)
                .plus(" goes when that ").plus(EffectKind.BURN).plus(" ends");
    }
}
