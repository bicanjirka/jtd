package td.tower.cinder;

import td.term.TermText;

/** Awaken's Bellows: an Aura's fire-rate buff on this Cinder also widens its cone, 10% for each +10%. */
public final class BellowsPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withBellows();
    }

    @Override
    public TermText describe() {
        return TermText.of("Bellows: an Aura's attack-speed buff on this tower also widens its cone 10% per +10%");
    }
}
