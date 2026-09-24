package td.enemy;


import td.util.ThreadConfined;

/**
 * A live mob's mutable bookkeeping for one {@link Ability}: periodic countdowns, fire-once flags
 * and re-arm state. The {@link Ability} itself stays immutable and shared.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class AbilityState {

    private int ticksRemaining;
    private boolean fired;
    private boolean waitingForReset;

    private AbilityState(int ticksRemaining) {
        this.ticksRemaining = ticksRemaining;
    }

    public static AbilityState forTrigger(AbilityTrigger trigger) {
        return new AbilityState(initialTicksRemaining(trigger));
    }

    private static int initialTicksRemaining(AbilityTrigger trigger) {
        return switch (trigger) {
            case PeriodicTrigger t -> t.intervalTicks();
            case OnceTrigger t -> t.delayTicks();
            case HealthThresholdTrigger t -> 0;
            case OnDeathTrigger t -> 0;
            case TimeSinceLastHitTrigger t -> 0;
            case OnCriticalHitTakenTrigger t -> 0;
            case OnFirstDamageTakenTrigger t -> 0;
        };
    }

    int ticksRemaining() {
        return this.ticksRemaining;
    }

    void tickDown() {
        this.ticksRemaining--;
    }

    void resetTicks(int ticks) {
        this.ticksRemaining = ticks;
    }

    boolean isFired() {
        return this.fired;
    }

    void markFired() {
        this.fired = true;
    }

    boolean isWaitingForReset() {
        return this.waitingForReset;
    }

    void setWaitingForReset(boolean waitingForReset) {
        this.waitingForReset = waitingForReset;
    }
}
