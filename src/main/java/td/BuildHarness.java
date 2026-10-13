package td;

import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Compares builds of one tower type: a few copies of it, all bought the same way, among a fixed
 * background of the other towers. Reports lives lost, what the tower dealt and what the build cost, for every single node (with what leads to it) and for every combination of a head's last
 * node, an extra chain and a special. A tool run on demand, not a test.
 * <p>
 * Usage: {@code BuildHarness [levelIndex] [seeds] [towerType] [copies] [background] [budget] [upgraded]}.
 * With a budget, every build is bought on the best {@code upgraded} copies and the rest of the copies are plain, as
 * many as that many credits buy (a copy costs its list price plus the surcharge for the copies before it), so builds
 * are compared at equal spend, the way a player chooses between upgrading and building more.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class BuildHarness {

    private static final long FIRST_SEED = 20260917L;
    private static final int GRANTED_CREDITS = 10_000_000;
    /** So the level is played to its end whatever leaks: the number of lives lost is the score. */
    private static final int UNLIMITED_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 60_000;
    private static final double PER_HUNDRED_CREDITS = 100.0;
    private static final String EXTRA_MARK = ".extra.";

    private final LevelDefinition level;
    private final int seeds;
    private final TowerFactory.Type type;
    private final int copies;
    private final int background;
    private final int budget;
    private final int upgraded;

    private BuildHarness(LevelDefinition level, int seeds, TowerFactory.Type type, int copies, int background,
                         int budget, int upgraded) {
        this.budget = budget;
        this.upgraded = upgraded;
        this.level = level;
        this.seeds = seeds;
        this.type = type;
        this.copies = copies;
        this.background = background;
    }

    public static void main(String[] args) {
        int levelIndex = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        TowerFactory.Type type = TowerFactory.Type.valueOf((args.length > 2 ? args[2] : "cinder").toUpperCase(Locale.ROOT));
        int copies = args.length > 3 ? Integer.parseInt(args[3]) : 4;
        int background = args.length > 4 ? Integer.parseInt(args[4]) : 2;
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(levelIndex).withStartingLives(UNLIMITED_LIVES);
        int budget = args.length > 5 ? Integer.parseInt(args[5]) : 0;
        int upgraded = args.length > 6 ? Integer.parseInt(args[6]) : 1;
        new BuildHarness(level, seeds, type, copies, background, budget, upgraded).report();
    }

    private void report() {
        UpgradeTree tree = this.sampleTower().upgradeTree();
        List<UpgradeNode> nodes = tree.nodes();
        Result baseline = this.measure(List.of(), this.copiesFor(List.of()));
        System.out.printf("=== Build report: %s, %d seeds, %d x %s among %d of each other tower ===%n", this.level.name(),
                this.seeds, this.copies, this.type.displayName(), this.background);
        System.out.println(Result.header());
        System.out.println(baseline.format("plain", baseline));

        System.out.println();
        System.out.println("-- each node alone, with everything that leads to it --");
        List<Row> alone = new ArrayList<>();
        for (UpgradeNode node : nodes) {
            alone.add(this.row(node.displayName() + " (" + node.id() + ")", List.of(node), tree));
        }
        print(alone, baseline);

        System.out.println();
        System.out.println("-- a head's last node + an extra chain + a special --");
        List<Row> combos = new ArrayList<>();
        for (UpgradeNode head : this.lastHeads(nodes)) {
            for (UpgradeNode extra : this.withNone(this.lastExtras(nodes))) {
                for (UpgradeNode special : this.withNone(nodes.stream().filter(n -> n.slot() == UpgradeSlot.SPECIAL).toList())) {
                    List<UpgradeNode> targets = new ArrayList<>(List.of(head));
                    if (extra != null) {
                        targets.add(extra);
                    }
                    if (special != null) {
                        targets.add(special);
                    }
                    String label = head.displayName() + (extra == null ? "" : " + " + extra.displayName())
                            + (special == null ? "" : " + " + special.displayName());
                    combos.add(this.row(label, targets, tree));
                }
            }
        }
        print(combos, baseline);
    }

    private void print(List<Row> rows, Result baseline) {
        System.out.println(Result.header());
        Comparator<Row> order = this.budget > 0 ? Comparator.comparingDouble((Row row) -> row.result.livesLost())
                : Comparator.comparingDouble((Row row) -> row.value(baseline)).reversed();
        rows.stream().sorted(order)
                .forEach(row -> System.out.println(row.result.format(row.label, baseline)));
    }

    private List<UpgradeNode> withNone(List<UpgradeNode> options) {
        List<UpgradeNode> all = new ArrayList<>();
        all.add(null);
        all.addAll(options);
        return all;
    }

    /** Head nodes nothing else in the tree builds on, the ends of the head chains. */
    private List<UpgradeNode> lastHeads(List<UpgradeNode> nodes) {
        return nodes.stream().filter(n -> n.slot() == UpgradeSlot.HEAD && !n.id().contains(EXTRA_MARK))
                .filter(n -> nodes.stream().noneMatch(other -> other.requires().requiredNodes().contains(n.id())))
                .toList();
    }

    private List<UpgradeNode> lastExtras(List<UpgradeNode> nodes) {
        return nodes.stream().filter(n -> n.id().contains(EXTRA_MARK))
                .filter(n -> nodes.stream().noneMatch(other -> other.requires().requiredNodes().contains(n.id())))
                .toList();
    }

    private Row row(String label, List<UpgradeNode> targets, UpgradeTree tree) {
        List<UpgradeNode> build = buyOrder(targets, tree);
        return new Row(label, this.measure(build, this.copiesFor(build)));
    }

    /** How many copies the budget buys when {@code build} goes on the first few of them; the fixed number without one. */
    private int copiesFor(List<UpgradeNode> build) {
        if (this.budget <= 0) {
            return this.copies;
        }
        int upgradedCount = build.isEmpty() ? 0 : this.upgraded;
        int spent = upgradedCount * build.stream().mapToInt(UpgradeNode::price).sum();
        int count = 0;
        while (spent + this.type.priceFor(count) <= this.budget || count < upgradedCount) {
            spent += this.type.priceFor(count);
            count++;
        }
        return count;
    }

    /** The nodes to buy for {@code targets}, in order, each once, with what leads to them first. */
    private static List<UpgradeNode> buyOrder(List<UpgradeNode> targets, UpgradeTree tree) {
        Map<String, UpgradeNode> order = new LinkedHashMap<>();
        for (UpgradeNode target : targets) {
            for (UpgradeNode step : tree.pathTo(target)) {
                order.putIfAbsent(step.id(), step);
            }
        }
        return List.copyOf(order.values());
    }

    private Tower sampleTower() {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(FIRST_SEED));
        engine.loadLevel(this.level);
        engine.debugGrantCredits(GRANTED_CREDITS);
        return HarnessBoard.place(engine, this.type, HarnessBoard.rankCells(engine).getFirst()).orElseThrow();
    }

    /** Plays every seed, in parallel, with {@code build} bought on each copy; the means. */
    private Result measure(List<UpgradeNode> build, int copyCount) {
        List<Run> runs = IntStream.range(0, this.seeds).parallel().mapToObj(i -> this.play(FIRST_SEED + i, build, copyCount)).toList();
        return new Result(
                runs.stream().mapToInt(Run::livesLost).average().orElse(0),
                runs.stream().mapToLong(Run::damage).average().orElse(0),
                runs.stream().mapToInt(Run::kills).average().orElse(0),
                runs.stream().mapToInt(Run::cost).max().orElse(0), copyCount);
    }

    private Run play(long seed, List<UpgradeNode> build, int copyCount) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(this.level);
        engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
        engine.debugGrantCredits(GRANTED_CREDITS);
        List<HarnessBoard.Cell> cells = HarnessBoard.rankCells(engine);
        int next = 0;
        List<Tower> subjects = new ArrayList<>();
        for (int i = 0; i < copyCount; i++) {
            Optional<Tower> placed = Optional.empty();
            while (placed.isEmpty() && next < cells.size()) {
                placed = HarnessBoard.place(engine, this.type, cells.get(next++));
            }
            placed.ifPresent(subjects::add);
        }
        for (int round = 0; round < this.background; round++) {
            for (TowerFactory.Type other : TowerFactory.Type.values()) {
                Optional<Tower> placed = Optional.empty();
                while (other != this.type && placed.isEmpty() && next < cells.size()) {
                    placed = HarnessBoard.place(engine, other, cells.get(next++));
                }
            }
        }
        int before = engine.getGameWorld().economy().getCredits();
        for (Tower subject : subjects.subList(0, Math.min(subjects.size(), this.budget > 0 ? this.upgraded : subjects.size()))) {
            for (UpgradeNode node : build) {
                if (!subject.buyUpgrade(node)) {
                    throw new IllegalStateException("Could not buy " + node.id() + " on " + this.type);
                }
            }
        }
        int cost = before - engine.getGameWorld().economy().getCredits();

        for (int t = 1; t <= TICK_BUDGET && !engine.outcome().isOver(); t++) {
            engine.nextWave();
            engine.doTick(t);
        }
        long damage = subjects.stream().mapToLong(Tower::getDamageDealt).sum();
        int kills = subjects.stream().mapToInt(Tower::getKillCount).sum();
        return new Run(UNLIMITED_LIVES - engine.getGameWorld().economy().getLives(), damage, kills, cost);
    }

    private record Run(int livesLost, long damage, int kills, int cost) {
    }

    private record Row(String label, Result result) {

        double value(Result baseline) {
            return this.result.value(baseline);
        }
    }

    /** Means over the seeds; {@code cost} is what the build costs all the copies together. */
    private record Result(double livesLost, double damage, double kills, int cost, int copies) {

        static String header() {
            return String.format("  %-62s %3s %6s %9s %7s %10s %7s %8s", "build", "n", "cost", "lives lost", "saved",
                    "dmg dealt", "kills", "per 100");
        }

        double value(Result baseline) {
            return this.cost <= 0 ? 0 : (baseline.livesLost - this.livesLost) / this.cost * PER_HUNDRED_CREDITS;
        }

        String format(String label, Result baseline) {
            return String.format("  %-62s %3d %6d %9.1f %7.1f %10.0f %7.1f %8.2f",
                    label.length() > 62 ? label.substring(0, 62) : label, this.copies, this.cost, this.livesLost, baseline.livesLost - this.livesLost, this.damage, this.kills,
                    this.value(baseline));
        }
    }
}
