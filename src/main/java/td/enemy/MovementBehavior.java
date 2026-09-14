package td.enemy;

/**
 * The closed set of facing behaviors an {@link EnemyDefinition} picks from - governs which way
 * a mob's body points, not whether it moves at all (a stationary mob, like the Warden's boss
 * egg, is simply one whose {@link EnemyDefinition#baseSpeed()} is {@code 0}). A sealed
 * interface rather than a plain enum because {@link RotorMovement} needs its own parameter (a
 * spin rate and direction) - the same reason {@link AbilityTrigger} is one. Replaces
 * {@code AbstractEnemyMobDirectional}/{@code AbstractEnemyMobRotor}'s existing two subclasses.
 */
public sealed interface MovementBehavior permits FixedMovement, PathDirectionalMovement, RotorMovement, PulseMovement {
}
