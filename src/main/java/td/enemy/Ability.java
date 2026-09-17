package td.enemy;

/**
 * One triggered, active behavior an {@link EnemyDefinition} carries - see {@link AbilityTrigger}/{@link AbilityAction}.
 */
public record Ability(AbilityTrigger trigger, AbilityAction action) {
}
