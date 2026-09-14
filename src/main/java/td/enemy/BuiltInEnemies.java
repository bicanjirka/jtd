package td.enemy;

import java.util.List;

/**
 * The four built-in {@link EnemyDefinition}s migrated from the old {@code EnemyMobCircle}/
 * {@code Square}/{@code Triangle}/{@code Ghost} leaf classes - {@code EnemyFactory.Enemy.create()}
 * builds every mob other than Empty (which stays a standalone class - see {@link EnemyMobEmpty})
 * from one of these via {@link DefinedEnemyMob}, reproducing their exact prior behavior.
 */
final class BuiltInEnemies {

    static final EnemyDefinition CIRCLE = new EnemyDefinition(
            "c", "Simple mob", "No special abilities.",
            1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.CIRCLE, new FixedMovement(),
            List.of(), List.of());

    static final EnemyDefinition SQUARE = new EnemyDefinition(
            "s", "Square mob", "Takes less damage.",
            1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.SQUARE, new RotorMovement((float) Math.toRadians(5.0)),
            List.of(new PercentResistTrait(0.8f, 0.05f)), List.of());

    static final EnemyDefinition TRIANGLE = new EnemyDefinition(
            "t", "Triangle mob", "Increases speed as it takes damage.",
            1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.TRIANGLE, new RotorMovement((float) Math.toRadians(-5.0)),
            List.of(new HurtSpeedTrait(1.4f, 0.1f)), List.of());

    static final EnemyDefinition GHOST = new EnemyDefinition(
            "g", "Ghost mob", "Invisible to all towers. Area damage hurts them.",
            1.28f, 5f, EnemyMob.type.Invisible,
            BodyArchetype.GHOST, new FixedMovement(),
            List.of(), List.of());

    private BuiltInEnemies() {
    }
}
