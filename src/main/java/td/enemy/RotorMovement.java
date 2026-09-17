package td.enemy;

/**
 * Spins at a constant rate, independent of the path - {@code radiansPerTick} may be negative to spin the other way. Square and Triangle's behavior.
 */
public record RotorMovement(float radiansPerTick) implements MovementBehavior {
}
