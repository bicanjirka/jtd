package td.enemy;

/**
 * Fires exactly once, {@code delayTicks} after the mob spawned - the boss egg's "hatch if not
 * defeated within N seconds" rule. Needs no explicit "cancel if the mob dies first" case: a
 * dead mob's {@code doTick} never runs again, so a delayed ability simply never fires if its
 * mob is killed before the delay elapses.
 */
public record OnceTrigger(int delayTicks) implements AbilityTrigger {
}
