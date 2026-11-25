package td.effect;

import td.util.ThreadConfined;

import java.util.List;

/**
 * The heals one enemy took under Backlash, each struck whole as it came. They are kept, one share per source on its
 * own clock, so a source renewing its heal before it ran out strikes only what the renewal adds.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class TurnedHeals {

    private List<HealShare> shares = List.of();

    /** Takes {@code heal} in: what it adds to what the heals turned before still had to give, in damage units. */
    public long turn(Effect heal) {
        long before = left(this.shares);
        this.shares = List.copyOf(SourceShares.joined(this.shares, heal.healShares()));
        return left(this.shares) - before;
    }

    /** One tick of every turned heal's clock. */
    public void tick() {
        if (!this.shares.isEmpty()) {
            this.shares = List.copyOf(SourceShares.retimed(this.shares, ticks -> ticks - 1));
        }
    }

    // A heal that lasts until ended has no whole to strike; it is worth its one tick.
    private static long left(List<HealShare> shares) {
        long sum = 0L;
        for (HealShare share : shares) {
            long ticks = share.remainingTicks() == Effect.LASTING_TICKS ? 1L : share.remainingTicks();
            sum += share.perTick() * ticks;
        }
        return sum;
    }
}
