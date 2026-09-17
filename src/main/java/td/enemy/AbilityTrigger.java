package td.enemy;

/**
 * The closed set of preconditions that fire an {@link Ability}. A plain enum can't carry each
 * kind's own parameter (an interval, a delay, a threshold, a window), so this is a sealed
 * interface of small records instead - the same shape {@code td.tower.upgrade.UpgradeCondition}
 * and {@code td.ui.render}'s sealed draw-command hierarchies already use in this codebase.
 * {@link AbilityEvaluator} is the one place that pattern-matches over this closed set.
 */
public sealed interface AbilityTrigger permits PeriodicTrigger, OnceTrigger, HealthThresholdTrigger, OnDeathTrigger, TimeSinceLastHitTrigger, OnCriticalHitTakenTrigger {
}
