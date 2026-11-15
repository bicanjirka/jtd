package td.tower.pulse;

/**
 * Gravity's hard stop: an enemy {@code afterTicks} inside the field is Dazed for {@code dazeTicks}, once a visit.
 * {@link #none()} Dazes nobody.
 */
public record GravitySpec(int afterTicks, int dazeTicks) {

    private static final GravitySpec NONE = new GravitySpec(0, 0);

    public static GravitySpec none() {
        return NONE;
    }

    public boolean isActive() {
        return this.dazeTicks > 0;
    }
}
