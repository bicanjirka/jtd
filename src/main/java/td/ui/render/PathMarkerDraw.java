package td.ui.render;

/**
 * One marker along the path - either the static trail or a moving indicator, distinguished by
 * {@code palette}/{@code shape}. {@code facingRadians} is set for every marker regardless of
 * shape, so a static dot can be swapped for a direction-sensitive symbol without touching this
 * model (see {@link PathMarkerShape}).
 */
public record PathMarkerDraw(PathMarkerShape shape, Palette palette, float x, float y,
                              double facingRadians, float size) {
}
