package td.tower.seeker;

import td.term.TermText;

/** Brood: a salvo spreads across different targets. */
public final class BroodPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withSpreadSalvo();
    }

    @Override
    public TermText describe() {
        return TermText.of("a salvo spreads across different targets");
    }
}
