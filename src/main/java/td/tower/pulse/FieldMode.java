package td.tower.pulse;

/** A rule that holds while an enemy is inside the field. */
public enum FieldMode {
    /** Every enemy inside is Silenced. */
    SILENCE,
    /** Nothing inside can be healed or shielded. */
    DEAD_ZONE,
    /** Armor is lowered inside. */
    CORROSION,
    /** What a shield absorbs of the field's hits is dealt back to the enemy. */
    MIRROR,
    /** Enemies inside are chilled and Anchored. */
    UNDERTOW,
    /** Enemies inside take extra damage from every source. */
    KILL_ZONE,
    /** Each second inside costs spirit, and lost spirit makes the field hit harder. */
    SOUL_DRAIN,
    /** Each tick has a chance to add a Sundered or Exposed. */
    RATTLE
}
