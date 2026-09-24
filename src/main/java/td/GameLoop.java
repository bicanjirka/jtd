package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.util.ThreadConfined;
import td.util.Threads;
import td.util.TickRate;

/**
 * Runs the simulation on its own thread with two fixed-timestep accumulators: {@code onTick} runs
 * zero or more times per interval depending on tick speed, while {@code onRender} fires at a flat
 * ~60fps, so the board keeps redrawing while paused.
 * <p>
 * Both callbacks run on the {@code game-loop} thread, never the EDT: {@code onRender} builds a
 * snapshot of state that thread owns. Handing it to the EDT is the caller's job.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public class GameLoop implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);

    private static final long RENDER_INTERVAL_NANOS = 16_666_667L; // ~60fps
    private static final long POLL_NANOS = 1_000_000L;
    private static final int MAX_CONSECUTIVE_TICK_FAILURES = 10;

    private final TickAccumulator tickAccumulator = new TickAccumulator(TickRate.TICK_NANOS);
    private final TickAccumulator renderAccumulator = new TickAccumulator(RENDER_INTERVAL_NANOS);
    private final Runnable onTick;
    private final Runnable onRender;

    private volatile double speedMultiplier = TickSpeed.NORMAL.multiplier();
    private volatile boolean running = false;
    private volatile Thread thread;
    private long tickNumber = 0;
    private int consecutiveTickFailures = 0;
    private volatile double tickInterpolationAlpha = 0.0;
    // Cosmetic clock: advances in real time even while paused or fast-forwarding.
    private volatile double animationSeconds = 0.0;

    public GameLoop(Runnable onTick, Runnable onRender) {
        this.onTick = onTick;
        this.onRender = onRender;
    }

    /**
     * How fast {@link #animationSeconds} advances relative to wall-clock time. {@code 1.0} ignores
     * game speed; return {@code multiplier} to track it exactly.
     */
    private static double animationTimeScale(double multiplier) {
        return 1.0;
    }

    private static void sleepQuietly(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000L, (int) (nanos % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * How far past the last tick the loop is, as a fraction of a step in {@code [0, 1)}, for
     * interpolating a frame. Refreshed immediately before each {@code onRender}.
     */
    public double tickInterpolationAlpha() {
        return this.tickInterpolationAlpha;
    }

    /**
     * Seconds of cosmetic animation time, which neither freezes while paused nor races while
     * fast-forwarding. See {@link #animationTimeScale(double)}.
     */
    public double animationSeconds() {
        return this.animationSeconds;
    }

    public void setSpeed(TickSpeed speed) {
        LOG.info("Tick speed changed: {} -> {}x", speed, speed.multiplier());
        this.speedMultiplier = speed.multiplier();
    }

    /**
     * Starts the loop on a new daemon thread. A second call while running is ignored. Restartable
     * after {@link #stop()}: all per-run progress is reset, except the cosmetic
     * {@code animationSeconds}.
     */
    public synchronized void start() {
        if (this.running) {
            LOG.warn("GameLoop already running, ignoring start()");
            return;
        }
        LOG.info("GameLoop starting");
        this.tickAccumulator.reset();
        this.renderAccumulator.reset();
        this.tickInterpolationAlpha = 0.0;
        this.tickNumber = 0;
        this.consecutiveTickFailures = 0;
        this.running = true;
        Thread newThread = new Thread(this, Threads.GAME_LOOP);
        newThread.setDaemon(true);
        this.thread = newThread;
        newThread.start();
    }

    /**
     * Stops the loop and <strong>waits without bound</strong> for the in-flight tick to finish, so
     * a caller tearing the level down never races a running tick. A bounded wait would hand the
     * caller exactly that race.
     * <p>
     * Must not be called on the EDT, where a wedged tick would freeze the UI. Called from the loop
     * thread itself it is a no-op rather than a self-join.
     */
    public synchronized void stop() {
        Threads.assertNotEventDispatchThread("GameLoop.stop()");
        LOG.info("GameLoop stopping");
        this.running = false;
        Thread loopThread = this.thread;
        if (loopThread == null || loopThread == Thread.currentThread()) {
            return;
        }
        try {
            loopThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for the game-loop thread to stop");
        }
        this.thread = null;
    }

    @Override
    public void run() {
        // A thread finishing its last iteration after a fast stop()+start() must not adopt the
        // new run's `running` and keep looping.
        Thread self = Thread.currentThread();
        long lastNanos = System.nanoTime();
        while (this.running && this.thread == self) {
            long now = System.nanoTime();
            long elapsedNanos = now - lastNanos;
            lastNanos = now;

            double multiplier = this.speedMultiplier;
            this.animationSeconds += (elapsedNanos / 1_000_000_000.0) * animationTimeScale(multiplier);

            if (multiplier > 0.0) {
                int ticks = this.tickAccumulator.accumulate((long) (elapsedNanos * multiplier));
                if (ticks > 1) {
                    ticks = 1;
                    this.tickAccumulator.reset();
                }
                for (int i = 0; i < ticks && this.running; i++) {
                    this.runTick();
                }
            }

            if (this.renderAccumulator.accumulate(elapsedNanos) > 0) {
                this.tickInterpolationAlpha = this.tickAccumulator.fractionElapsed();
                this.runRender();
            }

            sleepQuietly(POLL_NANOS);
        }
    }

    private void runTick() {
        this.tickNumber++;
        try {
            this.onTick.run();
            this.consecutiveTickFailures = 0;
        } catch (RuntimeException e) {
            this.consecutiveTickFailures++;
            LOG.error("Tick {} failed, skipping this tick ({}/{} consecutive failures)",
                    this.tickNumber, this.consecutiveTickFailures, MAX_CONSECUTIVE_TICK_FAILURES, e);
            if (this.consecutiveTickFailures >= MAX_CONSECUTIVE_TICK_FAILURES) {
                LOG.error("GameLoop stopping after {} consecutive tick failures", this.consecutiveTickFailures);
                this.running = false;
            }
        }
    }

    private void runRender() {
        try {
            this.onRender.run();
        } catch (RuntimeException e) {
            LOG.error("Render failed", e);
        }
    }
}
