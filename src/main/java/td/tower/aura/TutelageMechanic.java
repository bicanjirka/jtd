package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Tutelage: buffed towers earn more XP. */
public final class TutelageMechanic implements AuraMechanic {

    private static final float XP = 0.1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withTutelageXp(XP);
    }

    @Override
    public TermText describe() {
        return TermText.of("buffed towers earn " + TermNumbers.signedPercent(XP) + " XP, on top of ")
                .plus(TowerTerm.KINSHIP);
    }
}
