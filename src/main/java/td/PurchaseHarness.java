package td;

import td.FairPriceTable.MeasuredCopy;
import td.FairPriceTable.MeasuredLine;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Compares purchases on levels: how many lives a fixed army loses with and without each one, per
 * credit it costs. The army is a few towers of every type (one unless asked) on the best cells for reaching the path; a
 * purchase is another tower of a type, or an upgrade node with everything that leads to it
 * ({@link td.tower.upgrade.UpgradeTree#pathTo}). Each line's fair price is the {@link FairPriceTable}'s, and a table of
 * what each tier should cost closes the report. Every run is seeded, so a table can be compared with
 * the next one. A tool run on demand, not a test: it takes minutes.
 * <p>
 * Usage: {@code PurchaseHarness [levelIndexes] [seeds] [towerType] [armyCopies]}, with levels such as {@code 0,1,2}. The
 * price of "another copy" rises with the copies already built, so a larger army measures it at the price a real build
 * pays.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class PurchaseHarness {

    private static final long FIRST_SEED = 20260917L;
    private static final int DEFAULT_SEEDS = 6;
    private static final int GRANTED_CREDITS = 10_000_000;
    /** So the level is played to its end whatever leaks: the number of lives lost is the score. */
    private static final int UNLIMITED_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 60_000;
    private static final double PER_HUNDRED_CREDITS = 100.0;

    private final int seeds;
    private final int armyCopies;

    private PurchaseHarness(int seeds, int armyCopies) {
        this.seeds = seeds;
        this.armyCopies = armyCopies;
    }

    static void main(String[] args) {
        List<Integer> levelIndexes = args.length > 0
                ? Arrays.stream(args[0].split(",")).map(Integer::parseInt).toList() : List.of(0);
        int seeds = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_SEEDS;
        Optional<TowerFactory.Type> only = args.length > 2
                ? Optional.of(TowerFactory.Type.valueOf(args[2].toUpperCase(Locale.ROOT))) : Optional.empty();
        int armyCopies = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        List<LevelDefinition> all = new BuiltInLevelCatalog().levels();
        PurchaseHarness harness = new PurchaseHarness(seeds, armyCopies);
        List<Map<UpgradeTier, Double>> byLevel = levelIndexes.stream()
                .map(index -> harness.report(all.get(index).withStartingLives(UNLIMITED_LIVES), only)).toList();
        printTiers(byLevel);
    }

    /** Prints one level's purchases; what each tier should cost there, empty when no other tower saves a life. */
    private Map<UpgradeTier, Double> report(LevelDefinition level, Optional<TowerFactory.Type> only) {
        double baseline = this.meanLeaks(level, (engine, army, spare) -> 0);
        System.out.printf(Locale.ROOT, "=== Purchase report: %s, %d seeds, army of %d of each tower ===%n", level.name(), this.seeds,
                this.armyCopies);
        System.out.printf(Locale.ROOT, "Lives lost with no purchase: %.1f%n", baseline);
        Map<TowerFactory.Type, Row> copies = new EnumMap<>(TowerFactory.Type.class);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            copies.put(type, this.row(level, type.displayName() + " (another copy)", baseline, buildCopy(type)));
        }
        OptionalDouble livesPerCredit = FairPriceTable.livesPerCredit(copies.values().stream()
                .map(copy -> new MeasuredCopy(copy.cost(), baseline - copy.leaks())).toList());
        System.out.printf(Locale.ROOT, "Another tower saves %.2f lives per 100 credits, every type together%n",
                livesPerCredit.orElse(0) * PER_HUNDRED_CREDITS);
        Optional<FairPriceTable> table = livesPerCredit.isPresent()
                ? Optional.of(new FairPriceTable(livesPerCredit.getAsDouble())) : Optional.empty();

        List<MeasuredLine> lines = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            if (only.isPresent() && only.get() != type) {
                continue;
            }
            Row copy = copies.get(type);
            List<Row> rows = new ArrayList<>(List.of(copy));
            Tower sample = this.sampleOf(level, type);
            for (UpgradeNode node : sample.upgradeTree().nodes()) {
                Row row = this.row(level, node.displayName() + " (" + node.id() + ")", baseline, buyNode(type, node));
                rows.add(row);
                Set<String> steps = sample.upgradeTree().pathTo(node).stream().map(UpgradeNode::id).collect(Collectors.toSet());
                lines.add(new MeasuredLine(type, node.tier(), steps, baseline - row.leaks()));
            }
            System.out.println();
            System.out.println(type.displayName()
                    + ": value is lives saved per 100 credits; vs copy is that over the copy's; fair is the price that equals another tower");
            rows.stream().sorted(Comparator.comparingDouble(Row::value).reversed())
                    .forEach(row -> System.out.println(row.format(copy.value(), table)));
        }
        return table.map(prices -> prices.tierPercents(lines)).orElse(Map.of());
    }

    /** What each tier costs now and should cost on each level, as a percent of its tower's list price. */
    private static void printTiers(List<Map<UpgradeTier, Double>> byLevel) {
        System.out.println();
        System.out.println("Fair price of a node at each tier, in percent of its tower's list price: now, each level, their mean");
        for (UpgradeTier tier : UpgradeTier.values()) {
            List<Double> fair = byLevel.stream().filter(level -> level.containsKey(tier)).map(level -> level.get(tier)).toList();
            String levels = fair.stream().map(percent -> String.format(Locale.ROOT, "%6.0f", percent)).collect(Collectors.joining());
            System.out.printf(Locale.ROOT, "  %-13s %6d %s  mean %6.0f%n", tier, tier.price(100), levels,
                    fair.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }
    }

    private Row row(LevelDefinition level, String label, double baseline, Purchase purchase) {
        AtomicInteger cost = new AtomicInteger(-1);
        double leaks = this.meanLeaks(level, (engine, army, spare) -> {
            cost.set(purchase.apply(engine, army, spare));
            return cost.get();
        });
        return new Row(label, cost.get(), leaks, baseline);
    }

    /** A tower of {@code type} on a throwaway engine, to read its tree off. */
    private Tower sampleOf(LevelDefinition level, TowerFactory.Type type) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(FIRST_SEED));
        engine.loadLevel(level);
        engine.debugGrantCredits(GRANTED_CREDITS);
        List<Tower> army = placeArmy(engine, HarnessBoard.rankCells(engine), 1);
        return army.stream().filter(tower -> tower.getType() == type).findFirst().orElseThrow();
    }

    /** Mean lives lost over the seeds; {@code setup} spends on the purchase and returns what it cost, or -1. */
    private double meanLeaks(LevelDefinition level, Purchase setup) {
        return IntStream.range(0, this.seeds).parallel()
                .mapToDouble(i -> play(level, FIRST_SEED + i, this.armyCopies, setup)).average().orElse(0);
    }

    private static int play(LevelDefinition level, long seed, int armyCopies, Purchase setup) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(level);
        engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
        engine.debugGrantCredits(GRANTED_CREDITS);
        List<HarnessBoard.Cell> cells = HarnessBoard.rankCells(engine);
        List<Tower> army = placeArmy(engine, cells, armyCopies);
        setup.apply(engine, army, cells.subList(army.size(), cells.size()));

        for (int t = 1; t <= TICK_BUDGET && !engine.outcome().isOver(); t++) {
            engine.nextWave();
            engine.doTick(t);
        }
        return UNLIMITED_LIVES - engine.getGameWorld().economy().getLives();
    }

    /** {@code copies} towers of every type on the best cells, a round of the types at a time. */
    private static List<Tower> placeArmy(GameEngine engine, List<HarnessBoard.Cell> cells, int copies) {
        List<Tower> army = new ArrayList<>();
        int next = 0;
        for (int round = 0; round < copies; round++) {
            for (TowerFactory.Type type : TowerFactory.Type.values()) {
                Optional<Tower> placed = Optional.empty();
                while (placed.isEmpty() && next < cells.size()) {
                    placed = HarnessBoard.place(engine, type, cells.get(next++));
                }
                placed.ifPresent(army::add);
            }
        }
        return army;
    }

    private static Purchase buildCopy(TowerFactory.Type type) {
        return (engine, army, spare) -> {
            int before = engine.getGameWorld().economy().getCredits();
            for (HarnessBoard.Cell cell : spare) {
                if (HarnessBoard.place(engine, type, cell).isPresent()) {
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

    /** What a purchase did to the army: spends on it and returns the cost, or -1 when it could not be made. */
    private interface Purchase {

        int apply(GameEngine engine, List<Tower> army, List<HarnessBoard.Cell> spare);
    }

    private record Row(String label, int cost, double leaks, double baseline) {

        /** Lives saved per 100 credits; zero for a purchase that could not be made. */
        double value() {
            return this.cost <= 0 ? 0 : (this.baseline - this.leaks) / this.cost * PER_HUNDRED_CREDITS;
        }

        String format(double copyValue, Optional<FairPriceTable> table) {
            if (this.cost <= 0) {
                return String.format(Locale.ROOT, "  %-58s cannot be bought", this.label);
            }
            String versus = copyValue > 0 ? String.format(Locale.ROOT, "%5.2fx", this.value() / copyValue) : "    - ";
            String fair = table.map(prices -> String.format(Locale.ROOT, "%6.0f", prices.linePrice(this.baseline - this.leaks)))
                    .orElse("     -");
            return String.format(Locale.ROOT, "  %-58s cost %5d  lives lost %7.1f  saved %7.1f  value %6.2f  vs copy %s  fair %s",
                    this.label, this.cost, this.leaks, this.baseline - this.leaks, this.value(), versus, fair);
        }
    }
}
