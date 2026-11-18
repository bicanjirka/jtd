package td.tower.splash;

import td.tower.ShotFlight;
import td.wave.Vec2;

/**
 * A soul or a spark flying home with what its passive pays.
 *
 * @param passive Soulbrand for a soul, Static for a spark
 */
public record Wisp(Vec2 from, Passive passive, ShotFlight flight) {
}
