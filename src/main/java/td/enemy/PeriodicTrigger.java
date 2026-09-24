package td.enemy;

/** Fires every {@code intervalTicks} while alive. */
public record PeriodicTrigger(int intervalTicks) implements AbilityTrigger {
}
