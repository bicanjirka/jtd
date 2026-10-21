package td.ui;

import java.awt.Point;
import java.util.OptionalInt;

/**
 * Where an icon row puts its faces, and which face a point is on. Pure.
 *
 * @param count      how many faces
 * @param perLine    how many fit on a line
 * @param timeLabels whether every line leaves room for the time label under a face
 */
record IconRowLayout(int count, int perLine, boolean timeLabels) {

    static final int LABEL_WIDTH = 48;
    static final int GAP = 2;
    static final int LINE_GAP = 3;

    /** {@code count} faces beside the label in {@code width} px; with {@code timeLabels}, every line leaves room for one. */
    static IconRowLayout of(int count, boolean timeLabels, int width) {
        int perLine = Math.max(1, (width - LABEL_WIDTH + GAP) / (TermIconPainter.ROW + GAP));
        return new IconRowLayout(count, perLine, timeLabels);
    }

    int lines() {
        return Math.max(1, (this.count + this.perLine - 1) / this.perLine);
    }

    int lineHeight() {
        return TermIconPainter.ROW + (this.timeLabels ? TermIconPainter.LABEL : 0) + LINE_GAP;
    }

    int height() {
        return this.lines() * this.lineHeight() - LINE_GAP;
    }

    /** The top-left corner of face {@code index}. */
    Point cell(int index) {
        return new Point(LABEL_WIDTH + (index % this.perLine) * (TermIconPainter.ROW + GAP),
                (index / this.perLine) * this.lineHeight());
    }

    /** The face under {@code (x, y)}, relative to the row's top-left; empty between faces or past the last. */
    OptionalInt iconAt(int x, int y) {
        for (int i = 0; i < this.count; i++) {
            Point cell = this.cell(i);
            if (x >= cell.x && x < cell.x + TermIconPainter.ROW && y >= cell.y && y < cell.y + TermIconPainter.ROW) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
}
