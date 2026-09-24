package td.enemy;

/** The closed set of preconditions that fire an {@link Ability}. */
public sealed interface AbilityTrigger permits PeriodicTrigger, OnceTrigger, HealthThresholdTrigger, OnDeathTrigger, TimeSinceLastHitTrigger, OnCriticalHitTakenTrigger, OnFirstDamageTakenTrigger {
}
