package td.tower;

/** What kind of behaviour a {@link BehaviourLine} describes, which picks its glyph and colour. */
public enum BehaviourMarker {
    /** Who the tower attacks. */
    TARGETING,
    CHILL,
    BURN,
    FREEZE,
    VULNERABLE,
    REVEAL,
    POISON,
    CRACKED,
    DAZE,
    /** A bonus the tower gives others. */
    BUFF
}
