package td.effect;

/** A yes/no a kind carries, read by the rules that ask for it. */
public enum EffectTag {
    /** Stops the enemy where it stands and keeps it from casting. */
    STOPS,
    /** A fire: the pools a freeze puts out and a chill cools. */
    BURNING,
    /** A curse carries it on to the next enemy when its carrier dies. */
    SPREADS_WITH_CURSE,
    /** A Sympathy carrier shares it with the hexed enemies near it. */
    SHARED_BY_SYMPATHY,
    /** A tower's field holds it on the enemies inside, refreshing it every tick. */
    FIELD
}
