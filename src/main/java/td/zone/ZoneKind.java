package td.zone;

/** What a patch of ground does to the enemies standing on it. */
public enum ZoneKind {
    /** Sets whatever stands on it alight. */
    BURNING_GROUND,
    /** Slows and poisons, and leaves an enemy tarred. */
    TAR,
    /** Chills what stands on it, and freezes what stays. */
    FROST_GROUND,
    /** Drains the spirit of what stands on it and keeps heals and shields from taking hold. */
    FALLOUT,
    /** Waits for the first enemy to step on it, and goes off as one blast. */
    MINE,
    /** Gives every enemy inside the debuffs the enemy that died here carried. */
    CURSED_CLOUD
}
