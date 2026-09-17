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
 * One method, not one per enemy *type*: every data-driven enemy is a {@link DefinedEnemyMob}
 * regardless of its {@link EnemyDefinition}, so there is only one concrete class to visit.
 * Kept as a visitor rather than collapsed into a plain method call so a second non-data-driven
 * mob (a wave-timing spacer, a scripted event) can be added later without reopening every call
 * site that already double-dispatches through it.
 */
public interface EnemyMobVisitor<R> {
    R visitDefined(DefinedEnemyMob mob);
}
