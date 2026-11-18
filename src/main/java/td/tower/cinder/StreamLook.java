package td.tower.cinder;

/**
 * How its stream is drawn: in Soulfire, with a white core, as Dragon's Breath, and the half-width of its Core in
 * radians, 0 for none.
 */
public record StreamLook(boolean soulfire, boolean whiteCore, boolean dragonsBreath, float coreHalfWidth) {
}
