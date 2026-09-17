package td.enemy;

/**
 * No facing change of its own - a fixed orientation for as long as the mob is alive. Circle and Ghost's behavior.
 */
public record FixedMovement() implements MovementBehavior {
}
