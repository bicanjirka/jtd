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

    /** The distinct enemy definitions used in this wave, in first-seen order - never includes the {@code e} spacer. */
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
                        count++;
                    }
                }
                case EmptySlot ignored -> {
                }
            }
        }
        return count;
    }

    /** The real enemy count - every spawn slot except the {@code e} spacer. */
    public int enemyCount() {
        int count = 0;
        for (WaveSlot slot : this.spawnSequence) {
            switch (slot) {
                case EnemySlot ignored -> count++;
                case EmptySlot ignored -> {
                }
            }
        }
        return count;
    }
}
