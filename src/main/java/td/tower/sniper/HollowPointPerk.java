package td.tower.sniper;

import td.effect.EffectKind;
import td.tower.targeting.HighestHealthSelector;

/** Hollow Point: a crit leaves the enemy vulnerable; aims at the most health. */
public final class HollowPointPerk implements SniperPerk {

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(new HighestHealthSelector(), "most health"));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.applyStacks(result.target(), EffectKind.VULNERABLE, 1);
        }
    }
}
