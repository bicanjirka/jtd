package td.save;

import td.wave.Point;

/** An Obelisk's Ley Line the player pinned: the cell of the Obelisk that casts it and of the one it is pinned to. */
public record LeyLinePin(Point caster, Point host) {
}
