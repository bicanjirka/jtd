package td.wave;

/** One slot in a wave: a real enemy or the {@code e} spacer. */
public sealed interface WaveSlot permits EnemySlot, EmptySlot {
}
