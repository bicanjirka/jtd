package td.enemy;

/** The read-only view of the live enemies that targeting and rendering scan. */
public interface EnemyRegistry {
    EnemyMob[] getEnemies();
}
