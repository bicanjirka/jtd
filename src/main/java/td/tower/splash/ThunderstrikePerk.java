package td.tower.splash;

/** Thunderstrike: every sixth shot calls lightning onto the healthiest enemy in range. */
public final class ThunderstrikePerk implements SplashPerk {

    private static final int INTERVAL = 6;

    @Override
    public SplashShot shape(SplashShot shot, ShotContext context) {
        return context.shotNumber() % INTERVAL == 0 ? shot.withThunderstrike() : shot;
    }
}
