package td.enemy;

/**
 * Double-dispatch over the closed set of concrete {@link EnemyMob} types,
 * used by td.ui's rendering code so it can draw type-specific enemy bodies
 * without an instanceof chain (see CLAUDE.md's rule 9).
 */
public interface EnemyMobVisitor<R> {
    R visitCircle(EnemyMobCircle mob);

    R visitSquare(EnemyMobSquare mob);

    R visitTriangle(EnemyMobTriangle mob);

    R visitGhost(EnemyMobGhost mob);

    R visitEmpty(EnemyMobEmpty mob);
}
