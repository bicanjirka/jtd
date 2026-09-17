package td.enemy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The slot-position-to-ticks conversion moved here from AbstractEnemyMob's constructor (see
 * docs/features/FEATURE-enemy-spawn-types.md's Risk 6), so it can be exercised without a
 * GameWorld.
 */
class SpawnParametersTest {

    @Test
    void aSlotAtPositionZeroHasNoDelay() {
        SpawnParameters parameters = SpawnParameters.atSlot(0, 1.28f, 50, 3);

        assertThat(parameters.delayTicks()).isZero();
    }

    @Test
    void delayTicksGrowsWithSlotPosition() {
        SpawnParameters oneSlot = SpawnParameters.atSlot(1, 1.28f, 50, 3);
        SpawnParameters twoSlots = SpawnParameters.atSlot(2, 1.28f, 50, 3);

        assertThat(twoSlots.delayTicks()).isGreaterThan(oneSlot.delayTicks());
    }

    @Test
    void aStationaryMobAtANonZeroSlotPositionHasNoDelayInsteadOfAnUnboundedOne() {
        // Guards against dividing by a zero speed (an egg's baseSpeed is 0), which would
        // otherwise round to Integer.MAX_VALUE ticks rather than the intended zero.
        SpawnParameters parameters = SpawnParameters.atSlot(3, 0f, 50, 3);

        assertThat(parameters.delayTicks()).isZero();
    }

    @Test
    void aFasterSpeedMultiplierShortensTheDelayForTheSameSlotPosition() {
        SpawnParameters fullSpeed = SpawnParameters.of(2, 1.28f, 50, 3, 1f, 1f);
        SpawnParameters halfSpeed = SpawnParameters.of(2, 1.28f, 50, 3, 1f, 0.5f);

        assertThat(halfSpeed.delayTicks()).isGreaterThan(fullSpeed.delayTicks());
    }

    @Test
    void healthPriceAndMultipliersPassThroughUnchanged() {
        SpawnParameters parameters = SpawnParameters.of(0, 1.28f, 50, 3, 1.5f, 0.5f);

        assertThat(parameters.health()).isEqualTo(50);
        assertThat(parameters.price()).isEqualTo(3);
        assertThat(parameters.sizeMultiplier()).isEqualTo(1.5f);
        assertThat(parameters.speedMultiplier()).isEqualTo(0.5f);
    }
}
