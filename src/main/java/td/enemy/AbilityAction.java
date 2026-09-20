package td.enemy;

/**
 * What an {@link Ability} does when its {@link AbilityTrigger} fires - apply an effect, or
 * spawn one or more new enemies. {@link AbilityEvaluator} (to execute one), {@link EnemyCatalog}
 * (to walk spawn references for cycle detection) and {@link EnemyDefinition#supportAura()} (to
 * find the largest radius-targeted effect a definition projects) are the only three places
 * that pattern-match over this closed pair, via an exhaustive {@code switch} - the same narrow,
 * compiler-checked exception to the no-{@code instanceof} rule {@code Java2DFrameRenderer}
 * already has for {@code td.ui.render}'s sealed draw-command hierarchies - not a general license
 * to branch on {@code EnemyMob}/{@link Trait} types.
 */
public sealed interface AbilityAction permits ApplyEffectAction, SpawnEnemiesAction {
}
