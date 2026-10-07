package td.tower.cinder;

/** Cauterize: burning enemies receive half the healing and shielding. */
public final class CauterizePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().cauterizing());
    }
}
