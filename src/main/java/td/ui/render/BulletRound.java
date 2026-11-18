package td.ui.render;

/** What a Sniper's bullet is, which is how it is drawn; a ricochet is drawn as the bullet that made it. */
public enum BulletRound {
    /** A plain shot: thin, no glow. */
    PLAIN,
    /** A crit: white. */
    CRIT,
    /** The Fourth Shot, a sure crit: bold and red round a white-hot core, with a wide glow. */
    FOURTH_SHOT,
    /** Hollow Point: bold copper with a hot head, a warm glow. */
    HOLLOW_POINT,
    /** Silver Round: bold silver-blue round a white core, a cold glow and glitter left behind. */
    SILVER_ROUND
}
