package td.tower.sonar;

import td.effect.Effect;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.Viewpoint;
import td.util.TickRate;

/**
 * Wide Band: the beam reaches invisible enemies too, reveals every one it hits to all towers, and
 * Exposes it for a moment.
 */
public final class WideBandPerk implements SonarPerk {

    private static final float REVEAL_SECONDS = 3f;
    private static final float EXPOSE_SECONDS = 1f;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().withQuery(InRangeTargetQuery.everyone(view.x(), view.y(), view.range())));
    }

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (result.target().isHidden() && !result.killed()) {
            actions.apply(result.target(),
                    sink -> Effect.revealed(Math.round(REVEAL_SECONDS * TickRate.TICKS_PER_SECOND), sink));
            actions.apply(result.target(),
                    sink -> Effect.exposed(Math.round(EXPOSE_SECONDS * TickRate.TICKS_PER_SECOND), sink));
        }
    }
}
