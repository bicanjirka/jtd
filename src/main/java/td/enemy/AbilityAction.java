package td.enemy;

/** What an {@link Ability} does when it fires: apply an effect or spawn enemies. */
public sealed interface AbilityAction permits ApplyEffectAction, SpawnEnemiesAction {
}
