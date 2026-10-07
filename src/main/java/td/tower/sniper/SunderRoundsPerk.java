package td.tower.sniper;

import td.effect.EffectKind;

/** Sunder Rounds: every crit sunders the enemy. */
public final class SunderRoundsPerk implements SniperPerk {

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.applyStacks(result.target(), EffectKind.SUNDERED, 1);
        }
    }
}
