package td.util;

import java.util.Random;

/**
 * Where the simulation gets its randomness. Injected rather than called statically so a run
 * can be made reproducible: {@code Math.random()} is a single global generator with no seed,
 * which means two runs of {@link td.BalanceHarness} with the same loadout are not comparable
 * to each other - the whole point of the harness.
 * <p>
 * The game uses {@link #shared()}; the harness uses {@link #seeded(long)}.
 */
@FunctionalInterface
public interface RandomSource {

    /**
     * The JVM-wide generator behind {@link Math#random()} - unseeded, and what the game uses.
     */
    static RandomSource shared() {
        return Math::random;
    }

    /**
     * A generator seeded for reproducibility, so the same seed replays the same sequence.
     * Backed by {@link Random}, which is safe to share across threads.
     */
    static RandomSource seeded(long seed) {
        Random random = new Random(seed);
        return random::nextDouble;
    }

    /**
     * The next value in {@code [0, 1)}, with the same contract as {@link Math#random()}.
     * Implementations must be safe to call from the {@code game-loop} thread.
     */
    double nextDouble();

    /**
     * Picks an index in {@code [0, size)}. The one place the double-to-index conversion
     * lives, so no caller re-derives it.
     */
    default int nextIndex(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        return (int) (this.nextDouble() * size);
    }
}
