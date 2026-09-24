package td.util;

import java.util.Random;

/**
 * The simulation's randomness, injected so a run can be reproduced. The game uses
 * {@link #shared()}; {@link td.BalanceHarness} uses {@link #seeded(long)}.
 */
@FunctionalInterface
public interface RandomSource {

    /** The unseeded JVM-wide generator behind {@link Math#random()}. */
    static RandomSource shared() {
        return Math::random;
    }

    /**
     * A seeded generator that replays the same sequence. The seed is scrambled first, because
     * {@link Random} starts nearby seeds in nearby states and callers derive seeds by small
     * offsets. The scramble is a bijection, so a fixed seed stays reproducible.
     */
    static RandomSource seeded(long seed) {
        Random random = new Random(scramble(seed));
        return random::nextDouble;
    }

    /** SplitMix64's finalizer (Sebastiano Vigna, public domain). */
    private static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The next value in {@code [0, 1)}. Must be safe to call from the game-loop thread. */
    double nextDouble();

    /** An index in {@code [0, size)}. */
    default int nextIndex(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        return (int) (this.nextDouble() * size);
    }
}
