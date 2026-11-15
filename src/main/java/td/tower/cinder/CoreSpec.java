package td.tower.cinder;

/**
 * The Core of the Scorcher's cone: the middle {@code share} of its width, where it pours {@code factor} times the
 * Flame.
 */
public record CoreSpec(float share, float factor) {

    private static final CoreSpec NONE = new CoreSpec(0f, 1f);

    /** No Core: the whole cone pours alike. */
    public static CoreSpec none() {
        return NONE;
    }

    public boolean isActive() {
        return this.share > 0f && this.factor != 1f;
    }
}
