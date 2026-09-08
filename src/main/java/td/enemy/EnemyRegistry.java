package td.enemy;

/**
 * The live enemies a targeting query or renderer scans - the read-only slice of
 * {@link EnemyRoster} that neither cares about nor is allowed to mutate the roster.
 */
public interface EnemyRegistry {
    EnemyMob[] getEnemies();
}
