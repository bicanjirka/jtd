package td.tower;

/** A number a tower's info rows show, each with its own unit. */
public enum TowerStat {
    /** In cells. */
    RANGE,
    /** Points per hit. */
    PHYSICAL_DAMAGE,
    /** Points per hit. */
    MAGIC_DAMAGE,
    /** Attacks per second. */
    FIRE_RATE,
    /** Seconds per turn of a sweeping head. */
    ROTATION,
    /** A fraction in {@code [0, 1]}. */
    CRIT_CHANCE,
    /** In cells. */
    SPLASH_RADIUS
}
