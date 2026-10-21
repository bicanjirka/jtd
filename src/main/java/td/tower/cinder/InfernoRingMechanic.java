package td.tower.cinder;

import td.term.TermText;

/** Inferno Ring: the cone becomes a full ring. */
public final class InfernoRingMechanic implements CinderMechanic {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withRing();
    }

    @Override
    public TermText describe() {
        return TermText.of("the cone becomes a full ring");
    }
}
