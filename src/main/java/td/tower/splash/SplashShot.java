package td.tower.splash;

/**
 * What a shot does on top of its blast.
 *
 * @param thunderclap   whether it also discharges into every enemy in range as arcs
 * @param thunderstrike whether lightning first strikes the healthiest enemy in range
 */
public record SplashShot(boolean thunderclap, boolean thunderstrike) {

    /** Just the blast and whatever it carries. */
    public static SplashShot plain() {
        return new SplashShot(false, false);
    }

    public SplashShot thatDischarges() {
        return new SplashShot(true, this.thunderstrike);
    }

    public SplashShot withThunderstrike() {
        return new SplashShot(this.thunderclap, true);
    }
}
