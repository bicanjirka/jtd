package td.tower.cinder;

/** Heat: burning enemies take 10% more damage over time from every source. */
public final class HeatPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().heating());
    }
}
