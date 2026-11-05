package td.effect;

/** How applications of a kind combine. */
public enum EffectStacking {
    /** One live effect; a reapplication keeps the stronger and the longer. */
    REFRESH,
    /** Stacks up to a cap on one clock every application refreshes. */
    SHARED_CLOCK,
    /** Stacks that wear off one at a time. */
    EARNED,
    /** A decaying fuel pool (burn, poison, Soulfire). */
    POOL,
    /** One share per source that add up; a recast from the same source refreshes its own share. */
    PER_SOURCE
}
