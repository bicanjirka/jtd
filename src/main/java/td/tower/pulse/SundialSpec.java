package td.tower.pulse;

/**
 * The stone's shadow: every {@code periodTicks} it turns once around the stone over {@code turnTicks}, out to
 * {@code reachCells}, and every enemy it passes takes an aimed hit of {@code damagePoints} and a Toll stack.
 * {@link #none()} casts no shadow.
 *
 * @param inFieldFactor what the hit is multiplied by on an enemy inside the field
 * @param shadow        whether the hit is Shadow damage
 */
public record SundialSpec(int periodTicks, int turnTicks, float reachCells, float damagePoints, float inFieldFactor,
                          boolean shadow) {

    private static final SundialSpec NONE = new SundialSpec(0, 0, 0f, 0f, 1f, false);

    public static SundialSpec none() {
        return NONE;
    }

    public boolean isActive() {
        return this.periodTicks > 0;
    }

    public SundialSpec withPeriodTicks(int ticks) {
        return new SundialSpec(ticks, this.turnTicks, this.reachCells, this.damagePoints, this.inFieldFactor,
                this.shadow);
    }

    public SundialSpec withInFieldFactor(float factor) {
        return new SundialSpec(this.periodTicks, this.turnTicks, this.reachCells, this.damagePoints, factor,
                this.shadow);
    }

    public SundialSpec asShadow() {
        return new SundialSpec(this.periodTicks, this.turnTicks, this.reachCells, this.damagePoints,
                this.inFieldFactor, true);
    }
}
