package td.tower;

/**
 * When a shot flies: it leaves on one tick and lands on a later one.
 *
 * @param firedAt the tick it left
 * @param landsAt the tick it arrives
 */
public record ShotFlight(int firedAt, int landsAt) {
}
