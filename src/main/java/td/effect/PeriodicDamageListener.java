package td.effect;

/**
 * Told which effect is about to deal its damage through its sink, so whoever watches the carrier can
 * name the effect behind a periodic hit; the sink itself only carries the damage.
 */
@FunctionalInterface
public interface PeriodicDamageListener {

    /** A listener that ignores every tick, for a carrier nobody watches. */
    static PeriodicDamageListener none() {
        return kind -> {
        };
    }

    void dealing(EffectKind kind);
}
