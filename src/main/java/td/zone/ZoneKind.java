package td.zone;

/** What a patch of ground does to the enemies standing on it. */
public enum ZoneKind {
    /** Sets whatever stands on it alight. */
    BURNING_GROUND,
    /** Slows and poisons, and leaves an enemy tarred. */
    TAR,
    /** Chills what stands on it, and freezes what stays. */
    FROST_GROUND
}
