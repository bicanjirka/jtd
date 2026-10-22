package td.zone;

/** What a patch of ground does to the enemies, or the towers, within it. */
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
    CURSED_CLOUD,
    /** Heals what stands on it. */
    HEALING_GROUND,
    /** Speeds up what stands on it and keeps slows off it. */
    HASTE_GROUND,
    /** Goes off as a shield on the first enemy to step on it, once it is armed. */
    WARD_MINE,
    /** Makes the towers inside it miss; it works on towers, not on enemies. */
    BLINDING_FIELD
}
