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
    ENEMY_WARDEN,
    ENEMY_WARDEN_EGG,
    ENEMY_MENDER,
    TOWER_SNIPER_BODY,
    TOWER_SPLASH_BODY,
    TOWER_SONAR_BODY,
    TOWER_PULSE_BODY,
    TOWER_AURA_BODY,
    TOWER_MORTAR_BODY,
    TOWER_SEEKER_BODY,
    TOWER_CINDER_BODY,
    TOWER_AURA_RING,
    /**
     * The specialization-ring accent for a tower's first vs. second upgrade path - one shared pair of roles, not one per tower type.
     */
    TOWER_UPGRADE_PATH_A,
    TOWER_UPGRADE_PATH_B,
    TOWER_SNIPER_BEAM,
    TOWER_SPLASH_BEAM,
    TOWER_SPLASH_LINE,
    TOWER_SPLASH_FILL,
    TOWER_SONAR_BEAM,
    TOWER_PULSE_RING,
    TOWER_CINDER_CONE,
    PROJECTILE_CANNONBALL,
    PROJECTILE_MISSILE,
    /**
     * A small on-board marker naming which status effect is currently active on a mob.
     */
    STATUS_MARKER_SLOW,
    STATUS_MARKER_BURN,
    STATUS_MARKER_FREEZE,
    STATUS_MARKER_SHIELD,
    STATUS_MARKER_INVISIBLE,
    STATUS_MARKER_HEAL,
    /**
     * Stands in for every effect beyond the marker row's visible cap - see EnemyFrameBuilder.MAX_VISIBLE_MARKERS.
     */
    STATUS_MARKER_OVERFLOW,
    /**
     * A brief, fading burst at the point a critical hit landed - see CritSparkDraw.
     */
    CRIT_SPARK,
    /**
     * A brief ring where an ability-driven spawn (an egg hatch, a death split, a reinforcement)
     * arrived - not tied to any EffectKind, so it gets its own role rather than reusing one.
     */
    SPAWN_BURST,
    /**
     * An enemy's rank badge - see {@link RankBadge}. One shared role for Soldier's one chevron
     * and Veteran's two, since both read as the same army-insignia language; Elite's star and
     * Boss's skull each get their own role so they can carry their own (gold, silver) colour.
     */
    RANK_BADGE_CHEVRON,
    RANK_BADGE_ELITE,
    RANK_BADGE_BOSS,
    /**
     * A small hollow glyph naming one of a mob's always-on traits - see TraitMarkerDraw. One
     * role per td.enemy.TraitMarker, plus an overflow role for the row's own cap.
     */
    TRAIT_MARKER_PERCENT_RESIST,
    TRAIT_MARKER_FLAT_RESIST,
    TRAIT_MARKER_CRITICAL_IMMUNE,
    TRAIT_MARKER_HURT_SPEED,
    TRAIT_MARKER_OVERFLOW
}
