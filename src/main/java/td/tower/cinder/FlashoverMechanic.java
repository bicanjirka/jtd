package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Flashover: while it streams, an enemy in the cone whose burn holds enough fuel flashes over, and every enemy near it
 * takes part of the Flame too.
 */
public final class FlashoverMechanic implements CinderMechanic {

    /** The fuel, in points, a burn holds from which it flashes over. */
    public static final float HOT_FUEL_POINTS = 250f;
    /** The share of the Flame each enemy beside a hot one takes. */
    public static final float SHARE = 0.5f;
    public static final float REACH_CELLS = 1.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withFlashover();
    }

    @Override
    public TermText describe() {
        return TermText.of("while it streams, an enemy in the cone whose ").plus(EffectKind.BURN)
                .plus(" holds " + TermNumbers.whole(HOT_FUEL_POINTS) + " fuel or more flashes over: every enemy within "
                        + TermNumbers.cells(REACH_CELLS) + " of it takes " + TermNumbers.percent(SHARE) + " of the ")
                .plus(TowerTerm.FLAME).plus(" too");
    }
}
