package td.enemy;

/**
 * Where abilities add enemies to the live roster; kept apart from the read-only
 * {@link EnemyRegistry}.
 */
public interface EnemySpawner {

    void add(EnemyMob mob);

    /**
     * Swaps {@code outgoing} for {@code incoming}. A transformation, not a kill: no bounty, score
     * or kill credit.
     */
    void replace(EnemyMob outgoing, EnemyMob incoming);
}
