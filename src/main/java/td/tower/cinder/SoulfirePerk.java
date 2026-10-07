package td.tower.cinder;

/** Soulfire: every other wave is Soulfire, a blue burn in a pool of its own. */
public final class SoulfirePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSoulfire();
    }
}
