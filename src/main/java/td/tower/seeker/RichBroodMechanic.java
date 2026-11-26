package td.tower.seeker;

import td.term.TermText;

/** Rich Brood: the second kind of missile comes up more often. */
public final class RichBroodMechanic implements SeekerMechanic {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withBrood(spec.brood().richer());
    }

    @Override
    public TermText describe() {
        return TermText.of("the second kind of missile comes one in "
                + BroodSpec.frostOnly().withSecond(MissileKind.RUBY).richer().oneIn());
    }
}
