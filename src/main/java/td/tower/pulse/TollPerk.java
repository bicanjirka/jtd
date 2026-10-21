package td.tower.pulse;

import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.term.Term;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Attune's Toll: each second an enemy spends in the field adds a stack, up to five. */
public final class TollPerk implements PulsePerk {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.tolling();
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.TOLL);
    }

    @Override
    public String describe() {
        return "Toll: each second an enemy stays inside adds a stack, fading after it leaves; the field's effects on it start at "
                + PerkText.percent((float) FieldStrength.ENTRY / FieldStrength.FULL) + " strength and grow "
                + PerkText.percent(1f / FieldStrength.FULL) + " a stack";
    }
}
