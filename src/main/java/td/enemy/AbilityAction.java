package td.enemy;

/** What an {@link Ability} does when it fires: apply an effect, spawn enemies or leave a zone. */
public sealed interface AbilityAction permits ApplyEffectAction, SpawnEnemiesAction, LeaveZoneAction {

    /** Whether it puts an effect on the caster or on others. */
    boolean appliesEffects();
}
