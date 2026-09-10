package td.wave;

/**
 * An integer grid-cell coordinate. Used only for level authoring (a level's path corners) and
 * for {@link PathCoverage}'s result - the continuous pixel positions a {@link Path} stores and
 * enemies move through are {@link Vec2}, and the two are deliberately not interchangeable.
 * <p>
 * A corner may sit outside the board (the built-in levels start at {@code x = -1} so enemies
 * walk in from off-screen), so this is not validated against board bounds.
 */
public record Point(int x, int y) {
}
