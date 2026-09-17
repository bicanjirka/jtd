package td;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void tickInterpolationAlphaStaysWithinOneTickStep() throws InterruptedException {
        CountDownLatch sawARender = new CountDownLatch(3);
        GameLoop loop = new GameLoop(() -> {
        }, sawARender::countDown);
        loop.setSpeed(TickSpeed.NORMAL);

        loop.start();
        try {
            assertThat(sawARender.await(2, TimeUnit.SECONDS))
                    .as("expected several renders within 2s")
                    .isTrue();
            assertThat(loop.tickInterpolationAlpha()).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        } finally {
            loop.stop();
        }
    }

    @Test
    void animationSecondsAdvancesEvenWhilePaused() throws InterruptedException {
        GameLoop loop = new GameLoop(() -> {
        }, () -> {
        });
        loop.setSpeed(TickSpeed.PAUSED);

        loop.start();
        try {
            Thread.sleep(150);
            assertThat(loop.animationSeconds()).isGreaterThan(0.0);
        } finally {
            loop.stop();
        }
    }

    @Test
    void animationSecondsDoesNotRaceAheadAtSuperFastSpeed() throws InterruptedException {
        // Wall-clock animation is deliberately decoupled from tick speed - see
        // GameLoop.animationTimeScale(). At 250ms real time, animationSeconds should read
        // close to 0.25s regardless of the tick multiplier, not a multiple of it.
        GameLoop loop = new GameLoop(() -> {
        }, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);

        loop.start();
        try {
            Thread.sleep(250);
            assertThat(loop.animationSeconds()).isLessThan(1.0);
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

    @Test
    void aStoppedLoopCanBeStartedAgainAndResumesTicking() throws InterruptedException {
        // same scenario as returning to the menu and starting another level: a fresh start()
        // on an instance that already ran and stopped once
        AtomicInteger tickCount = new AtomicInteger();
        GameLoop loop = new GameLoop(tickCount::incrementAndGet, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);
        loop.start();
        Thread.sleep(50);
        loop.stop();
        Thread.sleep(50);
        int countAfterStop = tickCount.get();

        loop.start();
        try {
            Thread.sleep(200);
            assertThat(tickCount.get())
                    .as("expected the restarted loop to tick again")
                    .isGreaterThan(countAfterStop);
        } finally {
            loop.stop();
        }
    }

    @Test
    void stopWaitsForTheInFlightTickToFinishBeforeReturning() throws InterruptedException {
        // The whole point of the join: a caller stops the loop in order to tear the current
        // level down, and must not start clearing rosters and swapping the cell grid while a
        // tick is still walking them.
        AtomicBoolean insideATick = new AtomicBoolean();
        CountDownLatch tickBegan = new CountDownLatch(1);
        GameLoop loop = new GameLoop(() -> {
            insideATick.set(true);
            tickBegan.countDown();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            insideATick.set(false);
        }, () -> {
        });
        loop.setSpeed(TickSpeed.SUPER_FAST);

        loop.start();
        assertThat(tickBegan.await(2, TimeUnit.SECONDS)).isTrue();
        loop.stop();

        assertThat(insideATick)
                .as("stop() returned while a tick was still running")
                .isFalse();
    }

    @Test
    void startingAnAlreadyRunningLoopDoesNotSpawnASecondLoopThread() throws InterruptedException {
        GameLoop loop = new GameLoop(() -> {
        }, () -> {
        });
        loop.setSpeed(TickSpeed.PAUSED);

        loop.start();
        try {
            Thread.sleep(50);
            loop.start();
            Thread.sleep(50);

            long gameLoopThreads = Thread.getAllStackTraces().keySet().stream()
                    .filter(t -> "game-loop".equals(t.getName()))
                    .count();
            assertThat(gameLoopThreads).isEqualTo(1);
        } finally {
            loop.stop();
        }
    }
}
