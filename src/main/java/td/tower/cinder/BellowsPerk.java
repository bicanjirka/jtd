package td.tower.cinder;

import td.term.TermText;
import td.tower.TowerTerm;

/** Awaken's Bellows: an Aura's fire-rate buff on this Cinder also widens its cone, 10% for each +10%. */
public final class BellowsPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withBellows();
    }

    @Override
    public TermText describe() {
        return TermText.of("attack speed an ").plus(TowerTerm.AURA)
                .plus(" gives this tower also widens its cone, as much as it speeds it");
    }
}
