package td.ui.render;

/**
 * A small on-board indicator that one status effect is currently active on a mob - not a
 * permitted {@link EnemyDraw} subtype, since a mob is either an alive body or a fading corpse
 * (never both), and a marker is neither of those on its own. Positioned by whoever builds it
 * (see {@code EnemyFrameBuilder.buildMarkers}), not tied to any one enemy shape.
 */
public record StatusMarkerDraw(Palette palette, float x, float y, float scale) {
}
