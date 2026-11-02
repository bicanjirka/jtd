package td.ui;

import java.awt.Dimension;

/**
 * How many screen pixels the board draws for each of its own. The simulation keeps its own pixels;
 * only the view zooms, so the board fits its window without touching ranges, speeds or paths.
 */
public record BoardZoom(double factor) {

    /** Below this a board is clipped rather than shrunk further: its glyphs stop being readable. */
    static final double MIN_FACTOR = 0.5;

    private static final BoardZoom NATURAL = new BoardZoom(1);
    private static final BoardZoom SMALLEST = new BoardZoom(MIN_FACTOR);

    public static BoardZoom natural() {
        return NATURAL;
    }

    public static BoardZoom smallest() {
        return SMALLEST;
    }

    /** The largest zoom at which {@code board} pixels fit {@code room}, keeping the board's proportions. */
    public static BoardZoom fit(Dimension board, Dimension room) {
        if (board.width <= 0 || board.height <= 0) {
            return NATURAL;
        }
        double factor = Math.min((double) room.width / board.width, (double) room.height / board.height);
        return new BoardZoom(Math.max(MIN_FACTOR, factor));
    }

    /** {@code boardLength} board pixels on screen, rounded down so the board never spills out of its room. */
    public int toView(int boardLength) {
        return (int) (boardLength * this.factor);
    }

    /** The board pixel under screen offset {@code viewOffset}. */
    public int toBoard(int viewOffset) {
        return (int) Math.floor(viewOffset / this.factor);
    }
}
