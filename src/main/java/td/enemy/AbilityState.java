package td.enemy;

import td.damage.AttackOrigin;
import td.util.ThreadConfined;

/**
 * A live mob's mutable bookkeeping for one {@link Ability}: periodic countdowns, fire-once flags
 * and re-arm state. The {@link Ability} itself stays immutable and shared.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class AbilityState {

    /** Who an effect this ability puts on comes from: one source per mob and ability. */
    private final AttackOrigin source = AttackOrigin.fresh();

    private int ticksRemaining;
    private boolean fired;
    private boolean waitingForReset;
    private boolean armed;

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
            case OnDodgeTrigger t -> 0;
            case KinFallenTrigger t -> t.afterTicks();
        };
    }

    AttackOrigin source() {
        return this.source;
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

    boolean isArmed() {
        return this.armed;
    }

    void arm() {
        this.armed = true;
    }

    boolean isWaitingForReset() {
        return this.waitingForReset;
    }

    void setWaitingForReset(boolean waitingForReset) {
        this.waitingForReset = waitingForReset;
    }
}
