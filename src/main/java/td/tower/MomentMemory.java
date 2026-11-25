package td.tower;

import td.util.TickRate;

/**
 * How long a tower keeps a moment it made (a blast, a bolt, a shatter, a reveal) for the frame build to draw. How
 * long each moment is drawn is the presentation's choice, within this window; nothing in the game reads a kept moment.
 */
public final class MomentMemory {

    /** Long enough for the longest moment the board draws. */
    public static final int TICKS = Math.round(TickRate.TICKS_PER_SECOND * 3f);

    private MomentMemory() {
    }

    /** Whether a moment made at {@code madeAt} is past keeping at {@code now}. */
    static boolean forgets(long madeAt, long now) {
        return now - madeAt >= TICKS;
    }
}
