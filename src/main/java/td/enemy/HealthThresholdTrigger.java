package td.enemy;

/**
 * Fires exactly once, the tick health first crosses (falls to or below) {@code fraction} of
 * max health - edge-triggered, not evaluated fresh every tick while below the threshold, so a
 * mob sitting at 30% health for many ticks fires this once, not repeatedly.
 */
public record HealthThresholdTrigger(float fraction) implements AbilityTrigger {
}
