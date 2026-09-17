package td.enemy;

/**
 * Fires every time this mob survives a critical hit - repeatable, unlike a fire-once trigger
 * ({@link OnceTrigger}/{@link HealthThresholdTrigger}/{@link OnDeathTrigger}): "took another
 * crit" is a recurring event over a mob's life, not a one-time transition the way death or a
 * health threshold is. Needs no {@link AbilityState} bookkeeping of its own - see
 * {@link AbilityEvaluator}, which reads {@link AbilityContext#justTookCriticalHit()} directly.
 */
public record OnCriticalHitTakenTrigger() implements AbilityTrigger {
}
