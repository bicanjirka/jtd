package td.ui.render;

/**
 * A path marker's brightness role, dim trail or bright moving marker. Separate from {@link Palette}
 * because the hue is authored per path.
 */
public enum PathMarkerBrightness {
    STATIC,
    MOVING
}
