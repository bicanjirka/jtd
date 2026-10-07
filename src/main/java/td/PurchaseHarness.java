package td;

import td.cell.CellGrid;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Compares purchases on one level: how many lives a fixed army loses with and without each one, per
 * credit it costs. The army is one tower of every type on the best cells for reaching the path; a
 * purchase is another tower of a type, or an upgrade node with everything that leads to it
 * ({@link td.tower.upgrade.UpgradeTree#pathTo}). Every run is seeded, so a table can be compared with
 * the next one. A tool run on demand, not a test: it takes minutes.
 * <p>
 * Usage: {@code PurchaseHarness [levelIndex] [seeds] [towerType]}.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class PurchaseHarness {

    private static final long FIRST_SEED = 20260917L;
    private static final int DEFAULT_SEEDS = 6;
    private static final int GRANTED_CREDITS = 10_000_000;
    /** So the level is played to its end whatever leaks: the number of lives lost is the score. */
    private static final int UNLIMITED_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 60_000;
    /** A cell is worth the stretch of path within this many cells of it. */
    private static final double REACH_CELLS = 3.5;
    private static final double PER_HUNDRED_CREDITS = 100.0;

    private final int seeds;

    private PurchaseHarness(int seeds) {
        this.seeds = seeds;
    }

    public static void main(String[] args) {
        int levelIndex = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        int seeds = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_SEEDS;
        Optional<TowerFactory.Type> only = args.length > 2
                ? Optional.of(TowerFactory.Type.valueOf(args[2].toUpperCase(java.util.Locale.ROOT))) : Optional.empty();
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(levelIndex).withStartingLives(UNLIMITED_LIVES);
        new PurchaseHarness(seeds).report(level, only);
    }

    private void report(LevelDefinition level, Optional<TowerFactory.Type> only) {
        double baseline = this.meanLeaks(level, (engine, army, spare) -> 0);
        System.out.printf("=== Purchase report: %s, %d seeds, army of one of each tower ===%n", level.name(), this.seeds);
        System.out.printf("Lives lost with no purchase: %.1f%n", baseline);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            if (only.isPresent() && only.get() != type) {
                continue;
            }
            List<Row> rows = new ArrayList<>();
            Row copy = this.row(level, type.displayName() + " (another copy)", baseline, buildCopy(type));
            rows.add(copy);
            for (UpgradeNode node : this.nodesOf(level, type)) {
                rows.add(this.row(level, node.displayName(), baseline, buyNode(type, node)));
            }
            System.out.println();
            System.out.println(type.displayName() + ": value is lives saved per 100 credits; vs copy is that over the copy's");
            rows.stream().sorted(Comparator.comparingDouble(Row::value).reversed())
                    .forEach(row -> System.out.println(row.format(copy.value())));
        }
    }

    private Row row(LevelDefinition level, String label, double baseline, Purchase purchase) {
        int[] cost = new int[1];
        double leaks = this.meanLeaks(level, (engine, army, spare) -> {
            cost[0] = purchase.apply(engine, army, spare);
            return cost[0];
        });
        return new Row(label, cost[0], leaks, baseline);
    }

    /** The nodes of {@code type}'s tree, read off a tower of a throwaway engine. */
    private List<UpgradeNode> nodesOf(LevelDefinition level, TowerFactory.Type type) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(FIRST_SEED));
        engine.loadLevel(level);
        engine.debugGrantCredits(GRANTED_CREDITS);
        List<Tower> army = placeArmy(engine, rankCells(engine));
        return army.stream().filter(tower -> tower.getType() == type).findFirst().orElseThrow()
                .upgradeTree().nodes();
    }

    /** Mean lives lost over the seeds; {@code setup} spends on the purchase and returns what it cost, or -1. */
    private double meanLeaks(LevelDefinition level, Purchase setup) {
        double total = 0;
        for (int i = 0; i < this.seeds; i++) {
            total += play(level, FIRST_SEED + i, setup);
        }
        return total / this.seeds;
    }

    private static int play(LevelDefinition level, long seed, Purchase setup) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(level);
        engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
        engine.debugGrantCredits(GRANTED_CREDITS);
        List<Cell> cells = rankCells(engine);
        List<Tower> army = placeArmy(engine, cells);
        setup.apply(engine, army, cells.subList(army.size(), cells.size()));

        for (int t = 1; t <= TICK_BUDGET && !engine.outcome().isOver(); t++) {
            engine.nextWave();
            engine.doTick(t);
        }
        return UNLIMITED_LIVES - engine.getGameWorld().economy().getLives();
    }

    /** Buildable cells, the ones with the most path within reach first. */
    private static List<Cell> rankCells(GameEngine engine) {
        CellGrid grid = engine.cells();
        int scale = engine.getGameWorld().getBoard().scale();
        List<Vec2> pathPoints = engine.getGameWorld().level().paths().stream()
                .flatMap(path -> path.path().points().stream()).toList();
        double reach = REACH_CELLS * scale;
        List<Cell> cells = new ArrayList<>();
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                if (grid.at(x, y).buildable()) {
                    double centreX = x * scale + scale / 2.0;
                    double centreY = y * scale + scale / 2.0;
                    int near = (int) pathPoints.stream()
                            .filter(point -> Math.hypot(point.x() - centreX, point.y() - centreY) <= reach).count();
                    cells.add(new Cell(x, y, near));
                }
            }
        }
        cells.sort(Comparator.comparingInt(Cell::near).reversed().thenComparingInt(Cell::x).thenComparingInt(Cell::y));
        return cells;
    }

    /** One tower of every type on the best cells, in the order of the types. */
    private static List<Tower> placeArmy(GameEngine engine, List<Cell> cells) {
        List<Tower> army = new ArrayList<>();
        int next = 0;
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Optional<Tower> placed = Optional.empty();
            while (placed.isEmpty() && next < cells.size()) {
                placed = place(engine, type, cells.get(next++));
            }
            placed.ifPresent(army::add);
        }
        return army;
    }

    private static Optional<Tower> place(GameEngine engine, TowerFactory.Type type, Cell cell) {
        int scale = engine.getGameWorld().getBoard().scale();
        int pixelX = cell.x() * scale + scale / 2;
        int pixelY = cell.y() * scale + scale / 2;
        engine.startPlacing(type, 0f);
        engine.mouseClicked(pixelX, pixelY);
        if (!engine.cells().at(cell.x(), cell.y()).hasTower()) {
            return Optional.empty();
        }
        return engine.getTowers().stream().filter(tower -> tower.getX() == pixelX && tower.getY() == pixelY).findFirst();
    }

    private static Purchase buildCopy(TowerFactory.Type type) {
        return (engine, army, spare) -> {
            int before = engine.getGameWorld().economy().getCredits();
            for (Cell cell : spare) {
                if (place(engine, type, cell).isPresent()) {
                    return before - engine.getGameWorld().economy().getCredits();
                }
            }
            return -1;
        };
    }

    private static Purchase buyNode(TowerFactory.Type type, UpgradeNode node) {
        return (engine, army, spare) -> {
            Tower tower = army.stream().filter(candidate -> candidate.getType() == type).findFirst().orElseThrow();
            int before = engine.getGameWorld().economy().getCredits();
            for (UpgradeNode step : tower.upgradeTree().pathTo(node)) {
                if (!tower.buyUpgrade(step)) {
                    return -1;
                }
            }
            return before - engine.getGameWorld().economy().getCredits();
        };
    }

    private record Cell(int x, int y, int near) {
    }

    /** What a purchase did to the army: spends on it and returns the cost, or -1 when it could not be made. */
    private interface Purchase {

        int apply(GameEngine engine, List<Tower> army, List<Cell> spare);
    }

    private record Row(String label, int cost, double leaks, double baseline) {

        /** Lives saved per 100 credits; zero for a purchase that could not be made. */
        double value() {
            return this.cost <= 0 ? 0 : (this.baseline - this.leaks) / this.cost * PER_HUNDRED_CREDITS;
        }

        String format(double copyValue) {
            if (this.cost <= 0) {
                return String.format("  %-34s cannot be bought", this.label);
            }
            String versus = copyValue > 0 ? String.format("%5.2fx", this.value() / copyValue) : "    - ";
            return String.format("  %-34s cost %5d  lives lost %7.1f  saved %7.1f  value %6.2f  vs copy %s",
                    this.label, this.cost, this.leaks, this.baseline - this.leaks, this.value(), versus);
        }
    }
}
