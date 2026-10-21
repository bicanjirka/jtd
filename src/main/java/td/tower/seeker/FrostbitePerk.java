package td.tower.seeker;

import td.term.TermText;

/** Frostbite: the Seeker's hits on a frozen enemy, or one whose freezes are diminished, always crit. */
public final class FrostbitePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFrostbite();
    }

    @Override
    public TermText describe() {
        return TermText.of("this tower's hits on a frozen enemy are guaranteed crits, and so are its hits on an enemy whose freezes are "
                + "diminished by repeated freezing");
    }
}
