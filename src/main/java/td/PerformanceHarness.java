package td;

import td.cell.CellGrid;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.ui.BoardRenderer;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * Headless performance run: fills every buildable cell of the last built-in level with towers,
 * plays it to the end several times, and reports tick time, frame-build time and allocation per
 * frame. A tool run on demand, not a test.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class PerformanceHarness {

    private static final long RANDOM_SEED = 20260917L;
    private static final int TICKS_PER_PASS = 20_000;
    // The first pass runs mostly interpreted; it would measure the JIT, not the game.
    private static final int WARM_UP_PASSES = 1;
    private static final int MEASURED_PASSES = 5;
    private static final int GRANTED_CREDITS = 10_000_000;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final int BYTES_PER_KB = 1024;
    // Budgets: about 3x the measured p99, and one tick at super-fast speed is 3 ms.
    private static final double TICK_P99_BUDGET_MILLIS = 1.0;
    private static final double FRAME_P99_BUDGET_MILLIS = 1.0;
    private static final long FRAME_ALLOC_P99_BUDGET_KB = 512;

    private final com.sun.management.ThreadMXBean threads =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private final long[] tickNanos = new long[TICKS_PER_PASS * MEASURED_PASSES];
    private final long[] frameNanos = new long[TICKS_PER_PASS * MEASURED_PASSES];
    private final long[] frameBytes = new long[TICKS_PER_PASS * MEASURED_PASSES];
    private int measured = 0;
    private int peakEnemies = 0;
    private int towerCount = 0;

    public static void main(String[] args) {
        LevelDefinition level = new BuiltInLevelCatalog().levels().getLast();
        if (!new PerformanceHarness().run(level)) {
            System.exit(1);
        }
    }

    /** Plays the passes, prints the report and returns whether every p99 is within budget. */
    public boolean run(LevelDefinition level) {
        for (int pass = 0; pass < WARM_UP_PASSES + MEASURED_PASSES; pass++) {
            this.playPass(level, pass >= WARM_UP_PASSES);
        }

        System.out.println("=== Performance report: " + level.name() + " ===");
        System.out.println("Towers: " + this.towerCount + ", peak alive enemies: " + this.peakEnemies
                + ", measured ticks: " + this.measured);
        boolean tickOk = printMillis("Tick", Arrays.copyOf(this.tickNanos, this.measured), TICK_P99_BUDGET_MILLIS);
        boolean frameOk = printMillis("Frame build", Arrays.copyOf(this.frameNanos, this.measured),
                FRAME_P99_BUDGET_MILLIS);
        long[] bytes = Arrays.copyOf(this.frameBytes, this.measured);
        Arrays.sort(bytes);
        long allocP99Kb = percentile(bytes, 0.99) / BYTES_PER_KB;
        boolean allocOk = allocP99Kb <= FRAME_ALLOC_P99_BUDGET_KB;
        System.out.printf("%-13s p50 %6d KB  p99 %6d KB  max %6d KB  budget p99 %d KB %s%n", "Frame alloc",
                percentile(bytes, 0.50) / BYTES_PER_KB, allocP99Kb, bytes[bytes.length - 1] / BYTES_PER_KB,
                FRAME_ALLOC_P99_BUDGET_KB, verdict(allocOk));
        return tickOk && frameOk && allocOk;
    }

    private void playPass(LevelDefinition level, boolean record) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(RANDOM_SEED));
        BoardRenderer renderer = new BoardRenderer(engine.getGameWorld());
        engine.loadLevel(level);
        engine.debugGrantCredits(GRANTED_CREDITS);
        this.towerCount = fillBoard(engine);

        for (int t = 1; t <= TICKS_PER_PASS && !engine.outcome().isOver(); t++) {
            engine.nextWave();
            long tickStart = System.nanoTime();
            engine.doTick(t);
            long tickEnd = System.nanoTime();
            long bytesBefore = this.threads.getCurrentThreadAllocatedBytes();
            long frameStart = System.nanoTime();
            renderer.buildFrame(t, 0.5, t / (double) TickRate.TICKS_PER_SECOND);
            long frameEnd = System.nanoTime();
            long bytesAfter = this.threads.getCurrentThreadAllocatedBytes();
            if (record) {
                this.tickNanos[this.measured] = tickEnd - tickStart;
                this.frameNanos[this.measured] = frameEnd - frameStart;
                this.frameBytes[this.measured] = bytesAfter - bytesBefore;
                this.measured++;
                this.peakEnemies = Math.max(this.peakEnemies, engine.getGameWorld().enemies().aliveCount());
            }
        }
    }

    /** Places towers on every buildable cell, cycling through every tower type. */
    private static int fillBoard(GameEngine engine) {
        CellGrid grid = engine.cells();
        int scale = engine.getGameWorld().getBoard().scale();
        TowerFactory.Type[] types = TowerFactory.Type.values();
        int placed = 0;
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                if (!grid.at(x, y).buildable()) {
                    continue;
                }
                engine.startPlacing(types[placed % types.length], 0f);
                engine.mouseClicked(x * scale + scale / 2, y * scale + scale / 2);
                if (grid.at(x, y).hasTower()) {
                    placed++;
                }
            }
        }
        return placed;
    }

    private static boolean printMillis(String label, long[] nanos, double p99BudgetMillis) {
        Arrays.sort(nanos);
        double p99Millis = percentile(nanos, 0.99) / NANOS_PER_MILLI;
        boolean ok = p99Millis <= p99BudgetMillis;
        System.out.printf("%-13s p50 %6.3f ms  p99 %6.3f ms  max %6.3f ms  budget p99 %.1f ms %s%n", label,
                percentile(nanos, 0.50) / NANOS_PER_MILLI, p99Millis, nanos[nanos.length - 1] / NANOS_PER_MILLI,
                p99BudgetMillis, verdict(ok));
        return ok;
    }

    private static String verdict(boolean ok) {
        return ok ? "OK" : "OVER BUDGET";
    }

    private static long percentile(long[] sorted, double p) {
        return sorted[(int) Math.min(sorted.length - 1, Math.round(p * (sorted.length - 1)))];
    }
}
