package td.tower.splash;

import td.tower.upgrade.PerkText;
import td.util.ThreadConfined;

/**
 * Thunderclap: after a crit, the next shot discharges into every enemy in range. Its own crits
 * don't arm it again.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ThunderclapPerk implements SplashPerk {

    private static final float SHARE = 0.5f;

    private boolean armed;

    @Override
    public SplashShot shape(SplashShot shot, ShotContext context) {
        if (!this.armed) {
            return shot;
        }
        this.armed = false;
        return shot.thatDischarges(SHARE);
    }

    @Override
    public void react(ShotResult result) {
        this.armed |= result.critical();
    }

    @Override
    public String describe() {
        return "after a crit, the next shot also arcs into every enemy in range at " + PerkText.percent(SHARE)
                + ", Dazing each one it crits";
    }
}
