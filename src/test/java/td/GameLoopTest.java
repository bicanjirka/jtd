package td;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import td.util.TickRate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A turn of the loop is driven with made-up time through {@code advance}; only what needs the thread
 * itself (start, stop, restart) runs a real one. The tick maths is covered by {@code TickAccumulatorTest}.
 */
class GameLoopTest {

    private static final long ONE_SECOND = 1_000_000_000L;

    private static GameLoop loopAt(TickSpeed speed, Runnable onTick, Runnable onRender) {
        GameLoop loop = new GameLoop(onTick, onRender);
        loop.setSpeed(speed);
        return loop;
    }

    private static void noOp() {
    }

    @Test
    void aTurnRunsTheTicksItsTimePaysForAtTheSpeed() {
        AtomicInteger ticks = new AtomicInteger();
        GameLoop loop = loopAt(TickSpeed.NORMAL, ticks::incrementAndGet, GameLoopTest::noOp);

        loop.advance(TickRate.TICK_NANOS / 2);
        loop.advance(TickRate.TICK_NANOS / 2);

        assertThat(ticks.get()).isEqualTo(1);
    }

    @Test
    void aTurnThatFellBehindRunsOneTickRatherThanABurst() {
        AtomicInteger ticks = new AtomicInteger();
        GameLoop loop = loopAt(TickSpeed.NORMAL, ticks::incrementAndGet, GameLoopTest::noOp);

        loop.advance(5 * TickRate.TICK_NANOS);
        loop.advance(TickRate.TICK_NANOS / 2);

        assertThat(ticks.get()).isEqualTo(1);
    }

    @Test
    void doesNotTickWhilePaused() {
        AtomicInteger ticks = new AtomicInteger();
        GameLoop loop = loopAt(TickSpeed.PAUSED, ticks::incrementAndGet, GameLoopTest::noOp);

        loop.advance(ONE_SECOND);

        assertThat(ticks.get()).isZero();
    }

    @Test
    void aRequestedStepRunsExactlyOneTickWhilePaused() {
        AtomicInteger ticks = new AtomicInteger();
        GameLoop loop = loopAt(TickSpeed.PAUSED, ticks::incrementAndGet, GameLoopTest::noOp);

        loop.requestStep();
        loop.advance(0);
        loop.advance(ONE_SECOND);

        assertThat(ticks.get()).isEqualTo(1);
    }

    @Test
    void rendersEvenWhilePaused() {
        AtomicInteger renders = new AtomicInteger();
        GameLoop loop = loopAt(TickSpeed.PAUSED, GameLoopTest::noOp, renders::incrementAndGet);

        loop.advance(ONE_SECOND);

        assertThat(renders.get()).isEqualTo(1);
    }

    @Test
    void aFailingTickIsLoggedAndDoesNotStopSubsequentTicks() {
        AtomicInteger attempts = new AtomicInteger();
        RuntimeException failure = new RuntimeException("tick failed on purpose");
        GameLoop loop = loopAt(TickSpeed.NORMAL, () -> {
            if (attempts.getAndIncrement() == 0) {
                throw failure;
            }
        }, GameLoopTest::noOp);

        try (CapturedLog log = new CapturedLog(GameLoop.class)) {
            for (int turn = 0; turn < 4; turn++) {
                loop.advance(TickRate.TICK_NANOS);
            }

            assertThat(attempts.get()).isEqualTo(4);
            assertThat(log.atLevel(Level.ERROR))
                    .singleElement()
                    .satisfies(event -> assertThat(event.getThrowableProxy().getMessage())
                            .isEqualTo(failure.getMessage()));
        }
    }

    @Test
    void tickInterpolationAlphaIsHowFarTheLoopIsIntoTheNextTick() {
        GameLoop loop = loopAt(TickSpeed.NORMAL, GameLoopTest::noOp, GameLoopTest::noOp);

        loop.advance(TickRate.TICK_NANOS * 3 / 2);

        assertThat(loop.tickInterpolationAlpha()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void animationSecondsAdvancesEvenWhilePaused() {
        GameLoop loop = loopAt(TickSpeed.PAUSED, GameLoopTest::noOp, GameLoopTest::noOp);

        loop.advance(ONE_SECOND / 4);

        assertThat(loop.animationSeconds()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void animationSecondsDoesNotRaceAheadAtSuperFastSpeed() {
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, GameLoopTest::noOp, GameLoopTest::noOp);

        loop.advance(ONE_SECOND / 4);

        assertThat(loop.animationSeconds()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void ticksRepeatedlyOnItsOwnThreadAtSuperFastSpeed() throws InterruptedException {
        CountDownLatch sawSeveralTicks = new CountDownLatch(3);
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, sawSeveralTicks::countDown, GameLoopTest::noOp);

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
    void stopEndsTheLoopThreadSoNoTickCanFollow() throws InterruptedException {
        AtomicReference<Thread> loopThread = new AtomicReference<>();
        CountDownLatch ticked = new CountDownLatch(1);
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, () -> {
            loopThread.set(Thread.currentThread());
            ticked.countDown();
        }, GameLoopTest::noOp);
        loop.start();
        assertThat(ticked.await(2, TimeUnit.SECONDS)).isTrue();

        loop.stop();

        assertThat(loopThread.get().isAlive()).isFalse();
    }

    @Test
    void interruptingTheLoopThreadEndsIt() throws InterruptedException {
        AtomicReference<Thread> loopThread = new AtomicReference<>();
        CountDownLatch ticked = new CountDownLatch(1);
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, () -> {
            loopThread.set(Thread.currentThread());
            ticked.countDown();
        }, GameLoopTest::noOp);
        loop.start();
        assertThat(ticked.await(2, TimeUnit.SECONDS)).isTrue();

        loopThread.get().interrupt();
        loopThread.get().join(2_000);

        assertThat(loopThread.get().isAlive()).isFalse();
    }

    @Test
    void aStoppedLoopCanBeStartedAgainAndResumesTicking() throws InterruptedException {
        AtomicReference<CountDownLatch> nextTick = new AtomicReference<>(new CountDownLatch(1));
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, () -> nextTick.get().countDown(), GameLoopTest::noOp);
        loop.start();
        assertThat(nextTick.get().await(2, TimeUnit.SECONDS)).isTrue();
        loop.stop();
        CountDownLatch afterRestart = new CountDownLatch(1);
        nextTick.set(afterRestart);

        loop.start();
        try {
            assertThat(afterRestart.await(2, TimeUnit.SECONDS))
                    .as("expected the restarted loop to tick again")
                    .isTrue();
        } finally {
            loop.stop();
        }
    }

    @Test
    void stopWaitsOutAnInFlightTickHoweverLongItRuns() throws InterruptedException {
        AtomicBoolean insideATick = new AtomicBoolean();
        CountDownLatch tickBegan = new CountDownLatch(1);
        GameLoop loop = loopAt(TickSpeed.SUPER_FAST, () -> {
            insideATick.set(true);
            tickBegan.countDown();
            try {
                // Longer than the half-second bound stop() once gave up after.
                Thread.sleep(900);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            insideATick.set(false);
        }, GameLoopTest::noOp);

        loop.start();
        assertThat(tickBegan.await(2, TimeUnit.SECONDS)).isTrue();
        loop.stop();

        assertThat(insideATick)
                .as("stop() returned while a tick was still running")
                .isFalse();
    }

    @Test
    void stopRefusesToRunOnTheEventDispatchThreadRatherThanFreezingIt() throws InterruptedException {
        GameLoop loop = loopAt(TickSpeed.PAUSED, GameLoopTest::noOp, GameLoopTest::noOp);
        AtomicReference<RuntimeException> thrown = new AtomicReference<>();

        Thread pretendEdt = new Thread(() -> {
            try {
                loop.stop();
            } catch (RuntimeException e) {
                thrown.set(e);
            }
        }, "AWT-EventQueue-0");
        pretendEdt.start();
        pretendEdt.join();

        assertThat(thrown.get())
                .as("stop() joins without bound, so the EDT must be refused rather than blocked")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startingAnAlreadyRunningLoopDoesNotSpawnASecondLoopThread() {
        GameLoop loop = loopAt(TickSpeed.PAUSED, GameLoopTest::noOp, GameLoopTest::noOp);

        try (CapturedLog log = new CapturedLog(GameLoop.class)) {
            loop.start();
            try {
                loop.start();

                long gameLoopThreads = Thread.getAllStackTraces().keySet().stream()
                        .filter(t -> "game-loop".equals(t.getName()))
                        .count();
                assertThat(gameLoopThreads).isEqualTo(1);
            } finally {
                loop.stop();
            }

            assertThat(log.atLevel(Level.WARN)).hasSize(1);
        }
    }
}
