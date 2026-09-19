package td.ui.render;

/**
 * The alpha role a path marker plays - dim trail or bright moving indicator - independent of
 * which path's color it is drawn in. Split out of {@link Palette} rather than kept as two of its
 * constants: every other {@code Palette} role names a fixed color, but a path's own
 * {@link td.wave.PathColor} is authored data, so "which brightness" and "which hue" are two
 * different axes here instead of one.
 */
public enum PathMarkerBrightness {
    STATIC,
    MOVING
}
