package td.effect;

/** What paces a kind's timer or its stacks' decay. */
public enum EffectPacing {
    /** One tick per tick. */
    OWN,
    /** The enemy's spirit pace, never below the debuff floor, stopped at spirit -100. */
    SPIRIT,
    /** The enemy's spirit pace with no floor (a pool's lasting marks). */
    SPIRIT_UNFLOORED
}
