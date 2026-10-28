package td.effect;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
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

    /** {@code kinds} ordered by the tick each was last gained, oldest first, ties in kind order. */
    public List<EffectKind> inGainedOrder(Collection<EffectKind> kinds) {
        return kinds.stream().sorted(Comparator.comparingInt(EffectKind::ordinal))
                .sorted(Comparator.comparingInt(kind -> this.gainedTick.getOrDefault(kind, Integer.MAX_VALUE)))
                .toList();
    }

    /** Empty if never gained. */
    public OptionalInt ticksSinceGained(EffectKind kind, int gameTime) {
        return this.gainedWithin(kind, gameTime, Integer.MAX_VALUE);
    }

    /** Empty if never lost. */
    public OptionalInt ticksSinceLost(EffectKind kind, int gameTime) {
        return this.lostWithin(kind, gameTime, Integer.MAX_VALUE);
    }

    /** How long ago {@code kind} was last gained, if no more than {@code ticks} ago. */
    public OptionalInt gainedWithin(EffectKind kind, int gameTime, int ticks) {
        return within(this.gainedTick.get(kind), gameTime, ticks);
    }

    /** How long ago {@code kind} was last lost, if no more than {@code ticks} ago. */
    public OptionalInt lostWithin(EffectKind kind, int gameTime, int ticks) {
        return within(this.lostTick.get(kind), gameTime, ticks);
    }

    /** Builds an answer only inside the window: the frame build asks every kind of every enemy each frame. */
    private static OptionalInt within(Integer tick, int gameTime, int ticks) {
        return tick == null || gameTime - tick > ticks ? OptionalInt.empty() : OptionalInt.of(gameTime - tick);
    }
}
