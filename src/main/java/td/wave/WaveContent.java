package td.wave;

import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.SpawnParameters;
import td.util.GameStartupException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A parsed wave: one {@link WaveSlot} per spawn slot, in order, repeats expanded, and the spawn
 * delay one slot of spacing is worth. World-free; {@link Wave} makes it live.
 */
public record WaveContent(List<WaveSlot> spawnSequence, float delayTicksPerSlot) {

    public WaveContent {
        spawnSequence = List.copyOf(spawnSequence);
        if (!(delayTicksPerSlot > 0f) || Float.isInfinite(delayTicksPerSlot)) {
            throw new GameStartupException(
                    "A wave's spawn spacing must be a positive number of ticks, was " + delayTicksPerSlot);
        }
    }

    /** At the default spacing. */
    public WaveContent(List<WaveSlot> spawnSequence) {
        this(spawnSequence, SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT);
    }

    /** Distinct enemy definitions in first-seen order, without the spacer. */
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

    /** The resolved rank of the slot spawning {@code definition}. */
    public Rank rankFor(EnemyDefinition definition) {
        for (WaveSlot slot : this.spawnSequence) {
            switch (slot) {
                case EnemySlot s -> {
                    if (s.definition().equals(definition)) {
                        return s.rank();
                    }
                }
                case EmptySlot ignored -> {
                }
            }
        }
        throw new IllegalArgumentException("No slot in this wave spawns " + definition.id());
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
     * Enemies in the wave: every slot's member count, without spacers. The roster's alive count is
     * seeded from this, so a wave clears only when every member is dead.
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
