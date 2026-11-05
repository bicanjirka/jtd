package td.effect;

import td.util.ThreadConfined;
import td.util.TickRate;

/**
 * One enemy's tally of time frozen: it fills while the enemy is frozen and drains while it walks, and each freeze lasts
 * less the fuller it is. Over time the enemy can't stay frozen longer than {@code drain / (1 + drain)} of the time.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class FreezeMeter {

    private static final float FILL_PER_TICK = 1f / TickRate.TICKS_PER_SECOND;

    private final float drainPerTick;
    private float level;

    /** A meter that drains {@code drainPerSecond} for every second the enemy walks. */
    public FreezeMeter(float drainPerSecond) {
        this.drainPerTick = drainPerSecond / TickRate.TICKS_PER_SECOND;
    }

    /** One tick of the enemy being frozen, or walking, or neither: stopped by something else. */
    public void tick(boolean frozen, boolean walking) {
        if (frozen) {
            this.level += FILL_PER_TICK;
        } else if (walking) {
            this.level = Math.max(0f, this.level - this.drainPerTick);
        }
    }

    /** The share of its authored time the next freeze lasts. */
    public float nextShare() {
        return 1f / (1f + this.level);
    }

    public boolean isRunning() {
        return this.level > 0f;
    }
}
