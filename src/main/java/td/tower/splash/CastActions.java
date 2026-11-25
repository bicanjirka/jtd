package td.tower.splash;

/** What an ability may make the Burst do when it casts; each says whether it found a target and cast. */
public interface CastActions {

    /** Charges the next stone with an Arc. */
    boolean primeArc();

    /** Discharges into every enemy in range, each for {@code points} of magic damage. */
    boolean shockwave(float points);

    /**
     * Calls lightning onto the healthiest enemy in range, for {@code points} of magic damage, and dazes it
     * for {@code dazeSeconds}.
     */
    boolean thunderstrike(float points, float dazeSeconds);

    /** Sends {@code hex} at the enemies its rule picks. */
    boolean hex(Hex hex);
}
