package td.effect;

/** A yes/no a kind carries, read by the rules that ask for it. */
public enum EffectTag {
    /** Stops the enemy where it stands and keeps it from casting. */
    STOPS,
    /** Not renewed while it holds: one renewed before it ended would never end. */
    NOT_RENEWED,
    /** A fire: a pool that reveals its carrier. */
    BURNING,
    /** A curse carries it on to the next enemy when its carrier dies. */
    SPREADS_WITH_CURSE,
    /** A Sympathy carrier shares it with the hexed enemies near it. */
    SHARED_BY_SYMPATHY
}
