package td.zone;

/**
 * What one pulse of a zone does to an enemy inside, in numbers; which effects carry them is the zone's kind.
 *
 * @param strength a pool's fuel, a direct hit's magic damage or a heal a tick, in damage units, as the kind takes it;
 *                 0 for none
 * @param slow     the slow each pulse holds on, for a kind that slows
 * @param freeze   the freeze an enemy that stays gets, for any kind
 */
public record ZonePulse(int strength, Slow slow, Freeze freeze) {

    /** Every zone pulses this often. */
    public static final int PULSE_TICKS = ZoneRoster.PULSE_TICKS;

    /** A pulse of {@code strength} that slows and freezes nothing. */
    public static ZonePulse of(int strength) {
        return new ZonePulse(strength, Slow.none(), Freeze.none());
    }

    public ZonePulse withSlow(Slow held) {
        return new ZonePulse(this.strength, held, this.freeze);
    }

    public ZonePulse withFreeze(Freeze given) {
        return new ZonePulse(this.strength, this.slow, given);
    }

    /**
     * {@code share} of this pulse, put on an enemy by something that is not the zone: its strength scaled, its slow
     * whole, and no freeze, which only staying earns.
     */
    public ZonePulse shareOf(float share) {
        return new ZonePulse(Math.round(this.strength * share), this.slow, Freeze.none());
    }

    /**
     * A slow each pulse puts on, held a while past the pulse.
     *
     * @param strength the share of speed it takes
     */
    public record Slow(float strength, int holdTicks) {

        public static Slow none() {
            return new Slow(0f, 0);
        }

        public boolean slows() {
            return this.strength > 0f && this.holdTicks > 0;
        }
    }

    /**
     * A freeze for an enemy that stays {@code pulses} pulses in a row inside; while frozen it counts none.
     *
     * @param ticks how long the freeze holds
     */
    public record Freeze(int pulses, int ticks) {

        public static Freeze none() {
            return new Freeze(0, 0);
        }

        public boolean freezes() {
            return this.pulses > 0 && this.ticks > 0;
        }

        /** How long an enemy stays inside before it freezes: the first pulse lands as it enters. */
        public int ticksInside() {
            return (this.pulses - 1) * PULSE_TICKS;
        }
    }
}
