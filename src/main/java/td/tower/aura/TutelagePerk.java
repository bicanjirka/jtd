package td.tower.aura;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Tutelage: buffed towers earn more XP. */
public final class TutelagePerk implements AuraPerk {

    private static final float XP = 0.1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withTutelageXp(XP);
    }

    @Override
    public TermText describe() {
        return TermText.of("buffed towers earn +" + PerkText.percent(XP) + " XP, on top of Kinship");
    }
}
