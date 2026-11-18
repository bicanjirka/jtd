package td.ui.render;

/** How a tower is drawn, keeping the frame model AWT-free; {@code td.ui.TowerLooks} and
 * {@code td.ui.TowerFoundations} hold each one's art. A fork or a pick that changes what a tower is has a look of its own. */
public enum TowerLook {
    SNIPER,
    SPLASH,
    /** A Splash forked to Arc. */
    STORMCALLER,
    /** A Splash forked to Hex. */
    HEXER,
    PULSE,
    /** A Pulse whose stone carries the rune of a bar over a chevron. */
    BURDEN,
    /** A Pulse whose stone carries the rune of a spiral drawn inward. */
    GRAVITY,
    /** A Pulse whose stone carries the rune of an eclipsed disc. */
    UMBRA,
    AURA,
    MORTAR,
    SEEKER,
    /** A Seeker of red crystal. */
    RUBY,
    /** A Seeker of smoky dark crystal with a pale green light. */
    SHADE,
    /** A Seeker as a white marble tower: a look kept for an identity not made yet. */
    WHITE,
    CINDER,
    /** A Cinder of dark metal burning violet. */
    SOULFIRE
}
