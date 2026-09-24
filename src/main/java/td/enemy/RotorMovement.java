package td.enemy;

/** Spins at a constant rate; negative spins the other way. */
public record RotorMovement(float radiansPerTick) implements MovementBehavior {
}
