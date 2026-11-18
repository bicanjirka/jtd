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
    /** A white core in a red glow at the muzzle. */
    FOURTH_SHOT,
    /** The head shaken about, a step of the pose's beat at a time, red sparks off it. */
    FRENZY,
    /** A focus at its fullest that a node sharpens: the lens a golden eye. */
    KEEN_FOCUS,
    /** A ring round the lens and a line through it. */
    HEADHUNTER,
    /** A store that is full: the part that holds it glows steadily and turns pale. */
    CHARGED,
    /** Three shells loaded for one firing, one at each quarter of the reload. */
    VOLLEY,
    /** A stream pouring: a white-hot tip, no pilot flame, glowing vents. */
    STREAMING,
    /** A dragon's head for a nozzle. */
    DRAGONS_BREATH,
    /** A release that waits: the tip turns white and glows. */
    RELEASE_READY,
    /** A whiter light. */
    DAYLIGHT
}
