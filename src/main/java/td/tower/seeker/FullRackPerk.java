package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** The last Mixed Payloads node: every missile carries a payload, cycling through all four, each stronger. */
public final class FullRackPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withPayloads(spec.payloads().forEveryMissile());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.FREEZE, EffectKind.UNRAVELED, EffectKind.SILENCED, EffectKind.REVEALED,
                EffectKind.MARKED);
    }

    @Override
    public String describe() {
        return "every missile carries a payload, cycling Cryo, Arcane, EMP, Tracer, each "
                + PerkText.percent(PayloadPlan.FULL_RACK_STRENGTH - 1f) + " stronger";
    }
}
