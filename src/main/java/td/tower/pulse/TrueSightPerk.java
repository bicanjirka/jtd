package td.tower.pulse;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

import java.util.Set;

/** True Sight: the reveal lasts four seconds after leaving, and nothing revealed can be shrouded or fade. */
public final class TrueSightPerk implements PulsePerk {

    private static final int REVEAL_TICKS = Math.round(4f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSight(spec.sight().revealing(REVEAL_TICKS).unveiling());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.UNVEILED);
    }

    @Override
    public String describe() {
        return "the reveal lasts " + PerkText.seconds(REVEAL_TICKS) + " after an enemy leaves, and while it lasts the "
                + "enemy can't be shrouded or fade";
    }
}
