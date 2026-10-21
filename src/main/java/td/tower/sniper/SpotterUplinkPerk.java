package td.tower.sniper;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.targeting.Viewpoint;
import td.tower.targeting.WithEffectTargetQuery;
import td.tower.upgrade.PerkText;

/** Spotter Uplink: the Sniper may shoot a marked or revealed enemy from twice as far. */
public final class SpotterUplinkPerk implements SniperPerk {

    private static final float REACH = 2f;

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().widenedBy(new WithEffectTargetQuery(view.x(), view.y(),
                view.range() * REACH, EffectKind.MARKED, EffectKind.REVEALED)));
    }

    @Override
    public TermText describe() {
        return TermText.of("may shoot Marked or Revealed enemies within " + PerkText.times(REACH) + " its range");
    }
}
