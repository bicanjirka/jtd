package td.tower.cinder;

/** Awaken's Bellows: an Aura's fire-rate buff on this Cinder also widens its cone, 10% for each +10%. */
public final class BellowsPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withBellows();
    }
}
