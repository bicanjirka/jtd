package td.effect;

/**
 * A shield's pool as the inspector shows it, in points.
 *
 * @param remaining what is left to absorb
 * @param max       the pool it started with
 */
public record ShieldGauge(float remaining, float max) {

    private static final ShieldGauge NONE = new ShieldGauge(0f, 0f);

    public static ShieldGauge none() {
        return NONE;
    }
}
