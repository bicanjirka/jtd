package td.tower.sniper;

import td.tower.targeting.HighestRankSelector;
import td.util.ThreadConfined;

/**
 * Momentum: the shot after a crit hits five times as hard and ignores armor and plating, and a kill
 * speeds the Sniper up for a few seconds. Aims at the highest rank.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MomentumPerk implements SniperPerk {

    private static final float DAMAGE_FACTOR = 5f;

    private boolean charged;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        if (!this.charged) {
            return shot;
        }
        return shot.scaledBy(DAMAGE_FACTOR)
                .withAttack(attack -> attack.withArmorPenetration(1f, 0f).withPlatingPenetration(1f));
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(new HighestRankSelector(), "highest rank"));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        this.charged = result.critical();
        if (result.killed()) {
            actions.startBurst();
        }
    }
}
