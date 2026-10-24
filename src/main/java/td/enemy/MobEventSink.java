package td.enemy;

/** Where an observed mob's events go. */
@FunctionalInterface
public interface MobEventSink {

    static MobEventSink none() {
        return event -> {
        };
    }

    void report(MobEvent event);
}
