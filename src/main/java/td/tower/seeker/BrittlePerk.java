package td.tower.seeker;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.Term;
import td.term.TermText;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Brittle: a frozen enemy takes extra physical damage. */
public final class BrittlePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().makingBrittle());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BRITTLE);
    }

    @Override
    public TermText describe() {
        return TermText.of("frozen enemies take +" + PerkText.percent(EffectKind.BRITTLE.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                + " physical damage");
    }
}
