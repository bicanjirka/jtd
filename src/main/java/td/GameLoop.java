package td;

import javax.swing.SwingUtilities;

/**
 * Drives the simulation on its own dedicated thread using a fixed-timestep
 * {@link TickAccumulator}: {@code onTick} runs a variable number of times
 * per real-world interval - zero while paused ({@link TickSpeed#PAUSED}),
 * several in a row when running fast - while {@code onRender} is requested
 * via {@link SwingUtilities#invokeLater} at most once per loop iteration,
 * after every tick due that iteration has already run. Posting to the EDT
 * only once the whole tick batch is complete is the standard safe-publication
 * idiom for handing background-thread state to Swing, rather than relying on
 * {@code JComponent.repaint()}'s internal synchronization as an
 * implementation-detail coincidence.
 */
public class GameLoop implements Runnable {

    private static final long BASE_TICK_NANOS = 50_000_000L;
    private static final long POLL_NANOS = 1_000_000L;

    private final TickAccumulator accumulator = new TickAccumulator(BASE_TICK_NANOS);
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
                int ticks = this.accumulator.accumulate((long) (elapsedNanos * multiplier));
                if (ticks > 1) {
                    // Fell behind schedule (e.g. a debugger pause or a long GC) - run one
                    // tick and resync rather than bursting through the whole backlog at
                    // once, matching how the original loop handled falling behind.
                    ticks = 1;
                    this.accumulator.reset();
                }
                for (int i = 0; i < ticks; i++) {
                    this.onTick.run();
                }
                if (ticks > 0) {
                    SwingUtilities.invokeLater(this.onRender);
                }
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
