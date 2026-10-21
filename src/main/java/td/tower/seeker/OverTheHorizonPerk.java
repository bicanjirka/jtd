package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.targeting.Viewpoint;
import td.tower.targeting.WithEffectTargetQuery;
import td.tower.upgrade.PerkText;

/** Over the Horizon: the Seeker may fire at a Revealed or Marked enemy from one and a half times as far. */
public final class OverTheHorizonPerk implements SeekerPerk {

    private static final float REACH = 1.5f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().widenedBy(new WithEffectTargetQuery(view.x(), view.y(),
                view.range() * REACH, EffectKind.MARKED, EffectKind.REVEALED)));
    }

    @Override
    public TermText describe() {
        return TermText.of("Over the Horizon: may fire at Revealed or Marked enemies within " + PerkText.times(REACH) + " its range");
    }
}
