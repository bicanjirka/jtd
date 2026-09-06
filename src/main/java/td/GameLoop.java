package td;

import javax.swing.*;

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

    private static final long BASE_TICK_NANOS = 50_000_000L;
    private static final long RENDER_INTERVAL_NANOS = 16_666_667L; // ~60fps
    private static final long POLL_NANOS = 1_000_000L;

    private final TickAccumulator tickAccumulator = new TickAccumulator(BASE_TICK_NANOS);
    private final TickAccumulator renderAccumulator = new TickAccumulator(RENDER_INTERVAL_NANOS);
    private final Runnable onTick;
    private final Runnable onRender;

    private volatile double speedMultiplier = TickSpeed.NORMAL.multiplier();
    private volatile boolean running = true;

    public GameLoop(Runnable onTick, Runnable onRender) {
        this.onTick = onTick;
        this.onRender = onRender;
    }

    public void setSpeed(TickSpeed speed) {
        this.speedMultiplier = speed.multiplier();
    }

    /**
     * Starts the loop on a new daemon thread.
     */
    public void start() {
        Thread thread = new Thread(this, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
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
            if (multiplier > 0.0) {
                int ticks = this.tickAccumulator.accumulate((long) (elapsedNanos * multiplier));
                if (ticks > 1) {
                    // Fell behind schedule (e.g. a debugger pause or a long GC) - run one
                    // tick and resync rather than bursting through the whole backlog at
                    // once, matching how the original loop handled falling behind.
                    ticks = 1;
                    this.tickAccumulator.reset();
                }
                for (int i = 0; i < ticks; i++) {
                    this.onTick.run();
                }
            }

            if (this.renderAccumulator.accumulate(elapsedNanos) > 0) {
                SwingUtilities.invokeLater(this.onRender);
            }

            sleepQuietly(POLL_NANOS);
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
