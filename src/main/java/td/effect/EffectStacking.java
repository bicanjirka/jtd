package td.effect;

/** How applications of a kind combine: every kind is in exactly one of these. */
public enum EffectStacking {
    /** One timer and no stacks; a reapplication keeps the stronger and the longer, and it ends at once. */
    TIMED,
    /**
     * One shared count on the enemy: applications add up to the cap, and while anything holds it (a pool, a field, an
     * aura, an application's own hold) it loses nothing; once nothing does, it loses one stack each fade step.
     */
    COUNT,
    /**
     * One share per source, each on its own clock and worth its own stacks; the count is their sum up to the cap. A
     * recast from the same source refreshes its own share.
     */
    SHARES,
    /** A decaying fuel pool (burn, poison, Soulfire). */
    POOL,
    /** One dose whose damage halves as it runs: applications add their rates, and its time follows the rate. */
    DOSE
}
