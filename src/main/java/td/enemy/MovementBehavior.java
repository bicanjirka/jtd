package td.enemy;

/**
 * The closed set of facing behaviors an {@link EnemyDefinition} picks from - governs which way
 * a mob's body points, not whether it moves at all (a stationary mob, like the Warden's boss
 * egg, is simply one whose {@link EnemyDefinition#baseSpeed()} is {@code 0}). Mirrors
 * {@code AbstractEnemyMobDirectional}/{@code AbstractEnemyMobRotor}'s existing two.
 */
public enum MovementBehavior {
    /** No facing change of its own - a fixed orientation for as long as the mob is alive. */
    FIXED,
    /** Faces the path's own exact geometric direction at the mob's current position. */
    PATH_DIRECTIONAL,
    /** Spins at a constant rate, independent of the path. */
    ROTOR,
    /** A cosmetic pulse with no facing angle of its own. */
    PULSE
}
