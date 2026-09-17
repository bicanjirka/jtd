package td.enemy;

/**
 * Double-dispatch over the closed set of concrete {@link EnemyMob} types,
 * used by td.ui's rendering code so it can draw type-specific enemy bodies
 * without an instanceof chain (see CLAUDE.md §5 rule 11).
 * <p>
 * Adding a method here is deliberately a breaking change: it forces every
 * implementor - the frame builders in {@code td.ui} - to describe the new
 * enemy rather than silently skipping it.
 * <p>
 * Only two methods, not one per enemy *type*: every data-driven enemy is a {@link DefinedEnemyMob}
 * regardless of its {@link EnemyDefinition}, so there is only one real concrete class left to
 * visit for rendering - {@link EnemyMobEmpty} is the one deliberate exception, a wave-timing
 * spacer that is never drawn at all (see its own doc comment).
 */
public interface EnemyMobVisitor<R> {
    R visitDefined(DefinedEnemyMob mob);

    R visitEmpty(EnemyMobEmpty mob);
}
