package td.ui.render;

/**
 * A path marker's symbol. A new one needs a case in the renderer's shape switch. None is round: a round mark on the
 * ground means an area that acts.
 */
public enum PathMarkerShape {
    /** A short stroke along the path. */
    DASH,
    CHEVRON
}
