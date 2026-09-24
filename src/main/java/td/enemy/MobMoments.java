package td.enemy;

import td.util.ThreadConfined;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The tick each {@link Moment} last happened to one mob. A moment that happens where no game time
 * is known (a hit lands from another phase of the tick) is marked pending and stamped by the next
 * {@link #capturePending}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class MobMoments {

    private final Map<Moment, Integer> lastTick = new EnumMap<>(Moment.class);
    private final Set<Moment> pending = EnumSet.noneOf(Moment.class);

    void markPending(Moment moment) {
        this.pending.add(moment);
    }

    void capturePending(int gameTime) {
        for (Moment moment : this.pending) {
            this.lastTick.put(moment, gameTime);
        }
        this.pending.clear();
    }

    void record(Moment moment, int gameTime) {
        this.lastTick.put(moment, gameTime);
    }

    /** {@code -1} if it never happened, or is still pending. */
    int ticksSince(Moment moment, int gameTime) {
        Integer tick = this.lastTick.get(moment);
        return tick == null ? -1 : gameTime - tick;
    }

    enum Moment {
        DAMAGE_TAKEN,
        CRITICAL_HIT,
        /** Recorded once: a dead mob takes no further hits. */
        DEATH,
        ABILITY_SPAWN
    }
}
