package td.ui.render;

/**
 * Something a tower holds that shows on it for as long as it is held. Cues combine: each sits on its own part, so a
 * tower holding several shows them all. A look that has no part for a cue ignores it.
 */
public enum TowerCue {
    /** A longer barrel or nozzle: the one thing more reach changes on a head. */
    LONG_BARREL,
    /** A silver front third on the barrel. */
    SILVER_ROUND,
    /** An orange band round the middle of the barrel. */
    HOLLOW_POINT,
    /** A white diamond in a white glow at the muzzle. */
    FOURTH_SHOT,
    /** The barrel shaken sideways, red sparks at the breech. */
    FRENZY,
    /** A ring round the lens and a line through it. */
    HEADHUNTER,
    /** A store that is full: the part that holds it glows steadily and turns pale. */
    CHARGED,
    /** A second tray of shells behind the barrel. */
    VOLLEY,
    /** A stream pouring: a white-hot tip, no pilot flame, glowing vents. */
    STREAMING,
    /** A longer, thinner nozzle. */
    DRAGONS_BREATH,
    /** A release that waits: the ring and the perch brighten. */
    RELEASE_READY,
    /** A whiter light. */
    DAYLIGHT
}
