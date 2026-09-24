package td.enemy;

/** Fires once, the tick health first falls to or below {@code fraction} of max. */
public record HealthThresholdTrigger(float fraction) implements AbilityTrigger {
}
