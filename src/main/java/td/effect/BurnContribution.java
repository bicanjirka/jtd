package td.effect;

/**
 * One tower's share of a burn pool: {@code amount} decays with the pool, and {@code sink} credits
 * that tower for its share.
 */
record BurnContribution(DamageSink sink, float amount) {

    BurnContribution decayedBy(float alpha) {
        return new BurnContribution(this.sink, this.amount * alpha);
    }
}
