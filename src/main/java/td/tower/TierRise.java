package td.tower;

import java.util.OptionalInt;

/**
 * The last time a tower's highest open tier rose: from which tier to which, on which tick. {@link #none()} for a tower
 * whose tier has not risen since it was built or loaded.
 */
public record TierRise(int from, int to, int atTick) {

    private static final TierRise NONE = new TierRise(0, 0, -1);

    public static TierRise none() {
        return NONE;
    }

    /** Ticks since the rise, if it happened no more than {@code window} ticks before {@code gameTime}. */
    public OptionalInt ticksSince(int gameTime, int window) {
        if (this.atTick < 0) {
            return OptionalInt.empty();
        }
        int ticks = gameTime - this.atTick;
        return ticks >= 0 && ticks <= window ? OptionalInt.of(ticks) : OptionalInt.empty();
    }
}
