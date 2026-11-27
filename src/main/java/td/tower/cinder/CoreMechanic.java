package td.tower.cinder;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Core: the middle of the cone, where its target stands, pours more, all the way out to its reach. */
public final class CoreMechanic implements CinderMechanic {

    private static final float SHARE = 0.25f;
    private static final float MORE = 0.4f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCore(new FlameBand(SHARE, MORE));
    }

    @Override
    public TermText describe() {
        return TermText.of("the ").plus(TowerTerm.CORE).plus(", the middle " + TermNumbers.percent(SHARE)
                + " of the cone's width, pours " + TermNumbers.percent(MORE) + " more ").plus(TowerTerm.FLAME);
    }
}
