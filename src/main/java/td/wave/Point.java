package td.wave;

/**
 * A grid-cell coordinate, for authoring and cell sets; pixel positions are {@link Vec2}. Not
 * bounded to the board: corners may sit off-screen so enemies walk in.
 */
public record Point(int x, int y) {
}
