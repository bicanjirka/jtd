package td.tower.sniper;

import td.util.ThreadConfined;

/** Tradecraft: a crit makes the next crit hit harder, two in a row harder still, and no further. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CritStreakPerk implements SniperPerk {

    private static final float CRIT_DAMAGE_PER_STREAK = 0.25f;
    private static final int MAX_STREAK = 2;

    private int streak;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withCritDamageBonus(CRIT_DAMAGE_PER_STREAK * this.streak));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        this.streak = result.critical() ? Math.min(MAX_STREAK, this.streak + 1) : 0;
    }
}
