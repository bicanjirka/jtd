package td.effect;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-mob record of the tick each {@link EffectKind} was last gained or lost, diffed from the
 * set observed on the previous call - the transition-detection half of the effect-visuals
 * feature (see {@code td/ui/CLAUDE.md}). Not itself the source of truth for which effects are
 * active - {@link ActiveEffects#activeKinds()} is - this only remembers *when* that set last
 * changed, the same "entity records a tick, a frame builder turns it into a progress" idiom
 * {@code AbstractEnemyMob.deathTick}/{@code criticalHitTick} already use.
 */
public final class EffectTransitions {

    private final Map<EffectKind, Integer> gainedTick = new EnumMap<>(EffectKind.class);
    private final Map<EffectKind, Integer> lostTick = new EnumMap<>(EffectKind.class);
    private final Set<EffectKind> previouslyActive = EnumSet.noneOf(EffectKind.class);

    /**
     * Diffs {@code nowActive} against the set observed on the previous call, recording
     * {@code gameTime} against every kind that just appeared or just disappeared. Called once
     * per live tick, immediately after {@code ActiveEffects.tick()} has removed anything that
     * expired this tick - see {@code AbstractEnemyMob.doTick}'s ordering comment.
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

    /**
     * Ticks elapsed since {@code kind} was last observed to become active, or {@code -1} if it
     * never has.
     */
    public int ticksSinceGained(EffectKind kind, int gameTime) {
        Integer tick = this.gainedTick.get(kind);
        return tick == null ? -1 : gameTime - tick;
    }

    /**
     * Ticks elapsed since {@code kind} was last observed to become inactive, or {@code -1} if it
     * never has (including if it is currently active and has never been lost).
     */
    public int ticksSinceLost(EffectKind kind, int gameTime) {
        Integer tick = this.lostTick.get(kind);
        return tick == null ? -1 : gameTime - tick;
    }
}
