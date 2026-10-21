package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;

/** Chosen: the aura buffs only its most experienced tower, more strongly. */
public final class ChosenPerk implements AuraPerk {

    private static final float STRENGTH = 3f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.choosing(STRENGTH);
    }

    @Override
    public TermText describe() {
        return TermText.of("buffs only the most experienced tower in range, at " + TermNumbers.times(STRENGTH) + " strength");
    }
}
