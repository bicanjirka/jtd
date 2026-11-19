package td.tower.ability;

/**
 * One ability's store of its tower's currency. Every bar holds the same number of units however little its ability
 * costs, so a payout landing while the ability waits for a target is kept for the casts after it; past that the rest is
 * lost.
 *
 * @param cost what one cast takes, in whole units
 * @param fill what it holds now
 */
public record AbilityBar(int cost, AbilityUnits fill) {

    /** How many whole units a bar holds when full: the most any ability may cost. */
    public static final int CAPACITY = 5;
    private static final int UNITS_AT_WAVE_START = 1;

    public AbilityBar {
        if (cost < 1) {
            throw new IllegalArgumentException("An ability costs at least one unit: " + cost);
        }
        if (cost > CAPACITY) {
            throw new IllegalArgumentException("An ability costs at most what a bar holds: " + cost);
        }
        if (fill.compareTo(AbilityUnits.whole(CAPACITY)) > 0) {
            throw new IllegalArgumentException("A bar holds at most " + CAPACITY + " units: " + fill);
        }
    }

    /** The bar as every wave finds it, so the first cast comes early. */
    public static AbilityBar atWaveStart(int cost) {
        return new AbilityBar(cost, AbilityUnits.whole(UNITS_AT_WAVE_START));
    }

    /** How many whole units it holds when full, as long as it is drawn. */
    public int capacity() {
        return CAPACITY;
    }

    /** With {@code earned} added; what does not fit is lost. */
    public AbilityBar earning(AbilityUnits earned) {
        return new AbilityBar(this.cost, this.fill.plus(earned).atMost(AbilityUnits.whole(this.capacity())));
    }

    /** Whether one cast's cost is in. */
    public boolean isReady() {
        return this.fill.atLeast(AbilityUnits.whole(this.cost));
    }

    /** After one cast: the cost paid, the rest kept. */
    public AbilityBar afterCast() {
        if (!this.isReady()) {
            throw new IllegalStateException("A cast needs its cost in: " + this.fill + " of " + this.cost);
        }
        return new AbilityBar(this.cost, this.fill.minus(AbilityUnits.whole(this.cost)));
    }
}
