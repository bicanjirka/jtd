package td.enemy;

/**
 * Double dispatch over the concrete {@link EnemyMob} types, so rendering needs no type checks.
 * Adding a method forces every visitor to handle the new type.
 */
public interface EnemyMobVisitor<R> {
    R visitDefined(DefinedEnemyMob mob);
}
