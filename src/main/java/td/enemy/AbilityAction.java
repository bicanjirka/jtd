package td.enemy;

/** What an {@link Ability} does when it fires: apply an effect, spawn enemies, leave a zone or shove its allies. */
public sealed interface AbilityAction permits ApplyEffectAction, SpawnEnemiesAction, LeaveZoneAction, ShoveAlliesAction {

    /** Whether it puts an effect on the caster or on others. */
    boolean appliesEffects();
}
