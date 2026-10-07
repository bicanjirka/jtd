package td.tower.cinder;

/** Pyromancer's Mark: burning enemies take 15% more magic damage from every source. */
public final class PyromancersMarkPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withTuning(spec.tuning().marking());
    }
}
