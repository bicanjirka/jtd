package td.stat;

/** Which way a stat's value is better for the player. */
public enum StatDirection {
    HIGHER,
    LOWER,
    /** Neither: a change is shown, never judged. */
    NEUTRAL
}
