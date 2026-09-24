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
 * Behaviour between the EDT and the game-loop thread. A passing race test only proves the race
 * didn't happen this run; these are regression nets, not proofs.
 */
class EngineThreadingTest {

    // Carries a real wave, unlike the shared level fixture.
    private static LevelDefinition level(String name, int width, int height) {
        return LevelDefinition.unsmoothed(name, "", width, height, LevelFixtures.STRAIGHT_PATH,
                List.of(new WaveDefinition("3 c", Rank.GRUNT)), 100, 5);
    }

    /** A level load replaces the state a running tick walks. */
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

    /** The wave request is made on one thread and consumed by {@code doTick} on another. */
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
