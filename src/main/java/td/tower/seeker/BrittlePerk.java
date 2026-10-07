package td.tower.seeker;

/** Brittle: a frozen enemy takes extra physical damage. */
public final class BrittlePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().makingBrittle());
    }
}
