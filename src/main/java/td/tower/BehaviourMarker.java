package td.tower;

/** What kind of behaviour a {@link BehaviourLine} describes, which picks its glyph and colour. */
public enum BehaviourMarker {
    /** Who the tower attacks. */
    TARGETING,
    SLOW,
    BURN,
    FREEZE,
    /** A bonus the tower gives others. */
    BUFF
}
