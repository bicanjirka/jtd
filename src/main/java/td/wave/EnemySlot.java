package td.wave;

import td.enemy.EnemyDefinition;

/** A real enemy spawn - {@link Wave} builds one live mob from {@code definition} for this slot. */
public record EnemySlot(EnemyDefinition definition) implements WaveSlot {
}
