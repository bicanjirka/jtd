package td.tower.sniper;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.Viewpoint;
import td.tower.targeting.WithEffectTargetQuery;

/** Spotter Uplink: the Sniper may shoot a marked or revealed enemy from twice as far. */
public final class SpotterUplinkMechanic implements SniperMechanic {

    private static final float REACH = 2f;

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        Viewpoint view = spec.view();
        return spec.withReach(spec.reach().widenedBy(new WithEffectTargetQuery(view.x(), view.y(),
                view.range() * REACH, EffectKind.MARKED, EffectKind.REVEALED)));
    }

    @Override
    public TermText describe() {
        return TermText.of("may shoot an enemy with ").plus(EffectKind.MARKED).plus(" or ").plus(EffectKind.REVEALED)
                .plus(" within " + TermNumbers.times(REACH) + " its range");
    }
}
