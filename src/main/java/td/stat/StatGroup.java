package td.stat;

/** Where a stat's row sits on a sheet: groups in this order, a gap between two, rows within one as listed. */
public enum StatGroup {
    REACH,
    DAMAGE,
    CRIT,
    EFFECTS,
    DELIVERY,
    ECONOMY,
    /** What a tower hands to the towers around it. */
    GIVES,
    SPEED,
    DEFENCE,
    DAMAGE_TAKEN,
    HEALING,
    RESISTS
}
