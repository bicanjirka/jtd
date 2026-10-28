package td.enemy;

/** How a mob's body is posed: which way it faces and how large it is drawn. Whether it moves at all is its speed. */
public sealed interface MovementBehavior permits FixedMovement, PathDirectionalMovement, RotorMovement, PulseMovement, SwingMovement {
}
