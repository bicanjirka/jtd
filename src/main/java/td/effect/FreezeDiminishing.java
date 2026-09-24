package td.effect;

import td.util.ThreadConfined;
import td.util.TickRate;

/**
 * One mob's freeze diminishing returns: successive fresh freezes last 100%, 50%, 25%, then none,
 * until {@link #WINDOW_TICKS} pass after the last freeze that landed. A freeze reapplied while the
 * mob is still frozen uses the current step, so two hits landing together spend one step.
 * <p>
 * Times are the mob's own tick count, since an effect is applied without the game time.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class FreezeDiminishing {

    public static final int WINDOW_TICKS = Math.round(10 * TickRate.TICKS_PER_SECOND);
    private static final float[] STEPS = {1f, 0.5f, 0.25f, 0f};

    private int freshFreezes;
    private int lastFreezeTick = -1;

    /** The duration factor a freeze applied at {@code tick} gets, without recording it. */
    public float factorAt(int tick, boolean alreadyFrozen) {
        int step = this.stepAt(tick);
        if (alreadyFrozen && step > 0) {
            return STEPS[step - 1];
        }
        return STEPS[Math.min(step, STEPS.length - 1)];
    }

    /** Records a freeze that landed at {@code tick}; only a fresh one advances the step. */
    public void recordAt(int tick, boolean alreadyFrozen) {
        this.freshFreezes = this.stepAt(tick);
        if (!alreadyFrozen) {
            this.freshFreezes++;
        }
        this.lastFreezeTick = tick;
    }

    /** How many fresh freezes have landed in the current window. */
    public int stepAt(int tick) {
        if (this.lastFreezeTick < 0 || tick - this.lastFreezeTick >= WINDOW_TICKS) {
            return 0;
        }
        return this.freshFreezes;
    }
}
