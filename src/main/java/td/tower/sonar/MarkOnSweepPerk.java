package td.tower.sonar;

import td.effect.Effect;
import td.tower.BehaviourLine;
import td.tower.BehaviourMarker;

import java.util.Optional;

/**
 * Mark on Sweep: every enemy the beam hits is marked until the next pass, so the next hit on it from
 * any tower crits. The beam's own hit lands first, so it spends the mark of the pass before.
 */
public final class MarkOnSweepPerk implements SonarPerk {

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (!result.killed()) {
            actions.apply(result.target(), sink -> Effect.marked(result.spec().untilNextPass(), sink));
        }
    }

    @Override
    public Optional<BehaviourLine> behaviour() {
        return Optional.of(new BehaviourLine(BehaviourMarker.TARGETING, "Marks", "next hit crits"));
    }

    @Override
    public String describe() {
        return "marks every enemy the beam hits: the next hit on it from any tower is a guaranteed crit";
    }
}
