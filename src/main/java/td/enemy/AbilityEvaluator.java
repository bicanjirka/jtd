package td.enemy;

/**
 * Decides whether an {@link Ability}'s {@link AbilityTrigger} fires this tick, and executes its
 * {@link AbilityAction} when it does. Pattern-matches over both closed, sealed hierarchies -
 * the same narrow, compiler-checked exception to the no-{@code instanceof} rule
 * {@code Java2DFrameRenderer} already has for {@code td.ui.render}'s sealed draw-command
 * hierarchies (root {@code CLAUDE.md}). A caller evaluates once per {@code doTick}:
 * {@code if (AbilityEvaluator.shouldFire(ability.trigger(), state, context)) AbilityEvaluator.execute(ability.action(), context);}
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
            case SpawnEnemiesAction a -> context.spawnEnemies(a.definitionId(), a.count(), a.consumesSelf());
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

    /**
     * Counts down from {@code delayTicks}, fires once, then never again - see {@link OnceTrigger}.
     */
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

    /**
     * Edge-triggered: fires once, the tick health first reaches the threshold - see {@link HealthThresholdTrigger}.
     */
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

    /**
     * Edge-triggered like {@link #fireOnDeath} - fires once, the tick a hit is first observed,
     * then never again - see {@link OnFirstDamageTakenTrigger}.
     */
    private static boolean fireOnFirstDamageTaken(AbilityState state, AbilityContext context) {
        if (state.isFired() || !context.justTookDamage()) {
            return false;
        }
        state.markFired();
        return true;
    }

    /**
     * Fires once the idle window is reached, then waits for a hit (which resets
     * {@code ticksSinceLastHit} below the window) before it can fire again - see
     * {@link TimeSinceLastHitTrigger}.
     */
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
