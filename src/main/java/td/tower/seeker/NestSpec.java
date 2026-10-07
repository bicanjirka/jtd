package td.tower.seeker;

/**
 * The Seeker's nest: how many missiles it banks, and how far apart a salvo launches them.
 *
 * @param capacity       how many missiles it holds; {@code 0} for no nest, so it fires as its cooldown ends
 * @param launchGapTicks ticks between the missiles of a salvo
 */
public record NestSpec(int capacity, int launchGapTicks) {

    /** No nest: the cooldown fires a missile directly. */
    public static NestSpec none() {
        return new NestSpec(0, 0);
    }

    /** A nest of {@code capacity} whose salvo launches a missile every {@code launchGapTicks}. */
    public static NestSpec of(int capacity, int launchGapTicks) {
        return new NestSpec(capacity, launchGapTicks);
    }

    public boolean isActive() {
        return this.capacity > 0;
    }

    public NestSpec withCapacity(int capacity) {
        return new NestSpec(capacity, this.launchGapTicks);
    }

    /** This nest holding {@code extra} more missiles. */
    public NestSpec holdingMore(int extra) {
        return this.withCapacity(this.capacity + extra);
    }
}
