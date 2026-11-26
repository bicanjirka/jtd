package td.tower.seeker;

import td.term.TermText;
import td.tower.TowerTerm;

/** Rich Brood: the second kind of missile comes up more often. */
public final class RichBroodMechanic implements SeekerMechanic {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withBrood(spec.brood().richer());
    }

    @Override
    public TermText describe() {
        return TermText.empty().plus(TowerTerm.RUBY).plus(" or ").plus(TowerTerm.SHADE).plus(" missiles come one in "
                + BroodSpec.frostOnly().withSecond(MissileKind.RUBY).richer().oneIn() + " instead of one in "
                + BroodSpec.frostOnly().withSecond(MissileKind.RUBY).oneIn());
    }
}
