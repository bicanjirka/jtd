package td.tower.seeker;

/** Brood: a salvo spreads across different targets. */
public final class BroodPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withSpreadSalvo();
    }

    @Override
    public String describe() {
        return "a salvo spreads across different targets";
    }
}
