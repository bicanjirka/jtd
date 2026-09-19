package td.ui.render;

import td.wave.PathColor;

/**
 * One marker along a path - either the static trail or a moving indicator, distinguished by
 * {@code brightness}/{@code shape}. {@code color} is that path's own on-board hue;
 * {@code brightness} names only the dim-trail-vs-bright-chevron role the two layers have always
 * had, combined with {@code color} at paint time rather than each layer owning one fixed hue.
 * {@code facingRadians} is set for every marker regardless of shape, so a static dot can be
 * swapped for a direction-sensitive symbol without touching this model (see
 * {@link PathMarkerShape}).
 */
public record PathMarkerDraw(PathMarkerShape shape, PathMarkerBrightness brightness, PathColor color, float x,
                             float y, double facingRadians, float size) {
}
