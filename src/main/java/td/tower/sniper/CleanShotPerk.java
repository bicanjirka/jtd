package td.tower.sniper;

import td.damage.AttackProfile;

/** Clean Shot: crits go through shields straight to health. */
public final class CleanShotPerk implements SniperPerk {

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(AttackProfile::withCritsPierceShields);
    }
}
