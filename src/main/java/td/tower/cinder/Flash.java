package td.tower.cinder;

/** A Flashover from a hot enemy at {@code fromX, fromY} to one beside it, in board pixels. */
public record Flash(double fromX, double fromY, double toX, double toY) {
}
