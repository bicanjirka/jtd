package td.enemy;

/**
 * Double-dispatch over the closed set of concrete {@link EnemyMob} types,
 * used by td.ui's rendering code so it can draw type-specific enemy bodies
 * without an instanceof chain (see CLAUDE.md's no-instanceof rule).
 * <p>
 * Adding a method here is deliberately a breaking change: it forces every
 * implementor - the frame builders in {@code td.ui} - to describe the new
 * enemy rather than silently skipping it.
 */
public interface EnemyMobVisitor<R> {
    R visitCircle(EnemyMobCircle mob);

    R visitSquare(EnemyMobSquare mob);

    R visitTriangle(EnemyMobTriangle mob);

    R visitGhost(EnemyMobGhost mob);

    R visitEmpty(EnemyMobEmpty mob);
}
