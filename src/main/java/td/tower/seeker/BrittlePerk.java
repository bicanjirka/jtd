package td.tower.seeker;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.tower.upgrade.PerkText;

/** Brittle: a frozen enemy takes extra physical damage. */
public final class BrittlePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().makingBrittle());
    }

    @Override
    public String describe() {
        return "frozen enemies take +" + PerkText.percent(EffectKind.BRITTLE.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                + " physical damage";
    }
}
