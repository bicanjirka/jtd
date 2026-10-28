package td.enemy;

/**
 * Fires every {@code intervalTicks} while alive, and while its health is at least {@code minHealthFraction}.
 *
 * @param minHealthFraction the share of health below which it stops counting and firing; {@code 0} for never
 */
public record PeriodicTrigger(int intervalTicks, float minHealthFraction) implements AbilityTrigger {

    public PeriodicTrigger(int intervalTicks) {
        this(intervalTicks, 0f);
    }
}
