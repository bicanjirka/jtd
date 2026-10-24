package td.enemy;

import java.util.function.IntSupplier;

/** Writes each of one mob's events as a trace line, stamped with the tick it happened in. */
final class LoggedMobEvents implements MobEventSink {

    private final ObserveLog log;
    private final IntSupplier tick;
    private final MobLabel label;

    LoggedMobEvents(ObserveLog log, IntSupplier tick, MobLabel label) {
        this.log = log;
        this.tick = tick;
        this.label = label;
    }

    @Override
    public void report(MobEvent event) {
        this.log.write(MobEventText.line(this.tick.getAsInt(), this.label, event));
    }
}
