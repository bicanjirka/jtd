package td.enemy;

/** What an {@link Ability} does when it fires: apply an effect, spawn enemies, leave a zone, shove its allies or rewind. */
public sealed interface AbilityAction permits ApplyEffectAction, SpawnEnemiesAction, LeaveZoneAction, ShoveAlliesAction, RewindAction {

    /** Whether it puts an effect on the caster or on others. */
    boolean appliesEffects();
}
