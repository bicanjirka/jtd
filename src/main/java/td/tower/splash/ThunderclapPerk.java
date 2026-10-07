package td.tower.splash;

import td.util.ThreadConfined;

/**
 * Thunderclap: after a crit, the next shot discharges into every enemy in range. Its own crits
 * don't arm it again.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ThunderclapPerk implements SplashPerk {

    private boolean armed;

    @Override
    public SplashShot shape(SplashShot shot, ShotContext context) {
        if (!this.armed) {
            return shot;
        }
        this.armed = false;
        return shot.thatDischarges();
    }

    @Override
    public void react(ShotResult result, SplashActions actions) {
        this.armed |= result.critical();
    }
}
