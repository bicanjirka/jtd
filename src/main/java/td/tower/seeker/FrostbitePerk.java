package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermText;

/** Frostbite: the Seeker's hits on a frozen enemy, or one whose freezes are diminished, always crit. */
public final class FrostbitePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFrostbite();
    }

    @Override
    public TermText describe() {
        return TermText.of("this tower's hits on an enemy with ").plus(EffectKind.FREEZE)
                .plus(" are crits, and so are its hits on an enemy whose next ").plus(EffectKind.FREEZE)
                .plus(" lasts less from diminishing returns");
    }
}
