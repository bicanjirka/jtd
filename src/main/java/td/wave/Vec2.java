package td.wave;

/**
 * A continuous pixel-space point. Distinct from {@link Point}, which holds grid/cell
 * coordinates: after the path-smoothing refactor, {@code Point} is used only for level
 * authoring (cell indices), and {@code Vec2} is used only for the continuous pixel positions
 * a {@link Path} actually stores and enemies move through.
 */
public record Vec2(double x, double y) {
}
