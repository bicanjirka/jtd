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
    TOWER_AURA_RING,
    TOWER_ONE_BEAM,
    TOWER_TWO_BEAM,
    TOWER_TWO_SPLASH_LINE,
    TOWER_TWO_SPLASH_FILL,
    TOWER_THREE_BEAM,
    TOWER_FOUR_PULSE,
    PATH_MARKER_STATIC,
    PATH_MARKER_MOVING
}
