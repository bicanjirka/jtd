package td.ui.render;

/**
 * Names a draw command's colour role without committing to an actual
 * {@code java.awt.Color} - the frame model stays AWT-free, and the single
 * role-to-colour mapping lives in the one backend that needs it
 * ({@link td.ui.Java2DFrameRenderer}).
 */
public enum Palette {
    ENEMY_CIRCLE,
    ENEMY_GHOST,
    ENEMY_SQUARE,
    ENEMY_TRIANGLE,
    TOWER_ONE_BODY,
    TOWER_TWO_BODY,
    TOWER_THREE_BODY,
    TOWER_FOUR_BODY,
    TOWER_AURA_BODY,
    TOWER_MORTAR_BODY,
    TOWER_SEEKER_BODY,
    TOWER_CINDER_BODY,
    TOWER_AURA_RING,
    /** The specialization-ring accent for a tower's first vs. second upgrade path - one shared pair of roles, not one per tower type. */
    TOWER_UPGRADE_PATH_A,
    TOWER_UPGRADE_PATH_B,
    TOWER_ONE_BEAM,
    TOWER_TWO_BEAM,
    TOWER_TWO_SPLASH_LINE,
    TOWER_TWO_SPLASH_FILL,
    TOWER_THREE_BEAM,
    TOWER_FOUR_PULSE,
    TOWER_CINDER_CONE,
    PROJECTILE_CANNONBALL,
    PROJECTILE_MISSILE,
    /** A small on-board marker naming which status effect is currently active on a mob. */
    STATUS_MARKER_SLOW,
    STATUS_MARKER_BURN,
    STATUS_MARKER_FREEZE,
    PATH_MARKER_STATIC,
    PATH_MARKER_MOVING
}
