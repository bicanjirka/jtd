package td.enemy;

/**
 * Fires once the mob has gone {@code windowTicks} without taking a hit - an "ignored too long"
 * punish. The window resets to zero on every hit landed, so a mob under sustained fire never
 * fires this, regardless of how long it's been alive.
 */
public record TimeSinceLastHitTrigger(int windowTicks) implements AbilityTrigger {
}
