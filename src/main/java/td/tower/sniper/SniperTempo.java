package td.tower.sniper;

import td.util.ThreadConfined;

/**
 * The Sniper's bursts of speed. Frenzy halves the next few waits between shots, a Momentum burst
 * halves every wait for a few seconds. Neither stacks with itself or the other: a burst replaces a
 * running Frenzy, and a Frenzy never starts over a running burst.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SniperTempo {

    private static final int FRENZY_WAITS = 3;
    /** The share of a wait that twice as fast takes away. */
    private static final float DOUBLE_SPEED = 0.5f;

    private int frenzyWaitsLeft;
    private int burstEndsAt = -1;

    /** Starts a Frenzy unless one is running or a burst is. */
    public void startFrenzy(int tick) {
        if (this.frenzyWaitsLeft == 0 && tick >= this.burstEndsAt) {
            this.frenzyWaitsLeft = FRENZY_WAITS;
        }
    }

    /** Starts a burst until {@code endsAt}, ending any Frenzy and restarting a running burst. */
    public void startBurst(int endsAt) {
        this.frenzyWaitsLeft = 0;
        this.burstEndsAt = endsAt;
    }

    /**
     * The extra fire rate for the wait that starts at {@code tick}. A Frenzy wait is used up by
     * asking.
     */
    public float takeFireRateBonus(int tick) {
        if (tick < this.burstEndsAt) {
            return DOUBLE_SPEED;
        }
        if (this.frenzyWaitsLeft > 0) {
            this.frenzyWaitsLeft--;
            return DOUBLE_SPEED;
        }
        return 0f;
    }
}
