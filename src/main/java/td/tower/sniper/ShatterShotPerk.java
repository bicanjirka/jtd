package td.tower.sniper;

import td.effect.EffectKind;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.PreferringSelector;

/** Shatter Shot: a crit on a frozen enemy hits harder. Aims at a frozen enemy first. */
public final class ShatterShotPerk implements SniperPerk {

    private static final float CRIT_DAMAGE_BONUS = 0.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return context.target().hasEffect(EffectKind.FREEZE)
                ? shot.withAttack(attack -> attack.withCritDamageBonus(CRIT_DAMAGE_BONUS)) : shot;
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(PreferringSelector.frozenFirst(new HighestHealthSelector()), "frozen first"));
    }
}
