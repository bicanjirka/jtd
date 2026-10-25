package td.ui;

import td.economy.StatusTerm;
import td.util.ThreadConfined;

import java.util.EnumMap;
import java.util.Map;

/**
 * How brightly each status icon flashes now. Lives flash when they drop, the round when it goes up, and cash when a
 * purchase is refused for want of it; a change in cash or score never flashes, since they change several times a
 * second late in a wave. Values set directly (a level starting or loading) are a new baseline, never a change. The
 * clock is passed in, so nothing here reads one.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class StatusFlash {

    static final long DURATION_MS = 300;

    private final Map<StatusTerm, Long> startedAt = new EnumMap<>(StatusTerm.class);
    private StatusReadout last = null;

    /** The next readout is a baseline: whatever it holds is not a change. */
    void reset() {
        this.last = null;
        this.startedAt.clear();
    }

    /** Notes {@code now}, starting the flash of whatever it changed since the last readout. */
    void observe(StatusReadout now, long nowMillis) {
        if (this.last != null) {
            if (now.lives() < this.last.lives()) {
                this.startedAt.put(StatusTerm.LIVES, nowMillis);
            }
            if (now.round() > this.last.round()) {
                this.startedAt.put(StatusTerm.WAVE, nowMillis);
            }
        }
        this.last = now;
    }

    /** Starts the cash flash: something the player tried to buy cost more than they have. */
    void refused(long nowMillis) {
        this.startedAt.put(StatusTerm.CASH, nowMillis);
    }

    /** From 1 the moment it changed, fading evenly to 0 after {@value #DURATION_MS} ms. */
    float strength(StatusTerm term, long nowMillis) {
        Long started = this.startedAt.get(term);
        if (started == null) {
            return 0f;
        }
        return Math.clamp(1f - (nowMillis - started) / (float) DURATION_MS, 0f, 1f);
    }

    /** Whether any icon is still flashing, so the bar knows to keep repainting. */
    boolean active(long nowMillis) {
        return this.startedAt.keySet().stream().anyMatch(term -> this.strength(term, nowMillis) > 0f);
    }
}
