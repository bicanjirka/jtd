package td.tower.sniper;

import td.tower.targeting.HighestHealthSelector;

/** Fifth Shot: every fifth shot is a guaranteed crit that hits harder; aims at the most health. */
public final class FifthShotPerk implements SniperPerk {

    private static final int INTERVAL = 5;
    private static final float CRIT_DAMAGE_BONUS = 0.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        if (context.shotNumber() % INTERVAL != 0) {
            return shot;
        }
        return shot.withAttack(attack -> attack.withGuaranteedCrit().withCritDamageBonus(CRIT_DAMAGE_BONUS));
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(new HighestHealthSelector(), "most health"));
    }
}
