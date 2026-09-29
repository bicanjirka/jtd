package td.effect;

/**
 * One tower's share of a decaying level (burn, poison or chill): {@code amount} decays with the
 * level, and {@code sink} credits that tower for its share. {@code decayPerTick} is a chill's linear
 * decay; a pool decays exponentially and leaves it {@code 0}.
 */
record FuelContribution(DamageSink sink, float amount, float decayPerTick) {

    FuelContribution(DamageSink sink, float amount) {
        this(sink, amount, 0f);
    }

    FuelContribution decayedBy(float alpha) {
        return new FuelContribution(this.sink, this.amount * alpha, this.decayPerTick);
    }

    /** One tick further along a linear decay; may reach zero. */
    FuelContribution decayedLinearly() {
        return new FuelContribution(this.sink, Math.max(0f, this.amount - this.decayPerTick), this.decayPerTick);
    }

    /** {@code share} of this contribution, decaying at the same rate per unit. */
    FuelContribution scaledTo(float newAmount) {
        return new FuelContribution(this.sink, newAmount, this.amount == 0f ? 0f : this.decayPerTick * newAmount / this.amount);
    }
}
