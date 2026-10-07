package td.tower.seeker;

/** A node that makes the nest hold more missiles. */
public final class NestGrowthPerk implements SeekerPerk {

    private final int extra;

    public NestGrowthPerk(int extra) {
        this.extra = extra;
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withNest(spec.nest().holdingMore(this.extra));
    }
}
