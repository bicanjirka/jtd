package td.tower.seeker;

import td.effect.EffectKind;
import td.tower.BehaviourLine;
import td.tower.BehaviourMarker;
import td.tower.upgrade.PerkText;

import java.util.Optional;

/** Arcane Warhead: every impact Unravels its target, twice over if it was already frozen or chilled. */
public final class ArcaneWarheadPerk implements SeekerPerk {

    private static final int CONTROLLED_STACKS = 2;

    @Override
    public void react(Impact impact, SeekerActions actions) {
        actions.applyStacks(impact.target(), EffectKind.UNRAVELED, impact.wasControlled() ? CONTROLLED_STACKS : 1);
    }

    @Override
    public Optional<BehaviourLine> behaviour() {
        return Optional.of(new BehaviourLine(BehaviourMarker.TARGETING, "Impact applies", "unraveled, " + PerkText.times(CONTROLLED_STACKS) + " if frozen or chilled"));
    }

    @Override
    public String describe() {
        return "every impact applies Unraveled, " + CONTROLLED_STACKS + " stacks if the target was frozen or chilled";
    }
}
