package td.tower.mortar;

/**
 * Whom a shell's impact stuns: everything within {@code radiusCells} of where it lands, for
 * {@code ticks}.
 */
public record DazeSpec(int ticks, float radiusCells) {

    public static DazeSpec none() {
        return new DazeSpec(0, 0f);
    }

    public static DazeSpec of(int ticks, float radiusCells) {
        return new DazeSpec(ticks, radiusCells);
    }

    public boolean isActive() {
        return this.ticks > 0;
    }
}
