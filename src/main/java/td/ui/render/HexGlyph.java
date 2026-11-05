package td.ui.render;

/** The rune a hex is drawn as over the enemy carrying it: its shape says what the hex does. */
public enum HexGlyph {
    /** An hourglass: the damage it stores runs out on the enemy when the time is up. */
    DOOM,
    /** A drop: poison. */
    BLIGHT,
    /** Three linked dots: it spreads to its neighbours. */
    CONTAGION,
    /** A star of three strokes: what ails it doesn't let go. */
    MALAISE,
    /** A triangle turned on its head: what helps it hurts it. */
    INVERSION,
    /** Two linked rings: what one suffers, all suffer. */
    SYMPATHY,
    /** A ring with spokes: a burst when it dies. */
    RECKONING
}
