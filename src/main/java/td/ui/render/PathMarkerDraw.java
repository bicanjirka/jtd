package td.ui.render;

import td.wave.PathColor;

/**
 * One path marker in the path's {@code color}, dimmed or brightened by {@code brightness}. Every
 * marker has a facing, so any shape can be direction-sensitive.
 */
public record PathMarkerDraw(PathMarkerShape shape, PathMarkerBrightness brightness, PathColor color, float x,
                             float y, double facingRadians, float size) {
}
