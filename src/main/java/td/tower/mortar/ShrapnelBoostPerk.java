package td.tower.mortar;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Fragmentation Rounds II: the shrapnel does 30% more of everything and carries its shell's own effect. */
public final class ShrapnelBoostPerk implements MortarPerk {

    private static final float FACTOR = 1.3f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().scaledBy(FACTOR).carryingTheShellsEffect());
    }

    @Override
    public TermText describe() {
        return TermText.of("shrapnel does " + PerkText.percent(FACTOR - 1f) + " more of everything it does, and applies its shell's own effect");
    }
}
