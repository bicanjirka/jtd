package td.tower.sniper;

import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.PreferringSelector;

/** Shatter Shot: a crit on a frozen enemy hits harder. Aims at a frozen enemy first. */
public final class ShatterShotPerk implements SniperPerk {

    private static final float CRIT_DAMAGE_BONUS = 0.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return context.target().isStopped()
                ? shot.withAttack(attack -> attack.withCritDamageBonus(CRIT_DAMAGE_BONUS)) : shot;
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(PreferringSelector.stoppedFirst(new HighestHealthSelector()), "frozen or dazed first"));
    }
}
