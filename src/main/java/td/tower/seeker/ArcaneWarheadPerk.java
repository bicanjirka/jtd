package td.tower.seeker;

import td.effect.EffectKind;
import td.tower.BehaviourLine;
import td.tower.BehaviourMarker;

import java.util.Optional;

/** Arcane Warhead: every impact Unravels its target, twice over if it was already frozen or chilled. */
public final class ArcaneWarheadPerk implements SeekerPerk {

    @Override
    public void react(Impact impact, SeekerActions actions) {
        actions.applyStacks(impact.target(), EffectKind.UNRAVELED, impact.wasControlled() ? 2 : 1);
    }

    @Override
    public Optional<BehaviourLine> behaviour() {
        return Optional.of(new BehaviourLine(BehaviourMarker.TARGETING, "Impact applies", "unraveled, x2 if frozen or chilled"));
    }
}
