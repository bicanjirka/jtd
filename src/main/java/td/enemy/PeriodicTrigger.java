package td.enemy;

/** Fires every {@code intervalTicks} while the mob is alive - the Warden's reinforcement and self-shield abilities. */
public record PeriodicTrigger(int intervalTicks) implements AbilityTrigger {
}
