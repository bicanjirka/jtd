package td.enemy;

import java.util.List;

/**
 * The four built-in {@link EnemyDefinition}s migrated from the old {@code EnemyMobCircle}/
 * {@code Square}/{@code Triangle}/{@code Ghost} leaf classes - {@link EnemyCatalog#builtIn()}
 * pre-registers these under their wave-script letters, reproducing their exact prior behavior
 * via {@link DefinedEnemyMob}. Empty stays a standalone class, never one of these - see
 * {@link EnemyMobEmpty}.
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
