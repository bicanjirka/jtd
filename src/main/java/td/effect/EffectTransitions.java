package td.effect;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Per mob, the tick each {@link EffectKind} was last gained or lost, diffed between calls.
 * Remembers when the active set changed, not what it is.
 */
public final class EffectTransitions {

    private final Map<EffectKind, Integer> gainedTick = new EnumMap<>(EffectKind.class);
    private final Map<EffectKind, Integer> lostTick = new EnumMap<>(EffectKind.class);
    private final Set<EffectKind> previouslyActive = EnumSet.noneOf(EffectKind.class);

    /**
     * Records {@code gameTime} for every kind that appeared or disappeared since the last call.
     * Called once per live tick, after expired effects are removed.
     */
    public void observe(Set<EffectKind> nowActive, int gameTime) {
        for (EffectKind kind : nowActive) {
            if (!this.previouslyActive.contains(kind)) {
                this.gainedTick.put(kind, gameTime);
            }
        }
        for (EffectKind kind : this.previouslyActive) {
            if (!nowActive.contains(kind)) {
                this.lostTick.put(kind, gameTime);
            }
        }
        this.previouslyActive.clear();
        this.previouslyActive.addAll(nowActive);
    }

    /** {@code -1} if never gained. */
    public int ticksSinceGained(EffectKind kind, int gameTime) {
        Integer tick = this.gainedTick.get(kind);
        return tick == null ? -1 : gameTime - tick;
    }

    /** {@code -1} if never lost. */
    public int ticksSinceLost(EffectKind kind, int gameTime) {
        Integer tick = this.lostTick.get(kind);
        return tick == null ? -1 : gameTime - tick;
    }
}
