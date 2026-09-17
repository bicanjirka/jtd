package td.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RandomSourceTest {

    @Test
    void theSameSeedReplaysTheSameSequence() {
        RandomSource first = RandomSource.seeded(42L);
        RandomSource second = RandomSource.seeded(42L);

        for (int i = 0; i < 10; i++) {
            assertThat(first.nextDouble()).isEqualTo(second.nextDouble());
        }
    }

    @Test
    void seedsOneApartProduceUncorrelatedFirstDraws() {
        // The regression this guards: java.util.Random's own seed-to-state scramble is a thin,
        // reversible XOR, so handing it seed, seed+1, seed+2, ... directly makes every one of
        // those sources agree on their *first* draw to two decimal places - exactly the shape
        // of seed td.wave.Wave derives per slot (scatterSeed * 31 + slotIndex). seeded() must
        // scramble the seed first so that pathological case doesn't happen.
        long base = 382695L;
        double first = RandomSource.seeded(base).nextDouble();
        boolean anyMeaningfullyDifferent = false;
        for (long seed = base + 1; seed <= base + 11; seed++) {
            double next = RandomSource.seeded(seed).nextDouble();
            if (Math.abs(next - first) > 0.05) {
                anyMeaningfullyDifferent = true;
                break;
            }
        }

        assertThat(anyMeaningfullyDifferent).isTrue();
    }

    @Test
    void nextIndexStaysWithinBoundsAndCoversTheWholeRangeOverManyDraws() {
        RandomSource random = RandomSource.seeded(1L);
        boolean[] seen = new boolean[5];

        for (int i = 0; i < 500; i++) {
            int index = random.nextIndex(5);
            assertThat(index).isBetween(0, 4);
            seen[index] = true;
        }

        assertThat(seen).containsOnly(true);
    }

    @Test
    void nextIndexRejectsANonPositiveSize() {
        RandomSource random = RandomSource.seeded(1L);

        assertThatThrownBy(() -> random.nextIndex(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> random.nextIndex(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sharedDrawsStayInTheHalfOpenUnitRange() {
        RandomSource random = RandomSource.shared();

        for (int i = 0; i < 100; i++) {
            double value = random.nextDouble();
            assertThat(value).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        }
    }
}
