package td.enemy;

/**
 * Fires once the mob has gone {@code windowTicks} without a hit. Every hit resets the window, so a
 * mob under sustained fire never fires it.
 */
public record TimeSinceLastHitTrigger(int windowTicks) implements AbilityTrigger {
}
