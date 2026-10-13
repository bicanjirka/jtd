package td.tower.pulse;

import td.effect.ActiveEffects;
import td.tower.upgrade.PerkText;

/** Attune's Toll: each second an enemy spends in the field adds a stack, up to five. */
public final class TollPerk implements PulsePerk {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.tolling();
    }

    @Override
    public String describe() {
        return "Toll: each second an enemy stays inside adds a stack, fading after it leaves; every debuff on it wears off "
                + PerkText.percent(ActiveEffects.TOLL_SLOWDOWN_PER_STACK) + " slower a stack";
    }
}
