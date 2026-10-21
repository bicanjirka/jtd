package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Combustion: an enemy whose fire grows hotter than a single wave could make it bursts for half of what is left within a cell. */
public final class CombustionMechanic implements CinderMechanic {

    private static final float SHARE = 0.5f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCombustion(CombustionSpec.of(SHARE, RADIUS_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(EffectKind.BURN)
                .plus(" that grows hotter than one wave could make it bursts for " + TermNumbers.percent(SHARE)
                        + " of what it has left onto enemies within " + TermNumbers.cells(RADIUS_CELLS)
                        + ", and can burst again once it has cooled to half");
    }
}
