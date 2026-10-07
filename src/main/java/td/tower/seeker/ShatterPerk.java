package td.tower.seeker;

/** Deep Freeze II: a frozen enemy the Seeker kills shatters, hurting what stands near it. */
public final class ShatterPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withShatter(spec.shatter().alsoOnKill());
    }
}
