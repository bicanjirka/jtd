package td.tower.cinder;

/** Inferno Ring: the cone becomes a full ring. */
public final class InfernoRingPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withRing();
    }
}
