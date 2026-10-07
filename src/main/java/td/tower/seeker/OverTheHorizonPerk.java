package td.tower.seeker;

import td.effect.EffectKind;
import td.tower.targeting.Viewpoint;
import td.tower.targeting.WithEffectTargetQuery;

/** Over the Horizon: the Seeker may fire at a Revealed or Marked enemy from one and a half times as far. */
public final class OverTheHorizonPerk implements SeekerPerk {

    private static final float REACH = 1.5f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().widenedBy(new WithEffectTargetQuery(view.x(), view.y(),
                view.range() * REACH, EffectKind.MARKED, EffectKind.REVEALED)));
    }
}
