package td.wave;

import td.enemy.EnemyDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The parsed result of a wave's token string (see the wave mini-language table in
 * CLAUDE.md) - one {@link WaveSlot} per spawn slot, in spawn order, with repeat counts
 * already flattened out. Deliberately GameWorld-free, unlike {@link Wave} itself, which turns
 * this into live {@code EnemyMob}s bound to a world.
 */
public record WaveContent(List<WaveSlot> spawnSequence) {

    public WaveContent {
        spawnSequence = List.copyOf(spawnSequence);
    }

    /**
     * The distinct enemy definitions used in this wave, in first-seen order - never includes the {@code e} spacer.
     */
    public Set<EnemyDefinition> enemySet() {
        Set<EnemyDefinition> set = new LinkedHashSet<>();
        for (WaveSlot slot : this.spawnSequence) {
            switch (slot) {
                case EnemySlot s -> set.add(s.definition());
                case EmptySlot ignored -> {
                }
            }
        }
        return set;
    }

    public int enemyCount(EnemyDefinition definition) {
        int count = 0;
        for (WaveSlot slot : this.spawnSequence) {
            switch (slot) {
                case EnemySlot s -> {
                    if (s.definition().equals(definition)) {
                        count += s.shape().members();
                    }
                }
                case EmptySlot ignored -> {
                }
            }
        }
        return count;
    }

    /**
     * The real enemy count - every shaped slot's member count summed, excluding the {@code e}
     * spacer. A shaped slot (e.g. a swarm of 4) counts as its member count, not as one, since
     * this is what {@code GameWorld.startWave} seeds the roster's alive count from and a wave is
     * cleared only once every member is dead.
     */
    public int enemyCount() {
        int count = 0;
        for (WaveSlot slot : this.spawnSequence) {
            switch (slot) {
                case EnemySlot s -> count += s.shape().members();
                case EmptySlot ignored -> {
                }
            }
        }
        return count;
    }
}
