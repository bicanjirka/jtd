package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;

/** Apprentice: the buffed tower with the least XP earns double. */
public final class ApprenticeMechanic implements AuraMechanic {

    private static final float XP = 1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withApprenticeXp(XP);
    }

    @Override
    public TermText describe() {
        return TermText.of("the buffed tower with the least XP earns " + TermNumbers.times(1f + XP) + " XP from every source");
    }
}
