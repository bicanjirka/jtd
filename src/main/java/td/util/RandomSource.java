package td.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The simulation's randomness, injected so a run can be reproduced. The game uses
 * {@link #shared()}; headless balance runs use {@link #seeded(long)}.
 * <p>
 * Each wave draws from a stream of its own, started from the game's seed and the wave's number
 * ({@link #reseeded(long)}), so a wave's luck never depends on how the waves before it went, and a
 * restarted or loaded wave replays the same luck. A test's fixed source ignores seeds and stays as
 * it is.
 */
@FunctionalInterface
public interface RandomSource {

    /** Unseeded: each game draws a seed of its own. */
    static RandomSource shared() {
        return new Shared();
    }

    /**
     * A seeded generator that replays the same sequence. The seed is scrambled first, because
     * {@link Random} starts nearby seeds in nearby states and callers derive seeds by small
     * offsets. The scramble is a bijection, so a fixed seed stays reproducible.
     */
    static RandomSource seeded(long seed) {
        return new Seeded(new Random(scramble(seed)));
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

    /** A seed for a new game, drawn from this source; {@code 0} from one that cannot be reseeded. */
    default long newGameSeed() {
        return 0L;
    }

    /** This source's kind of stream, started over from {@code seed}; a source that cannot be reseeded is itself. */
    default RandomSource reseeded(long seed) {
        return this;
    }

    /** The JVM's own randomness until a wave reseeds it. */
    final class Shared implements RandomSource {

        private Shared() {
        }

        @Override
        public double nextDouble() {
            return ThreadLocalRandom.current().nextDouble();
        }

        @Override
        public long newGameSeed() {
            return ThreadLocalRandom.current().nextLong();
        }

        @Override
        public RandomSource reseeded(long seed) {
            return seeded(seed);
        }
    }

    /** A {@link Random} started from a known seed. */
    final class Seeded implements RandomSource {

        private final Random random;

        private Seeded(Random random) {
            this.random = random;
        }

        @Override
        public double nextDouble() {
            return this.random.nextDouble();
        }

        @Override
        public long newGameSeed() {
            return this.random.nextLong();
        }

        @Override
        public RandomSource reseeded(long seed) {
            return seeded(seed);
        }
    }
}
