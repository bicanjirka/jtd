package td.ui;

import td.util.ThreadConfined;

import java.util.HashMap;
import java.util.Map;

/**
 * When each round's skull cracked, and how far its crack has played. A skull cracks when its round goes over; a strip
 * set directly (a level starting or loading) is a new baseline, never a change. The clock is passed in, so nothing
 * here reads one.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class RoundCracks {

    static final long DURATION_MS = 700;

    private final Map<Integer, Long> startedAt = new HashMap<>();
    private RoundStrip last = null;

    /** The next strip is a baseline: whatever it holds is not a change. */
    void reset() {
        this.last = null;
        this.startedAt.clear();
    }

    /** Notes {@code now}, cracking every round it shows over that the last strip showed still to play. */
    void observe(RoundStrip now, long nowMillis) {
        if (this.last != null && this.last.count() == now.count()) {
            for (int round = 0; round < now.count(); round++) {
                if (now.stateAt(round).isOver() && !this.last.stateAt(round).isOver()) {
                    this.startedAt.put(round, nowMillis);
                }
            }
        }
        this.last = now;
    }

    /** From 0 the moment round {@code round} cracked to 1 after {@value #DURATION_MS} ms; 1 for a skull never seen crack. */
    float progress(int round, long nowMillis) {
        Long started = this.startedAt.get(round);
        if (started == null) {
            return 1f;
        }
        return Math.clamp((nowMillis - started) / (float) DURATION_MS, 0f, 1f);
    }

    /** Whether any skull is still cracking, so the strip knows to keep repainting. */
    boolean active(long nowMillis) {
        return this.startedAt.keySet().stream().anyMatch(round -> this.progress(round, nowMillis) < 1f);
    }
}
