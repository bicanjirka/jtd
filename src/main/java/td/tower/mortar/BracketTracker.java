package td.tower.mortar;

import td.util.ThreadConfined;

import java.util.Optional;

/**
 * Where the last shell landed and how many shells in a row have bracketed it. Only the game loop
 * lands shells; the frame build reads the marker.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class BracketTracker {

    private volatile Marker marker;

    /** Where a ranging marker is drawn: the last landing, and how tight the bracket is. */
    public record Marker(double x, double y, int step, int maxSteps) {
    }

    /**
     * Lands a shell at ({@code x}, {@code y}) and tells which step it is on: one more than the last
     * if it landed within the bracket's radius of the last shell, else none. Always remembers it.
     */
    public int land(double x, double y, BracketSpec spec, int cellSize) {
        if (!spec.active()) {
            this.marker = null;
            return 0;
        }
        Marker last = this.marker;
        int step = 0;
        if (last != null) {
            double reach = spec.radiusCells() * cellSize;
            double dx = x - last.x();
            double dy = y - last.y();
            if (dx * dx + dy * dy <= reach * reach) {
                step = Math.min(spec.maxSteps(), last.step() + 1);
            }
        }
        this.marker = new Marker(x, y, step, spec.maxSteps());
        return step;
    }

    /** The marker of the last landing; empty before the first shell or when the Mortar does not bracket. */
    public Optional<Marker> marker() {
        return Optional.ofNullable(this.marker);
    }
}
