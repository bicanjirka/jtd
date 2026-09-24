package td.enemy;

/** Which way a mob's body faces. Whether it moves at all is its speed. */
public sealed interface MovementBehavior permits FixedMovement, PathDirectionalMovement, RotorMovement, PulseMovement {
}
