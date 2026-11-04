package td.tower;

import td.damage.DamageUnits;
import td.util.TickRate;

import java.util.OptionalDouble;

/**
 * What a tower has dealt in one wave, over the span from its first damage of that wave to its last. What lands on the
 * first tick opens the span and takes none of it, so it is left out: counted, a few shots would read as more shots than
 * the gaps between them, far above what the tower keeps up.
 *
 * @param wave      which wave started this tally, counted from the start of the game
 * @param dealt     damage credited after the first tick, in hundredths
 * @param firstTick the tick of the first of it
 * @param lastTick  the tick of the last of it
 */
record WaveDamageTally(long wave, long dealt, int firstTick, int lastTick) {

    /** Under this span a few hits read as a rate far above what the tower keeps up. */
    private static final int SHORTEST_SPAN_TICKS = Math.round(TickRate.TICKS_PER_SECOND);

    private static final WaveDamageTally NONE = new WaveDamageTally(-1, 0, 0, 0);

    /** Nothing dealt, in no wave. */
    static WaveDamageTally none() {
        return NONE;
    }

    /** This tally with {@code credited} more in {@code wave} at {@code tick}; a later wave starts over. */
    WaveDamageTally plus(long credited, long wave, int tick) {
        if (wave != this.wave) {
            return new WaveDamageTally(wave, 0, tick, tick);
        }
        if (tick == this.firstTick) {
            return this;
        }
        return new WaveDamageTally(wave, this.dealt + credited, this.firstTick, tick);
    }

    /** Points a second over the span of {@code wave}; empty for another wave or before the span means anything. */
    OptionalDouble pointsPerSecondIn(long wave) {
        int span = this.lastTick - this.firstTick;
        if (wave != this.wave || span < SHORTEST_SPAN_TICKS) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(DamageUnits.inPoints(this.dealt) * TickRate.TICKS_PER_SECOND / span);
    }
}
