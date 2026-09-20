package td.enemy;

/**
 * Fires exactly once, the tick this mob is first observed to have taken any damage - the
 * Ghost's vanish-on-first-hit ability. Edge-triggered the same way {@link OnDeathTrigger} is,
 * not repeatable the way {@link OnCriticalHitTakenTrigger} is: once it has vanished, it never
 * fires again, no matter how many more hits land afterward.
 */
public record OnFirstDamageTakenTrigger() implements AbilityTrigger {
}
