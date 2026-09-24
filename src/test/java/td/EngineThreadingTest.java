package td;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.fixtures.LevelFixtures;
import td.level.LevelDefinition;
import td.wave.WaveDefinition;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The engine is driven from two threads in the real application: the EDT loads levels,
 * requests waves and places towers, while the {@code game-loop} thread ticks and renders.
 * Every other test in this suite is deliberately single-threaded, so these exist to cover the
 * handful of behaviours that only exist between the two.
 * <p>
 * A race test can pass on a broken build - it only proves the failure did not happen this
 * run. These are regression nets, not proofs: the guarantees themselves come from the
 * publication rules in CLAUDE.md 3, and each test says which one it is exercising.
 */
class EngineThreadingTest {

    // Carries a real wave, unlike the shared level fixture.
    private static LevelDefinition level(String name, int width, int height) {
        return LevelDefinition.unsmoothed(name, "", width, height, LevelFixtures.STRAIGHT_PATH,
                List.of(new WaveDefinition("3 c", Rank.GRUNT)), 100, 5);
    }

    /**
     * Exercises the volatile publication of {@code cellGrid}, {@code waves}, and GameWorld's
     * board/path: a level load replaces all of them while a tick is walking the previous
     * level's state. Before those were published safely - and before {@code GameLoop.stop()}
     * joined - this is the shape that threw from inside a tick and was swallowed as a one-off
     * ERROR line.
     */
    @Test
    void loadingALevelWhileTicksAreRunningNeverThrowsFromTheTickThread() throws InterruptedException {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level("First", 5, 5));
        AtomicReference<Throwable> tickFailure = new AtomicReference<>();
        CountDownLatch ticking = new CountDownLatch(1);
        AtomicReference<Boolean> keepTicking = new AtomicReference<>(true);

        Thread ticker = new Thread(() -> {
            int time = 0;
            ticking.countDown();
            while (keepTicking.get()) {
                try {
                    engine.doTick(time++);
                } catch (RuntimeException e) {
                    tickFailure.set(e);
                    return;
                }
            }
        }, "test-tick");
        ticker.start();
        assertThat(ticking.await(2, TimeUnit.SECONDS)).isTrue();

        for (int i = 0; i < 200 && tickFailure.get() == null; i++) {
            engine.loadLevel(level("Reload " + i, 5 + (i % 4), 5 + (i % 3)));
        }
        keepTicking.set(false);
        ticker.join(2000);

        assertThat(tickFailure.get()).isNull();
    }

    /**
     * Exercises the volatile publication of {@code startWave}: the request is made on one
     * thread and consumed by {@code doTick} on another, with no lock between them.
     */
    @Test
    void aWaveRequestedFromAnotherThreadIsConsumedByTheTickThread() throws InterruptedException {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level("Requested", 5, 5));
        engine.startLevel();
        CountDownLatch waveStarted = new CountDownLatch(1);

        Thread ticker = new Thread(() -> {
            int time = 0;
            while (waveStarted.getCount() > 0 && time < 100_000) {
                if (engine.doTick(time++)) {
                    waveStarted.countDown();
                }
            }
        }, "test-tick");
        ticker.start();
        engine.requestNextWave();

        assertThat(waveStarted.await(2, TimeUnit.SECONDS))
                .as("expected the tick thread to observe the wave request")
                .isTrue();
        ticker.join(2000);
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
    }
}
