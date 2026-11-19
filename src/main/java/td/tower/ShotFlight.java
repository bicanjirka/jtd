package td.tower;

/**
 * When a shot flies: it leaves on one tick and lands on a later one.
 *
 * @param firedAt the tick it left
 * @param landsAt the tick it arrives
 */
public record ShotFlight(int firedAt, int landsAt) {

    /** A flight that leaves at {@code now} and takes {@code ticks}. */
    public static ShotFlight lasting(int now, int ticks) {
        return new ShotFlight(now, now + ticks);
    }

    /** A flight that leaves at {@code now} to cover {@code pixels} at {@code pixelsPerTick}, never under {@code minTicks}. */
    public static ShotFlight across(int now, double pixels, double pixelsPerTick, int minTicks) {
        return lasting(now, Math.max(minTicks, (int) Math.ceil(pixels / pixelsPerTick)));
    }
}
