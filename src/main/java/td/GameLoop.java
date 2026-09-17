package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the simulation on its own dedicated thread using two independent
 * fixed-timestep {@link TickAccumulator}s: {@code onTick} runs a variable
 * number of times per real-world interval - zero while paused
 * ({@link TickSpeed#PAUSED}), several in a row when running fast - while
 * {@code onRender} fires on a flat ~60fps real-time cadence, regardless of
 * tick speed or pause, so the board (tower placement highlights, hover
 * effects, ...) keeps redrawing even while the simulation itself is paused
 * or running fast.
 * <p>
 * <strong>Both callbacks run on the {@code game-loop} thread, never on the
 * Event Dispatch Thread.</strong> That is deliberate: {@code onRender} is
 * where the caller builds an immutable snapshot of simulation state it then
 * hands to the EDT, so it has to run on the thread that owns that state. This
 * class contains no Swing dependency at all; crossing to the EDT is the
 * caller's job and happens after the snapshot exists. See CLAUDE.md 3.
 */
public class GameLoop implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);

    private static final long BASE_TICK_NANOS = 50_000_000L;
    private static final long RENDER_INTERVAL_NANOS = 16_666_667L; // ~60fps
    private static final long POLL_NANOS = 1_000_000L;
    private static final int MAX_CONSECUTIVE_TICK_FAILURES = 10;
    private static final long STOP_JOIN_TIMEOUT_MILLIS = 500L;

    private final TickAccumulator tickAccumulator = new TickAccumulator(BASE_TICK_NANOS);
    private final TickAccumulator renderAccumulator = new TickAccumulator(RENDER_INTERVAL_NANOS);
    private final Runnable onTick;
    private final Runnable onRender;

    private volatile double speedMultiplier = TickSpeed.NORMAL.multiplier();
    private volatile boolean running = false;
    // Identifies which thread run() is allowed to keep looping on - see start()'s javadoc for
    // why this, not just `running`, is what a restart needs to be race-free.
    private volatile Thread thread;
    private long tickNumber = 0;
    private int consecutiveTickFailures = 0;
    // Written and read on the game-loop thread, immediately before each onRender callback.
    // Kept volatile because it is also readable from outside the loop (see the accessor).
    private volatile double tickInterpolationAlpha = 0.0;
    // A clock for cosmetic, non-gameplay animation (e.g. the path's moving markers - see
    // td.ui.PathMarkerFrameBuilder) that by design keeps advancing at its own pace while
    // TickSpeed.PAUSED or fast-forwarding, unlike tickInterpolationAlpha above. Volatile for
    // the same reason: written on the loop thread, readable from outside it.
    private volatile double animationSeconds = 0.0;

    public GameLoop(Runnable onTick, Runnable onRender) {
        this.onTick = onTick;
        this.onRender = onRender;
    }

    /**
     * How far past the last completed simulation tick the loop currently is, as a fraction
     * of one tick step ({@code [0, 1)}). Intended for interpolating a render frame between
     * the previous and current tick's state, and refreshed immediately before each onRender
     * callback so a frame built there reads the value belonging to that frame.
     */
    public double tickInterpolationAlpha() {
        return this.tickInterpolationAlpha;
    }

    /**
     * Elapsed time, in seconds, for driving cosmetic animation that should not freeze while
     * paused or race ahead while fast-forwarding. Advances by wall-clock time scaled through
     * {@link #animationTimeScale(double)}, which today ignores tick speed entirely - see that
     * method to change how (or whether) game speed influences the rate.
     */
    public double animationSeconds() {
        return this.animationSeconds;
    }

    /**
     * How fast {@link #animationSeconds} advances relative to wall-clock time, given the
     * current tick-speed multiplier. {@code 1.0} means ignore game speed entirely, which is
     * today's behaviour: markers keep the same pace while paused and while fast-forwarding.
     * Swap the body for {@code multiplier} to make animation track game speed exactly, or
     * something like {@code Math.sqrt(multiplier)} for a damped middle ground.
     */
    private static double animationTimeScale(double multiplier) {
        return 1.0;
    }

    public void setSpeed(TickSpeed speed) {
        LOG.info("Tick speed changed: {} -> {}x", speed, speed.multiplier());
        this.speedMultiplier = speed.multiplier();
    }

    /**
     * Starts the loop on a new daemon thread. Idempotent (a second call while already running
     * is a no-op, logged and ignored, rather than spawning a second thread ticking the same
     * engine) and restartable after {@link #stop()} - returning to the level-select menu and
     * then starting another level calls this a second time on the same instance, so a fresh
     * start must not carry over the previous run's stale progress: the tick/render
     * accumulators, interpolation alpha, tick counter and consecutive-failure circuit breaker
     * are all reset here. {@code animationSeconds} is deliberately left alone - it is
     * wall-clock cosmetic animation with no level semantics, not simulation state.
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
        Thread newThread = new Thread(this, "game-loop");
        newThread.setDaemon(true);
        this.thread = newThread;
        newThread.start();
    }

    /**
     * Stops the loop and <strong>waits for the in-flight tick to finish</strong> before
     * returning. The join is the point: a caller that stops the loop in order to tear the
     * current level down (see {@code TowerDefense.startSelectedLevel}) would otherwise clear
     * the rosters and swap the cell grid, board geometry and path out from under a tick still
     * running. {@code GameEngine.loadLevel} being idempotent does not cover that - idempotency
     * is about calling twice, not about calling concurrently.
     * <p>
     * Bounded rather than indefinite, and skipped when called from the loop thread itself (the
     * circuit breaker's path), so a wedged tick cannot deadlock the EDT.
     */
    public void stop() {
        LOG.info("GameLoop stopping");
        this.running = false;
        Thread loopThread = this.thread;
        if (loopThread == null || loopThread == Thread.currentThread()) {
            return;
        }
        try {
            loopThread.join(STOP_JOIN_TIMEOUT_MILLIS);
            if (loopThread.isAlive()) {
                LOG.warn("game-loop thread still running {}ms after stop(); continuing without it",
                        STOP_JOIN_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for the game-loop thread to stop");
        }
    }

    @Override
    public void run() {
        // Pins this run() to the thread start() launched it on, so a thread still finishing
        // its last iteration when a fast stop()+start() replaces it cannot mistake the new
        // run's `running = true` for its own and keep looping as an unwanted second loop.
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
                    // Fell behind schedule (e.g. a debugger pause or a long GC) - run one
                    // tick and resync rather than bursting through the whole backlog at
                    // once, matching how the original loop handled falling behind.
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

    private static void sleepQuietly(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000L, (int) (nanos % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
