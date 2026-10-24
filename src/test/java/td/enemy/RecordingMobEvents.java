package td.enemy;

import java.util.ArrayList;
import java.util.List;

/** Keeps every event an observed mob reports, in order. */
final class RecordingMobEvents implements MobEventSink {

    private final List<MobEvent> events = new ArrayList<>();

    @Override
    public void report(MobEvent event) {
        this.events.add(event);
    }

    List<MobEvent> all() {
        return List.copyOf(this.events);
    }

    /** The events of one kind, in order. */
    <T extends MobEvent> List<T> of(Class<T> kind) {
        return this.events.stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    /** The effect changes of {@code change}, in order. */
    List<MobEvent.EffectChanged> changes(MobEvent.Change change) {
        return this.of(MobEvent.EffectChanged.class).stream().filter(c -> c.change() == change).toList();
    }
}
