package td.wave;

/**
 * One parsed slot in a wave's spawn sequence - either a real enemy (naming the
 * {@code EnemyDefinition} to spawn) or the {@code e} spacer, which occupies a slot for spawn
 * *timing* only. Replaces the closed {@code EnemyFactory.Enemy} this used to hold directly: a
 * spawn slot now names a definition, not a Java enum constant, so a per-level custom or cloned
 * enemy (once something registers one) is exactly as spawnable as a built-in.
 */
public sealed interface WaveSlot permits EnemySlot, EmptySlot {
}
