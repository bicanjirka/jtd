package td.enemy;

/**
 * The narrow seam {@link AbilityEvaluator} spawns enemies through - deliberately separate from
 * {@link EnemyRegistry} (the read-only view targeting/rendering depend on), since evaluating an
 * ability only ever needs to add to or replace within the live roster, never read every enemy
 * on the board. No production implementation exists yet; {@code EnemyRoster} gains one once
 * real spawning is wired up (see {@code docs/features/FEATURE-enemy-traits-and-effects.md}).
 */
public interface EnemySpawner {

    /** Adds a new, independent enemy to the roster - the Warden's periodic reinforcement and its on-death egg spawn. */
    void add(EnemyMob mob);

    /**
     * Removes {@code outgoing} and adds {@code incoming} as one step - a transformation, not a
     * kill: {@code outgoing} earns no bounty, no score, and no kill-count-gate credit. The boss
     * egg's hatch-on-timeout ability is the only v1 use of this.
     */
    void replace(EnemyMob outgoing, EnemyMob incoming);
}
