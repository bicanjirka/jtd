package td.tower.sniper;

import td.damage.AttackProfile;

/** Fifth Shot: every fifth shot is a guaranteed crit, and the Sniper's crits hit harder. */
public final class FifthShotPerk implements SniperPerk {

    private static final int INTERVAL = 5;
    private static final float CRIT_MULTIPLIER = 2.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        SniperShot harder = shot.withAttack(attack -> attack.withCritMultiplier(CRIT_MULTIPLIER));
        return context.shotNumber() % INTERVAL == 0 ? harder.withAttack(AttackProfile::withGuaranteedCrit) : harder;
    }
}
