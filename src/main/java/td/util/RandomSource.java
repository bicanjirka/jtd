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
     * <p>
     * The seed is scrambled before construction (the public-domain SplitMix64 finalizer) rather
     * than handed to {@link Random} directly - {@code Random}'s own seed-to-state scramble is
     * a thin, reversible XOR, so nearby seeds start in nearby states and their *first* draw is
     * strongly correlated (empirically, seeds 1 apart agreed to two decimal places). A caller
     * that derives several seeds from one base by a small offset - {@code Wave} does exactly
     * this per slot - would otherwise see every slot's first draw land in nearly the same spot.
     * The scramble is a bijection, so reproducibility for a single fixed seed is unaffected.
     */
    static RandomSource seeded(long seed) {
        Random random = new Random(scramble(seed));
        return random::nextDouble;
    }

    /**
     * SplitMix64's finalizer (Sebastiano Vigna, public domain) - a fast, well-mixed bijection
     * on 64 bits, used here only to decorrelate nearby input seeds before they reach
     * {@link Random}'s own weaker scramble.
     */
    private static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
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
