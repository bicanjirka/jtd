package td.ui.render;

/**
 * A draw command's colour role, keeping the frame model AWT-free; the backend maps roles to
 * colours.
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
    /** A Splash forked to Arc. */
    TOWER_STORMCALLER_BODY,
    /** A Splash forked to Hex. */
    TOWER_HEXER_BODY,
    TOWER_SONAR_BODY,
    TOWER_PULSE_BODY,
    TOWER_AURA_BODY,
    TOWER_MORTAR_BODY,
    TOWER_SEEKER_BODY,
    TOWER_CINDER_BODY,
    TOWER_AURA_RING,
    /** A faint line from an aura to a tower it buffs. */
    TOWER_AURA_LINK,
    /** Slot marks, one role per upgrade slot. */
    TOWER_UPGRADE_BASE,
    TOWER_UPGRADE_HEAD,
    TOWER_UPGRADE_SPECIAL,
    /** A Transcendent tower's halo ring and pip. */
    TOWER_TRANSCENDENT,
    /** A tower's rank pips, its rank-up glow and its XP row. */
    TOWER_RANK,
    TOWER_SNIPER_BEAM,
    /** The Sniper's aim line, brighter with each Steady Aim stack. */
    TOWER_SNIPER_LASER,
    /** A Silver Rounds shot. */
    TOWER_SNIPER_SILVER,
    /** A Transcendent Sniper's tracer. */
    TOWER_SNIPER_TRACER,
    /** A Transcendent Sniper's barrel. */
    TOWER_SNIPER_GOLD_BARREL,
    TOWER_SPLASH_BEAM,
    /** A blast's first flash. */
    TOWER_SPLASH_FLASH,
    /** A blast's ring as it spreads, bright... */
    TOWER_SPLASH_DETONATION,
    /** ...and darkening as it grows. */
    TOWER_SPLASH_DETONATION_DARK,
    /** The Stormcaller's arcs. */
    TOWER_SPLASH_ARC,
    /** A Hexer's cast, from the tower to each enemy it curses. */
    TOWER_SPLASH_CAST,
    /** The rune over an enemy carrying a hex. */
    HEX_RUNE,
    TOWER_SONAR_BEAM,
    /** A Sonar hit once its beam deals magic. */
    TOWER_SONAR_MAGIC_BEAM,
    /** The ring a Sonar sends out each revolution. */
    TOWER_SONAR_PING,
    /** The crosshair over the enemies a Sonar pinged. */
    TOWER_SONAR_CROSSHAIR,
    TOWER_PULSE_RING,
    TOWER_PULSE_RIPPLE,
    TOWER_PULSE_RIPPLE_NULL,
    TOWER_PULSE_RIPPLE_UNDERTOW,
    TOWER_PULSE_RIPPLE_CORROSION,
    TOWER_PULSE_ZAP,
    TOWER_CINDER_CONE,
    ZONE_BURNING,
    ZONE_TAR,
    ZONE_FROST,
    PROJECTILE_CANNONBALL,
    PROJECTILE_MISSILE,
    PROJECTILE_SMOKE,
    PROJECTILE_CRYO,
    PROJECTILE_ARCANE,
    PROJECTILE_EMP,
    PROJECTILE_TRACER,
    /** Status markers, one per effect kind. */
    STATUS_MARKER_CHILL,
    STATUS_MARKER_BURN,
    STATUS_MARKER_FREEZE,
    STATUS_MARKER_DAZED,
    STATUS_MARKER_SATURATED,
    STATUS_MARKER_CHARGED,
    STATUS_MARKER_DOOM,
    STATUS_MARKER_BLIGHT,
    STATUS_MARKER_CONTAGION,
    STATUS_MARKER_RIME,
    STATUS_MARKER_ASH,
    STATUS_MARKER_INVERSION,
    STATUS_MARKER_SYMPATHY,
    STATUS_MARKER_RECKONING,
    STATUS_MARKER_SILENCED,
    STATUS_MARKER_ANCHORED,
    STATUS_MARKER_UNRAVELED,
    STATUS_MARKER_BRITTLE,
    STATUS_MARKER_TOLL,
    STATUS_MARKER_CORRODED,
    STATUS_MARKER_UNDERTOW,
    STATUS_MARKER_DEAD_ZONE,
    STATUS_MARKER_KILL_ZONE,
    STATUS_MARKER_CRACKED,
    STATUS_MARKER_TARRED,
    STATUS_MARKER_SHIELD,
    STATUS_MARKER_INVISIBLE,
    STATUS_MARKER_HEAL,
    STATUS_MARKER_VULNERABLE,
    STATUS_MARKER_REVEALED,
    STATUS_MARKER_POISON,
    STATUS_MARKER_SCORCHED,
    STATUS_MARKER_SICKENED,
    STATUS_MARKER_SUNDERED,
    STATUS_MARKER_EXPOSED,
    STATUS_MARKER_MARKED,
    STATUS_MARKER_PRIORITY,
    STATUS_MARKER_RESONATING,
    STATUS_MARKER_FRACTURED,
    /** Stands in for every effect past the visible marker cap. */
    STATUS_MARKER_OVERFLOW,
    /** Ice over a frozen enemy, whiter than the slow and freeze markers. */
    FREEZE_CRYSTAL,
    CRIT_SPARK,
    /** The ring where an ability spawn arrived. */
    SPAWN_BURST,
    /** Rank badges. The two chevron ranks share a role; star and skull each have their own. */
    RANK_BADGE_CHEVRON,
    RANK_BADGE_ELITE,
    RANK_BADGE_BOSS,
    /** Trait markers, one role per trait marker plus overflow. */
    TRAIT_MARKER_PERCENT_RESIST,
    TRAIT_MARKER_PHYSICAL_RESIST,
    TRAIT_MARKER_MAGIC_RESIST,
    TRAIT_MARKER_FLAT_RESIST,
    TRAIT_MARKER_CRITICAL_IMMUNE,
    TRAIT_MARKER_HURT_SPEED,
    TRAIT_MARKER_BURN_IMMUNE,
    TRAIT_MARKER_FREEZE_IMMUNE,
    TRAIT_MARKER_EFFECT_RESIST,
    TRAIT_MARKER_OVERFLOW,
    /** An enemy's disruption ring and the marker on a tower it weakens. */
    DISRUPTION,
    /** The ring around the enemy the player is inspecting. */
    SELECTION,
    /** Info-panel tones: a damage type, and an upgrade gate met or not. */
    DAMAGE_PHYSICAL,
    DAMAGE_MAGIC,
    UPGRADE_GATE_MET,
    UPGRADE_GATE_UNMET
}
