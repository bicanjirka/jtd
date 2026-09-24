package td.enemy;

/**
 * Decides whether an {@link Ability}'s trigger fires this tick, and executes its action. A caller
 * evaluates each ability once per {@code doTick}.
 */
public final class AbilityEvaluator {

    private AbilityEvaluator() {
    }

    public static boolean shouldFire(AbilityTrigger trigger, AbilityState state, AbilityContext context) {
        return switch (trigger) {
            case PeriodicTrigger t -> firePeriodic(t, state);
            case OnceTrigger ignored -> fireOnce(state);
            case HealthThresholdTrigger t -> fireHealthThreshold(t, state, context);
            case OnDeathTrigger ignored -> fireOnDeath(state, context);
            case TimeSinceLastHitTrigger t -> fireTimeSinceLastHit(t, state, context);
            case OnCriticalHitTakenTrigger ignored -> context.justTookCriticalHit();
            case OnFirstDamageTakenTrigger ignored -> fireOnFirstDamageTaken(state, context);
        };
    }

    public static void execute(AbilityAction action, AbilityContext context) {
        switch (action) {
            case ApplyEffectAction a -> context.applyEffect(a.template(), a.target());
            case SpawnEnemiesAction a -> context.spawnEnemies(a.definitionId(), a.shape(), a.consumesSelf());
        }
    }

    private static boolean firePeriodic(PeriodicTrigger trigger, AbilityState state) {
        state.tickDown();
        if (state.ticksRemaining() <= 0) {
            state.resetTicks(trigger.intervalTicks());
            return true;
        }
        return false;
    }

    /** Counts down, fires once, then never again. */
    private static boolean fireOnce(AbilityState state) {
        if (state.isFired()) {
            return false;
        }
        state.tickDown();
        if (state.ticksRemaining() <= 0) {
            state.markFired();
            return true;
        }
        return false;
    }

    /** Fires once, the tick health first reaches the threshold. */
    private static boolean fireHealthThreshold(HealthThresholdTrigger trigger, AbilityState state, AbilityContext context) {
        if (state.isFired() || context.healthFraction() > trigger.fraction()) {
            return false;
        }
        state.markFired();
        return true;
    }

    private static boolean fireOnDeath(AbilityState state, AbilityContext context) {
        if (state.isFired() || !context.justDied()) {
            return false;
        }
        state.markFired();
        return true;
    }

    /** Fires once, the tick the first hit is observed. */
    private static boolean fireOnFirstDamageTaken(AbilityState state, AbilityContext context) {
        if (state.isFired() || !context.justTookDamage()) {
            return false;
        }
        state.markFired();
        return true;
    }

    /** Fires once the idle window is reached, then waits for a hit to re-arm. */
    private static boolean fireTimeSinceLastHit(TimeSinceLastHitTrigger trigger, AbilityState state, AbilityContext context) {
        boolean idleLongEnough = context.ticksSinceLastHit() >= trigger.windowTicks();
        if (!idleLongEnough) {
            state.setWaitingForReset(false);
            return false;
        }
        if (state.isWaitingForReset()) {
            return false;
        }
        state.setWaitingForReset(true);
        return true;
    }
}
