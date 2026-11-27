package td.tower.cinder;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Blue Flame: the middle of the cone glows white and pours more, all the way out to its reach, on top of the Core,
 * whose fire turns blue; in either kind of fire.
 */
public final class BlueFlameMechanic implements CinderMechanic {

    private static final float SHARE = 0.6f;
    private static final float MORE = 0.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withMiddle(new FlameBand(SHARE, MORE));
    }

    @Override
    public TermText describe() {
        return TermText.of("the middle " + TermNumbers.percent(SHARE) + " of the cone's width glows white and pours "
                + TermNumbers.percent(MORE) + " more ").plus(TowerTerm.FLAME).plus(", on top of the ")
                .plus(TowerTerm.CORE);
    }
}
