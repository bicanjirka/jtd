package td.stat;

import td.util.ThreadConfined;

import java.util.function.Consumer;

/** Records what contributions do to one stat, as one modifier: for a breakdown, never per tick. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class StatRecorder<S extends Enum<S> & StatKey> implements StatAccumulator<S> {

    private final S stat;

    private StatModifier recorded = StatModifier.none();

    private StatRecorder(S stat) {
        this.stat = stat;
    }

    /** What {@code contribution} does to {@code stat}. */
    public static <S extends Enum<S> & StatKey> StatModifier of(S stat, Consumer<StatAccumulator<S>> contribution) {
        StatRecorder<S> recorder = new StatRecorder<>(stat);
        contribution.accept(recorder);
        return recorder.recorded;
    }

    @Override
    public void add(S stat, StatModifier modifier) {
        this.record(stat, modifier);
    }

    @Override
    public void multiply(S stat, float factor) {
        this.record(stat, StatModifier.times(factor));
    }

    @Override
    public void restoreFlat(S stat, float amount) {
        this.record(stat, StatModifier.flat(amount));
    }

    @Override
    public void capAtShareOfBase(S stat, float share) {
        this.record(stat, StatModifier.capAtShareOfBase(share));
    }

    private void record(S stat, StatModifier modifier) {
        if (stat == this.stat) {
            this.recorded = this.recorded.plus(modifier);
        }
    }
}
