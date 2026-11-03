package td.board;

/** The closed set of patterns a board's floor can carry; the renderer owns how each one is drawn. */
public enum GroundMotif {
    NONE,
    /** A honeycomb of hexagons. */
    COMB,
    /** Slanting streaks of rain. */
    RAIN,
    /** Rippled lines, like wind-blown sand. */
    DUNES
}
