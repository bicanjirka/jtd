package td.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Uses threads named like the JDK's and the loop's, so no EDT is started. */
class ThreadsTest {

    /** Runs {@code body} on a thread named {@code name} and returns what it threw. */
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
