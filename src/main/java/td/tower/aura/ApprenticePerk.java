package td.tower.aura;

import td.tower.upgrade.PerkText;

/** Apprentice: the buffed tower with the least XP earns double. */
public final class ApprenticePerk implements AuraPerk {

    private static final float XP = 1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withApprenticeXp(XP);
    }

    @Override
    public String describe() {
        return "the buffed tower with the least XP earns " + PerkText.times(1f + XP) + " XP from every source";
    }
}
