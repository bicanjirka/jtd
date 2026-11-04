package td.ui.render;

/**
 * A stretch of a patch's outline: part of a disc's rim. Angles are in radians on the board, where y grows downward, so
 * a positive sweep turns from +x toward +y.
 */
public record ZoneArcDraw(float centerX, float centerY, float radius, float startRadians, float sweepRadians) {
}
