package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

/**
 * Drives the simulation on its own dedicated thread using two independent
 * fixed-timestep {@link TickAccumulator}s: {@code onTick} runs a variable
 * number of times per real-world interval - zero while paused
 * ({@link TickSpeed#PAUSED}), several in a row when running fast - while
 * {@code onRender} is requested on a flat ~60fps real-time cadence,
 * regardless of tick speed or pause, so the board (tower placement
 * highlights, hover effects, ...) keeps redrawing even while the simulation
 * itself is paused or running fast.
 * <p>
 * Both are requested via {@link SwingUtilities#invokeLater}, which is the
 * standard safe-publication idiom for handing background-thread state to
 * Swing, rather than relying on {@code JComponent.repaint()}'s internal
 * synchronization as an implementation-detail coincidence.
 */
public class GameLoop implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);

    private static final long BASE_TICK_NANOS = 50_000_000L;
    private static final long RENDER_INTERVAL_NANOS = 16_666_667L; // ~60fps
    private static final long POLL_NANOS = 1_000_000L;
    private static final int MAX_CONSECUTIVE_TICK_FAILURES = 10;

    private final TickAccumulator tickAccumulator = new TickAccumulator(BASE_TICK_NANOS);
    private final TickAccumulator renderAccumulator = new TickAccumulator(RENDER_INTERVAL_NANOS);
    private final Runnable onTick;
    private final Runnable onRender;

    private volatile double speedMultiplier = TickSpeed.NORMAL.multiplier();
    private volatile boolean running = true;
    private long tickNumber = 0;
    private int consecutiveTickFailures = 0;
    // Written on the game-loop thread immediately before each render request, read on the
    // EDT inside the onRender callback - a plain volatile snapshot is the safe-publication
    // idiom here, matching how render requests themselves cross threads (see class javadoc).
    private volatile double tickInterpolationAlpha = 0.0;
    // A clock for cosmetic, non-gameplay animation (e.g. the path's moving markers - see
    // td.ui.PathMarkerFrameBuilder) that by design keeps advancing at its own pace while
    // TickSpeed.PAUSED or fast-forwarding, unlike tickInterpolationAlpha above. Same
    // safe-publication idiom: written here, read on the EDT during rendering.
    private volatile double animationSeconds = 0.0;

    public GameLoop(Runnable onTick, Runnable onRender) {
        this.onTick = onTick;
        this.onRender = onRender;
    }

    /**
     * How far past the last completed simulation tick the loop currently is, as a fraction
     * of one tick step ({@code [0, 1)}). Intended for interpolating a render frame between
     * the previous and current tick's state; safe to call from the EDT inside an onRender
     * callback, where it reflects the value snapshotted just before that render was requested.
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
     * Starts the loop on a new daemon thread.
     */
    public void start() {
        LOG.info("GameLoop starting");
        Thread thread = new Thread(this, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        LOG.info("GameLoop stopping");
        this.running = false;
    }

    @Override
    public void run() {
        long lastNanos = System.nanoTime();
        while (this.running) {
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
                SwingUtilities.invokeLater(this::runRender);
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
