package td.tower.aura;

import td.tower.upgrade.PerkText;

/** Chosen: the aura buffs only its most experienced tower, more strongly. */
public final class ChosenPerk implements AuraPerk {

    private static final float STRENGTH = 3f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.choosing(STRENGTH);
    }

    @Override
    public String describe() {
        return "buffs only the most experienced tower in range, at " + PerkText.times(STRENGTH) + " strength";
    }
}
