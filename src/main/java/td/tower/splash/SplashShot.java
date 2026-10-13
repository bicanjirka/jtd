package td.tower.splash;

/**
 * What a shot does on top of its blast.
 *
 * @param thunderclapShare    the share of the blast each Thunderclap arc into an enemy in range deals; 0 for none
 * @param thunderstrikeFactor how many times the blast a Thunderstrike's lightning deals; 0 for none
 */
public record SplashShot(float thunderclapShare, float thunderstrikeFactor) {

    /** How long a Thunderclap or a Thunderstrike Dazes. */
    public static final float DAZE_SECONDS = 0.5f;

    /** Just the blast and whatever it carries. */
    public static SplashShot plain() {
        return new SplashShot(0f, 0f);
    }

    /** Also discharges into every enemy in range as arcs, each for {@code share} of the blast. */
    public SplashShot thatDischarges(float share) {
        return new SplashShot(share, this.thunderstrikeFactor);
    }

    /** First calls lightning onto the healthiest enemy in range, for {@code factor} times the blast. */
    public SplashShot withThunderstrike(float factor) {
        return new SplashShot(this.thunderclapShare, factor);
    }

    public boolean thunderclap() {
        return this.thunderclapShare > 0f;
    }

    public boolean thunderstrike() {
        return this.thunderstrikeFactor > 0f;
    }
}
