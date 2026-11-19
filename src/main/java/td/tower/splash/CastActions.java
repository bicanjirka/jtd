package td.tower.splash;

/** What an ability may make the Burst do when it casts; each says whether it found a target and cast. */
public interface CastActions {

    /** Charges the next stone with an Arc. */
    boolean primeArc();

    /** Discharges into every enemy in range, each for {@code share} of base damage as magic. */
    boolean thunderclap(float share);

    /**
     * Calls lightning onto the healthiest enemy in range, for {@code factor} times base damage as magic, and dazes it
     * for {@code dazeSeconds}.
     */
    boolean thunderstrike(float factor, float dazeSeconds);

    /** Sends {@code hex} at the enemies its rule picks. */
    boolean hex(Hex hex);
}
