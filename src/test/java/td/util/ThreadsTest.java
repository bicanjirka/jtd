package td.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Threads identifies the owning thread by name rather than through an AWT call, because
 * td.util is one of the headless packages. These tests therefore drive it with threads named
 * the way the JDK and GameLoop name theirs, which keeps them headless too - no EDT is started.
 */
class ThreadsTest {

    /**
     * Runs {@code body} on a thread with the given name and hands back whatever it threw.
     */
    private static RuntimeException runNamed(String name, Runnable body) throws InterruptedException {
        AtomicReference<RuntimeException> thrown = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                body.run();
            } catch (RuntimeException e) {
                thrown.set(e);
            }
        }, name);
        t.start();
        t.join();
        return thrown.get();
    }

    @Test
    void theGameLoopAssertionPassesOnTheLoopThreadAndFailsEverywhereElse() throws InterruptedException {
        assertThatThrownBy(() -> Threads.assertGameLoop("doTick"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("doTick")
                .hasMessageContaining("game-loop");

        assertThat(runNamed(Threads.GAME_LOOP, () -> Threads.assertGameLoop("doTick"))).isNull();
    }

    @Test
    void theEventDispatchAssertionPassesOnAnAwtEventQueueThread() throws InterruptedException {
        assertThat(runNamed("AWT-EventQueue-0",
                () -> Threads.assertEventDispatchThread("TowerDefense construction"))).isNull();

        assertThat(runNamed(Threads.GAME_LOOP,
                () -> Threads.assertEventDispatchThread("TowerDefense construction")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aBlockingCallIsRefusedOnTheEventDispatchThreadAndAllowedOffIt() throws InterruptedException {
        assertThat(runNamed("AWT-EventQueue-0",
                () -> Threads.assertNotEventDispatchThread("GameLoop.stop()")))
                .isInstanceOf(IllegalStateException.class);

        assertThatCode(() -> Threads.assertNotEventDispatchThread("GameLoop.stop()"))
                .doesNotThrowAnyException();
    }
}
