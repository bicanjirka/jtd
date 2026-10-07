package td.tower.sniper;

import td.effect.EffectKind;
import td.tower.targeting.Viewpoint;
import td.tower.targeting.WithEffectTargetQuery;

/** Spotter Uplink: the Sniper may shoot a marked or revealed enemy from twice as far. */
public final class SpotterUplinkPerk implements SniperPerk {

    private static final float REACH = 2f;

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().widenedBy(new WithEffectTargetQuery(view.x(), view.y(),
                view.range() * REACH, EffectKind.MARKED, EffectKind.REVEALED)));
    }
}
