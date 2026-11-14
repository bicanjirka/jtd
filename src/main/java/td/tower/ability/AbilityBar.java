package td.tower.ability;

/**
 * One ability's store of its tower's currency. It holds a unit more than the ability costs, so a full ability waiting
 * for a target, or a large payout landing at once, loses less; past that the rest is lost.
 *
 * @param cost what one cast takes, in whole units
 * @param fill what it holds now
 */
public record AbilityBar(int cost, AbilityUnits fill) {

    private static final int SPARE_UNITS = 1;
    private static final int UNITS_AT_WAVE_START = 1;

    public AbilityBar {
        if (cost < 1) {
            throw new IllegalArgumentException("An ability costs at least one unit: " + cost);
        }
        if (fill.compareTo(AbilityUnits.whole(cost + SPARE_UNITS)) > 0) {
            throw new IllegalArgumentException("A bar holds at most its cost and a spare unit: " + fill);
        }
    }

    /** The bar as every wave finds it, so the first cast comes early. */
    public static AbilityBar atWaveStart(int cost) {
        return new AbilityBar(cost, AbilityUnits.whole(UNITS_AT_WAVE_START));
    }

    /** How many whole units it holds when full, as long as it is drawn. */
    public int capacity() {
        return this.cost + SPARE_UNITS;
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
