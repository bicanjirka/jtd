package td.enemy;

/**
 * Fires once, {@code delayTicks} after spawning. A mob killed first never fires it, since a dead
 * mob no longer ticks.
 */
public record OnceTrigger(int delayTicks) implements AbilityTrigger {
}
