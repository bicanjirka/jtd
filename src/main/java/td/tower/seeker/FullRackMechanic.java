package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** The last Mixed Payloads node: every missile carries a payload, cycling through all four, each stronger. */
public final class FullRackMechanic implements SeekerMechanic {

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
    public TermText describe() {
        return TermText.of("every missile carries a payload, taking turns: ").plus(TowerTerm.CRYO_MISSILE).plus(", ")
                .plus(TowerTerm.ARCANE_MISSILE).plus(", ").plus(TowerTerm.EMP_MISSILE).plus(", ")
                .plus(TowerTerm.TRACER_MISSILE)
                .plus(", each " + TermNumbers.percent(PayloadPlan.FULL_RACK_STRENGTH - 1f) + " stronger");
    }
}
