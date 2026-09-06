package td;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GameLoop's tick-rate math is covered exhaustively and deterministically by
 * TickAccumulatorTest; these are lightweight smoke tests confirming the loop
 * actually drives a real background thread end-to-end.
 */
class GameLoopTest {

    @Test
    void ticksRepeatedlyAtSuperFastSpeed() throws InterruptedException {
        CountDownLatch sawSeveralTicks = new CountDownLatch(3);
        GameLoop loop = new GameLoop(sawSeveralTicks::countDown, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);

        loop.start();
        try {
            assertThat(sawSeveralTicks.await(2, TimeUnit.SECONDS))
                    .as("expected several ticks within 2s at super-fast speed")
                    .isTrue();
        } finally {
            loop.stop();
        }
    }

    @Test
    void doesNotTickWhilePaused() throws InterruptedException {
        AtomicInteger tickCount = new AtomicInteger();
        GameLoop loop = new GameLoop(tickCount::incrementAndGet, () -> {
        });
        loop.setSpeed(TickSpeed.PAUSED);

        loop.start();
        try {
            Thread.sleep(200);
            assertThat(tickCount.get()).isZero();
        } finally {
            loop.stop();
        }
    }

    @Test
    void rendersEvenWhilePaused() throws InterruptedException {
        // Rendering runs on its own real-time cadence so the board (tower placement
        // highlights, hover effects, ...) keeps redrawing while the simulation is paused.
        CountDownLatch sawARender = new CountDownLatch(1);
        GameLoop loop = new GameLoop(() -> {
        }, sawARender::countDown);
        loop.setSpeed(TickSpeed.PAUSED);

        loop.start();
        try {
            assertThat(sawARender.await(2, TimeUnit.SECONDS))
                    .as("expected a render request within 2s even while paused")
                    .isTrue();
        } finally {
            loop.stop();
        }
    }

    @Test
    void aFailingTickIsLoggedAndDoesNotStopSubsequentTicks() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch sawTicksAfterAFailure = new CountDownLatch(3);
        GameLoop loop = new GameLoop(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new RuntimeException("boom");
            }
            sawTicksAfterAFailure.countDown();
        }, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);

        loop.start();
        try {
            assertThat(sawTicksAfterAFailure.await(2, TimeUnit.SECONDS))
                    .as("expected ticks to keep running after the first one threw")
                    .isTrue();
        } finally {
            loop.stop();
        }
    }

    @Test
    void stopHaltsFutureTicks() throws InterruptedException {
        AtomicInteger tickCount = new AtomicInteger();
        GameLoop loop = new GameLoop(tickCount::incrementAndGet, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);
        loop.start();
        Thread.sleep(50);

        loop.stop();
        Thread.sleep(50);
        int countAfterStop = tickCount.get();
        Thread.sleep(100);

        assertThat(tickCount.get()).isEqualTo(countAfterStop);
    }
}
