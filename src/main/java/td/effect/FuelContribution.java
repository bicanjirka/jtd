package td.effect;

/**
 * One tower's share of a pool (burn, poison or Soulfire): {@code amount} decays with the pool, and {@code sink}
 * credits that tower for its share.
 */
record FuelContribution(DamageSink sink, float amount) {

    FuelContribution decayedBy(float alpha) {
        return new FuelContribution(this.sink, this.amount * alpha);
    }
}
