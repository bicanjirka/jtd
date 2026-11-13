package td.ui.render;

/** How a tower is drawn, keeping the frame model AWT-free; {@code td.ui.TowerLooks} holds each one's art. */
public enum TowerLook {
    SNIPER,
    SPLASH,
    /** A Splash forked to Arc. */
    STORMCALLER,
    /** A Splash forked to Hex. */
    HEXER,
    PULSE,
    AURA,
    MORTAR,
    SEEKER,
    CINDER
}
