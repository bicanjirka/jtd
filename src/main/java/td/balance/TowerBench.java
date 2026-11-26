package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.GameEngine;
import td.enemy.Affix;
import td.enemy.EnemyCatalog;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.level.LevelDefinition;
import td.tower.MortarTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.TickRate;
import td.wave.Point;
import td.wave.WaveScript;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Measures the towers themselves: each build of each tower stands alone beside a straight lane, in the real engine,
 * and the measure is its <em>capacity</em>, the toughest wave of a scenario it lets nothing through of. The wave's
 * shape is fixed and the health of its enemies is searched, so a capacity is health destroyed before anything
 * leaks. It is counted in Walkers: the health of a Walker of the lowest rank, times how much tougher than a Walker
 * the enemy stage found the scenario's enemy to be per point of health. A defence of plain mixed damage therefore
 * holds about as many Walkers in every scenario, and a tower's row shows what it is good and bad against.
 * <p>
 * The builds are read off each tower's tree, so a new tier, node, cluster or tower is measured with no change here:
 * the bare tower; every node alone on the nodes it needs ({@code line}) against those nodes without it; every tier
 * bought whole ({@code full}), one family for each side of a fork and one for each other member of a cluster; and
 * every node taken out of the whole tier it sits in ({@code without}) and out of the whole tree
 * ({@code without-top}). What a tower does for the towers around it is
 * measured beside two fixed partners, and every two towers are measured as a pair against what each holds alone.
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerBench [-Dexec.args="--towers sniper,burst --quick"]
 * </pre>
 *
 * A full run stores {@code towers.tsv}, {@code tower-pairs.tsv}, {@code tower-scenarios.tsv} and
 * {@code tower-bench.html} in {@code target/balance/}; with {@code --towers} it replaces only those towers' rows.
 * {@code --scenarios} and {@code --quick} (one seed, a coarse search) print and store nothing. It reads the enemy
 * stage's {@code enemy-power.tsv}.
 */
public final class TowerBench {

    static final String SHEET = "towers";
    static final String PAIR_SHEET = "tower-pairs";
    static final String SCENARIO_SHEET = "tower-scenarios";
    static final String BARE = "bare";
    static final String LINE = "line";
    static final String BEFORE = "before";
    static final String FULL = "full";
    /** Nodes only a strategy buys: a situational node no tier, cluster or whole-tree build owns, so it never drags them. */
    static final Set<String> STRATEGY_ONLY = Set.of("mortar.flare");
    static final String WITHOUT = "without";
    static final String WITHOUT_AT_TOP = "without-top";
    static final String STRATEGY = "strategy";
    /** What ends the name of a scenario fought beside the partners. */
    static final String BESIDE_MARK = "+";
    private static final String PAGE = "tower-bench";
    private static final String MOB = "benchmob";
    private static final String PLAIN = "walker";
    private static final String SEPARATOR = ":";
    static final Point SUBJECT = new Point(6, 0);
    private static final Point SECOND = new Point(7, 0);
    /** The towers a subject's help is measured on: the two cheapest, one that picks a target and one that hits an area. */
    private static final List<Stand> PARTNERS = List.of(
            new Stand(TowerFactory.Type.SNIPER, new Point(5, 0), List.of()),
            new Stand(TowerFactory.Type.SPLASH, SECOND, List.of()));
    /** The scenarios pairs of towers fight. */
    private static final List<String> PAIRED = List.of("stream", "group", "armored", "hidden");
    /** Five Snipers beside the subject: the focused fire a support's payout is measured on. */
    private static final List<Stand> FOCUS = Stream.of(new Point(3, 0), new Point(4, 0), new Point(5, 0), new Point(7, 0),
            new Point(8, 0)).map(cell -> new Stand(TowerFactory.Type.SNIPER, cell, List.of())).toList();
    private static final double TANK_SPEED = 0.3;
    /** A Sniper beside towers that slow and freeze, as most boards have: what a hit is worth on enemies held in place. */
    private static final List<Stand> CONTROL = List.of(
            new Stand(TowerFactory.Type.PULSE, new Point(4, 0), List.of()),
            new Stand(TowerFactory.Type.SNIPER, new Point(5, 0), List.of()),
            new Stand(TowerFactory.Type.SEEKER, SECOND, List.of()));
    private static final List<Scenario> SCENARIOS = scenarios();
    private static final List<String> SCENARIO_HEADER = List.of("scenario", "wave", "enemy", "rank", "enemies", "walk s", "toughness",
            "walkers a point", "partners", "tests");
    private static final int GROUP = 12;
    private static final int GROUP_SPACING = 7;
    private static final int GRANTED_CREDITS = 10_000_000;
    private static final int TICK_BUDGET = 12_000;
    private static final long FIRST_SEED = 1L;
    private static final int SEEDS = 5;
    private static final double TOLERANCE = 0.02;
    private static final int QUICK_SEEDS = 1;
    private static final double QUICK_TOLERANCE = 0.1;
    private static final double STEP = 1.5;
    private static final double LEAST = 1;
    private static final double MOST = 50_000_000;
    private static final double FIRST_GUESS = 60;
    /** How a build's capacity is guessed to grow with what it cost, to start its search near the answer. */
    private static final double GUESS_GROWTH = 0.7;
    private static final Map<String, RankedEnemy> LADDERS = new ConcurrentHashMap<>();

    private final int seeds;
    private final double tolerance;
    private final List<Scenario> scenarios;
    private final Weights weights;

    TowerBench(int seeds, double tolerance, List<Scenario> scenarios, Weights weights) {
        this.seeds = seeds;
        this.tolerance = tolerance;
        this.scenarios = List.copyOf(scenarios);
        this.weights = weights;
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        Set<String> onlyTowers = option(args, "--towers");
        Set<String> onlyScenarios = option(args, "--scenarios");
        boolean quick = Arrays.asList(args).contains("--quick");
        List<TowerFactory.Type> types = Arrays.stream(TowerFactory.Type.values())
                .filter(type -> onlyTowers.isEmpty() || onlyTowers.contains(type.name().toLowerCase(Locale.ROOT))
                        || onlyTowers.contains(type.displayName().toLowerCase(Locale.ROOT)))
                .toList();
        List<Scenario> scenarios = SCENARIOS.stream()
                .filter(scenario -> onlyScenarios.isEmpty() || onlyScenarios.contains(scenario.name())).toList();
        TowerBench bench = new TowerBench(quick ? QUICK_SEEDS : SEEDS, quick ? QUICK_TOLERANCE : TOLERANCE, scenarios,
                Weights.stored());
        long started = System.nanoTime();

        List<List<String>> rows = new ArrayList<>();
        for (TowerFactory.Type type : types) {
            List<List<String>> measured = bench.measured(type);
            System.out.printf(Locale.ROOT, "%n== %s (%s), %d builds%n", type.displayName(), type, measured.size());
            System.out.println(String.join(" ", bench.printed(bench.header())));
            measured.forEach(row -> System.out.println(String.join(" ", bench.printed(row))));
            rows.addAll(measured);
        }
        List<List<String>> pairs = bench.paired(types);
        System.out.printf(Locale.ROOT, "%n== Pairs%n%s%n", String.join(" ", bench.pairHeader()));
        pairs.forEach(row -> System.out.println(String.join(" ", row)));
        List<List<String>> described = bench.described();
        System.out.printf(Locale.ROOT, "%n== Scenarios%n");
        described.forEach(row -> System.out.println(String.join(" ", row)));
        System.out.printf(Locale.ROOT, "%nMeasured in %d s%n", (System.nanoTime() - started) / 1_000_000_000L);
        if (quick || !onlyScenarios.isEmpty()) {
            System.out.println("A partial or quick run stores nothing.");
            return;
        }
        Set<String> replaced = types.stream().map(Enum::name).collect(Collectors.toSet());
        ReportPage.Table towers = ReportPage.Table.of("Every build of every tower",
                bench.header(), merged(SHEET, bench.header(), rows, row -> replaced.contains(row.getFirst())))
                .noted("""
                        Each scenario column is the build's capacity in Walkers: the toughest wave of that scenario it
                        holds alone with no leak. bare: the tower as bought. line: one node on the nodes it needs,
                        against <i>versus</i>, the same without it. full: every node up to a tier, the first member
                        of each cluster unless the build names another. without: a full tier less one node, against
                        <i>versus</i>, that tier whole; without-top: the same in the whole tree. A column ending in + is fought beside the two partners and
                        counts what all three hold.""");
        ReportPage.Table pairTable = ReportPage.Table.of("Every two towers side by side", bench.pairHeader(),
                merged(PAIR_SHEET, bench.pairHeader(), pairs,
                        row -> replaced.contains(row.get(0)) || replaced.contains(row.get(2))))
                .noted("""
                        What two towers on neighbouring cells hold together, what they hold each alone on the same
                        cells added up, and the first over the second: above 1 they help each other, below 1 they get
                        in each other's way or waste hits on the same enemy.""")
                .heating(bench.shared().stream().map(scenario -> scenario.name() + " synergy").toArray(String[]::new));
        ReportPage.Table scenarioTable = ReportPage.Table.of("The scenarios", SCENARIO_HEADER, described).noted("""
                walk s: the seconds its first enemy takes to cross the lane. toughness: how many times as tough as a Walker of its rank the enemy stage found the enemy, per point
                of health. walkers a point: the Walkers one point of health on every enemy of the wave is worth.
                partners: what the two partners hold with no subject, in Walkers.""");
        ReportPage.store(SHEET, towers);
        ReportPage.store(PAIR_SHEET, pairTable);
        ReportPage.store(SCENARIO_SHEET, scenarioTable);
        ReportPage.publish(PAGE, "Tower bench", """
                Stage 3 of the balance pipeline, its raw measurements: every build of every tower in the real engine,
                against fixed waves whose enemies' health is searched for the most the build holds. Click a column to
                sort.""", List.of(scenarioTable, towers, pairTable));
        System.out.println("Stored in " + ReportPage.FOLDER);
    }

    private static Set<String> option(String[] args, String name) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals(name)) {
                return Set.of(args[i + 1].toLowerCase(Locale.ROOT).split(","));
            }
        }
        return Set.of();
    }

    /** The stored rows of {@code sheet} that are not {@code replaced}, then {@code fresh}; only {@code fresh} when the sheet's columns changed. */
    private static List<List<String>> merged(String sheet, List<String> header, List<List<String>> fresh,
                                             Predicate<List<String>> replaced) {
        List<List<String>> rows = new ArrayList<>();
        if (Files.exists(ReportPage.FOLDER.resolve(sheet + ".tsv"))) {
            List<List<String>> stored = ReportPage.load(sheet);
            if (!stored.isEmpty() && stored.getFirst().equals(header)) {
                stored.stream().skip(1).filter(row -> !replaced.test(row)).forEach(rows::add);
            }
        }
        rows.addAll(fresh);
        List<String> order = Arrays.stream(TowerFactory.Type.values()).map(Enum::name).toList();
        rows.sort(Comparator.comparingInt(row -> order.indexOf(row.getFirst())));
        return rows;
    }

    private static List<Scenario> scenarios() {
        List<Scenario> alone = List.of(
                Scenario.lone("single", PLAIN).testing("one plain enemy: damage on a single target over one pass"),
                Scenario.stream("stream", PLAIN).testing("plain enemies one at a time: sustained single-target damage"),
                Scenario.stream("group", PLAIN).ranked(Rank.SOLDIER).sending(GROUP, GROUP_SPACING)
                        .testing("a tight column of plain enemies: area damage"),
                Scenario.lone("boss", PLAIN).ranked(Rank.BOSS)
                        .testing("one Boss Walker: a single target with a boss's defences"),
                Scenario.stream("armored", "knight").testing("armor, magic resist and a little plating; no crits"),
                Scenario.stream("plated", "juggernaut").testing("heavy plating: small hits bounce, and it is slow"),
                Scenario.stream("resist", "templar").testing("magic resist"),
                Scenario.stream("hidden", "ghost").testing("enemies that fade out of sight"),
                Scenario.stream("fast", "runner").testing("enemies that run faster the more they are hurt"),
                Scenario.stream("elusive", PLAIN).with(Affix.ELUSIVE).testing("plain enemies that dodge aimed hits"),
                Scenario.stream("regen", PLAIN).with(Affix.REGENERATING)
                        .testing("plain enemies that heal themselves, for what turns a heal against them"),
                Scenario.stream("tank", PLAIN).slowedTo(TANK_SPEED).besideTeam(FOCUS)
                        .testing("slow plain enemies under focused fire, for the damage a hex or a mark adds to what "
                                + "others deal"),
                Scenario.stream("controlled", PLAIN).besideTeam(CONTROL)
                        .testing("plain enemies slowed and frozen by the towers beside it, for what walking or stopping "
                                + "changes: a bleed that runs on cells walked, a stop that adds to one already there"));
        List<Scenario> all = new ArrayList<>(alone);
        alone.stream().map(Scenario::withPartners).forEach(all::add);
        return List.copyOf(all);
    }

    /**
     * One wave on the test lane: enemies of one kind and rank, one after another. Their health differs, from half
     * the health searched to half as much again, so that how many hits one takes is not the same for all of them and
     * a small gain in a tower shows as a small gain in what it holds.
     *
     * @param kind    the enemy it is made of
     * @param spacing the ticks between two of them
     * @param count   how many it sends
     * @param beside  whether the partners stand beside the subject
     * @param tests   what it is there to show
     * @param affix   what each of its enemies carries, if anything
     * @param speed   how fast they walk, a share of their kind's speed
     * @param team    who stands beside the subject when {@code beside}, the two partners if empty
     */
    record Scenario(String name, String kind, Rank rank, int spacing, int count, boolean beside, String tests,
                    Optional<Affix> affix, double speed, List<Stand> team) {

        private static final int STREAM = 8;
        private static final int STREAM_SPACING = 100;
        private static final double LEAST_SHARE = 0.5;

        /** One Veteran. */
        static Scenario lone(String name, String kind) {
            return new Scenario(name, kind, Rank.VETERAN, STREAM_SPACING, 1, false, "", Optional.empty(), 1, List.of());
        }

        /** Veterans far enough apart that an area hit takes one. */
        static Scenario stream(String name, String kind) {
            return new Scenario(name, kind, Rank.VETERAN, STREAM_SPACING, STREAM, false, "", Optional.empty(), 1, List.of());
        }

        Scenario ranked(Rank next) {
            return new Scenario(this.name, this.kind, next, this.spacing, this.count, this.beside, this.tests, this.affix,
                    this.speed, this.team);
        }

        /** {@code enemies} of them, {@code ticks} apart. */
        Scenario sending(int enemies, int ticks) {
            return new Scenario(this.name, this.kind, this.rank, ticks, enemies, this.beside, this.tests, this.affix,
                    this.speed, this.team);
        }

        Scenario testing(String what) {
            return new Scenario(this.name, this.kind, this.rank, this.spacing, this.count, this.beside, what, this.affix,
                    this.speed, this.team);
        }

        /** Each of its enemies carrying {@code added}. */
        Scenario with(Affix added) {
            return new Scenario(this.name, this.kind, this.rank, this.spacing, this.count, this.beside, this.tests,
                    Optional.of(added), this.speed, this.team);
        }

        /** Walking at {@code share} of their kind's speed, so a tower has longer to work on each. */
        Scenario slowedTo(double share) {
            return new Scenario(this.name, this.kind, this.rank, this.spacing, this.count, this.beside, this.tests,
                    this.affix, share, this.team);
        }

        /** Fought beside {@code stands} instead of the two partners. */
        Scenario besideTeam(List<Stand> stands) {
            return new Scenario(this.name, this.kind, this.rank, this.spacing, this.count, this.beside, this.tests,
                    this.affix, this.speed, stands);
        }

        /** Who stands beside the subject here. */
        List<Stand> partners() {
            return this.team.isEmpty() ? PARTNERS : this.team;
        }

        Scenario withPartners() {
            return new Scenario(this.name + BESIDE_MARK, this.kind, this.rank, this.spacing, this.count, true,
                    this.tests + this.besideWords(), this.affix, this.speed, this.team);
        }

        private String besideWords() {
            return this.team.isEmpty() ? ", beside a bare Sniper and a bare Burst"
                    : ", beside " + this.team.size() + " bare " + this.team.getFirst().type().displayName() + "s";
        }

        /** The wave as a script of {@code id}, numbered when its enemies differ. */
        String script(String id) {
            String carried = this.affix.map(added -> added.word() + " ").orElse("");
            String slots = this.count == 1 ? carried + id
                    : IntStream.range(0, this.count).mapToObj(i -> carried + id + i).collect(Collectors.joining(" "));
            return "w" + this.spacing + " " + WaveScript.headed(this.rank, slots);
        }

        /**
         * The share of the health searched the enemy in slot {@code slot} has: evenly spread around one, and
         * shuffled so the wave neither grows nor shrinks as it goes.
         */
        double share(int slot) {
            if (this.count == 1) {
                return 1;
            }
            int stride = IntStream.iterate(this.count / 2 + 1, step -> step + 1)
                    .filter(step -> gcd(step, this.count) == 1).findFirst().orElse(1);
            return LEAST_SHARE + (double) (slot * stride % this.count) / (this.count - 1);
        }

        private static int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }

        /** The lane with this wave, its enemies at their shares of {@code health} points. */
        LevelDefinition lane(double health) {
            RankedEnemy ladder = LADDERS.computeIfAbsent(this.kind, id -> EnemyCatalog.builtIn().ranked(id));
            List<RankedEnemy> mobs = new ArrayList<>();
            for (int slot = 0; slot < this.count; slot++) {
                double points = health * this.share(slot);
                mobs.add(ladder.cloneAs(this.count == 1 ? MOB : MOB + slot, definition -> definition.withHealthAndPrice(
                        Math.max(1, (int) Math.round(points * definition.healthDivisor())), 0)
                        .withBaseSpeed((float) (definition.baseSpeed() * this.speed))));
            }
            return PowerBench.lane(EnemyPowerReport.NO_CONTENT.withCustomRankedEnemies(mobs), this.script(MOB));
        }
    }

    /**
     * What a point of health is worth in Walkers, from the enemy stage's sheet.
     *
     * @param walker    the health of a Walker of the lowest rank
     * @param toughness for each kind at each rank, how many times as tough as the Walker of that rank it is per point
     *                  of health
     */
    record Weights(double walker, Map<String, Double> toughness) {

        private static final String KEY = "@";

        /** Every enemy as tough as a Walker: for a run with no enemy sheet to read. */
        static Weights plain() {
            return new Weights(EnemyCatalog.builtIn().get(PLAIN, Rank.GRUNT).baseHealth(), Map.of());
        }

        /** Read from the enemy stage's stored sheet, which must have been run. */
        static Weights stored() {
            List<List<String>> sheet = ReportPage.load("enemy-power");
            List<String> header = sheet.getFirst();
            int rank = header.indexOf("rank");
            int kind = header.indexOf("kind");
            int health = header.indexOf("health");
            int blind = header.indexOf("blind");
            Map<String, Double> walkers = new LinkedHashMap<>();
            sheet.stream().skip(1).filter(row -> row.get(kind).equals(PLAIN))
                    .forEach(row -> walkers.put(row.get(rank), Double.parseDouble(row.get(health))));
            Map<String, Double> toughness = new LinkedHashMap<>();
            for (List<String> row : sheet.subList(1, sheet.size())) {
                try {
                    toughness.put(row.get(kind) + KEY + row.get(rank), Double.parseDouble(row.get(blind))
                            * walkers.getOrDefault(row.get(rank), Double.NaN) / Double.parseDouble(row.get(health)));
                } catch (NumberFormatException e) {
                    toughness.put(row.get(kind) + KEY + row.get(rank), 1.0);
                }
            }
            return new Weights(plain().walker(), toughness);
        }

        double toughnessOf(Scenario scenario) {
            double found = this.toughness.getOrDefault(
                    scenario.kind() + KEY + scenario.rank().name().toLowerCase(Locale.ROOT), 1.0);
            return Double.isNaN(found) ? 1.0 : found;
        }

        /** The Walkers a wave of {@code scenario} is worth for each point of health its enemies have. */
        double walkersAPoint(Scenario scenario) {
            return scenario.count() * this.toughnessOf(scenario) * scenario.speed() / this.walker;
        }
    }

    /**
     * A tower on a cell owning {@code owned}, bought in that order, with {@code xp}: what opens the tiers whose free
     * bonuses it holds.
     */
    record Stand(TowerFactory.Type type, Point cell, List<String> owned, int xp) {

        /** A tower with no XP, holding no tier's bonus. */
        Stand(TowerFactory.Type type, Point cell, List<String> owned) {
            this(type, cell, owned, 0);
        }
    }

    /**
     * One build of a tower.
     *
     * @param label  its name among the tower's builds
     * @param role   {@link #BARE}, {@link #LINE}, {@link #BEFORE}, {@link #FULL}, {@link #WITHOUT} or
     *               {@link #WITHOUT_AT_TOP}
     * @param node   the node it is there to weigh; for a full tier, the cluster member that is not the first, if any
     * @param versus the label of the build it is set against, or nothing
     * @param owned  the nodes it owns, in the order bought
     * @param cost   the tower and those nodes, at list price
     * @param xp     the XP it stands at: the gate of the tier it is measured in, so it holds the free bonus of every
     *               tier up to that one, and a build and the one it is set against hold the same
     */
    record Build(String label, String role, String node, String versus, List<String> owned, int cost, int xp) {

        static Build of(String label, String role, Resolved bought) {
            return new Build(label, role, "", "", bought.owned(), bought.cost(), 0);
        }

        Build weighing(String weighed, String against) {
            return new Build(this.label, this.role, weighed, against, this.owned, this.cost, this.xp);
        }

        /** The same build standing in {@code tier} of {@code tree}, with the XP that opens it. */
        Build inTier(UpgradeTree tree, int tier) {
            int xp = tree.tiers().get(Math.min(tier, tree.tiers().size() - 1)).xp();
            return new Build(this.label, this.role, this.node, this.versus, this.owned, this.cost, xp);
        }

        /** It on {@code cell}, as a tower of {@code type}. */
        Stand on(TowerFactory.Type type, Point cell) {
            return new Stand(type, cell, this.owned, this.xp);
        }

        /** The same for any two builds that own the same nodes at the same XP. */
        String key() {
            return this.xp + ":" + this.owned.stream().sorted().collect(Collectors.joining(" "));
        }
    }

    /** What a tower ended up owning of the nodes it was asked to buy, and what that and the tower cost. */
    record Resolved(List<String> owned, int cost) {
    }

    private record Job(Scenario scenario, List<Stand> stands, double guess) {
    }

    /**
     * Every build of {@code type} worth measuring, the bare tower first. Two builds may own the same nodes under
     * different labels; they are measured once.
     */
    static List<Build> buildsOf(TowerFactory.Type type) {
        UpgradeTree tree = treeOf(type);
        Map<String, Build> builds = new LinkedHashMap<>();
        builds.put(BARE, Build.of(BARE, BARE, resolve(type, Set.of())));
        for (UpgradeNode node : tree.nodes()) {
            Set<String> line = tree.pathTo(node).stream().map(UpgradeNode::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Resolved with = resolve(type, line);
            line.remove(node.id());
            Resolved before = resolve(type, line);
            String versus = before.owned().isEmpty() && node.tier() == 0 ? BARE : label(BEFORE, node.id());
            builds.putIfAbsent(versus, Build.of(versus, BEFORE, before).weighing(node.id(), "").inTier(tree, node.tier()));
            builds.put(label(LINE, node.id()), Build.of(label(LINE, node.id()), LINE, with).weighing(node.id(), versus)
                    .inTier(tree, node.tier()));
        }
        int top = tree.nodes().stream().mapToInt(UpgradeNode::tier).max().orElse(0);
        Optional<UpgradeCluster> fork = tree.fork();
        int forkTier = fork.map(sides -> tierOf(tree, sides)).orElse(Integer.MAX_VALUE);
        Set<String> weighed = new HashSet<>();
        for (String family : fork.map(UpgradeCluster::memberIds).orElse(List.of(""))) {
            List<Map<UpgradeCluster, String>> pickings = new ArrayList<>();
            pickings.add(Map.of());
            for (UpgradeCluster cluster : tree.clusters()) {
                cluster.memberIds().stream().skip(1).forEach(member -> pickings.add(Map.of(cluster, member)));
            }
            for (Map<UpgradeCluster, String> picking : pickings) {
                Map<UpgradeCluster, String> picks = new LinkedHashMap<>(picking);
                fork.ifPresent(sides -> picks.put(sides, family));
                String other = picking.values().stream().findFirst().orElse("");
                int from = picking.keySet().stream().mapToInt(cluster -> tierOf(tree, cluster)).findFirst().orElse(0);
                for (int tier = from; tier <= top; tier++) {
                    Resolved full = resolve(type, candidates(tree, tier, picks, ""));
                    if (!other.isEmpty() && !full.owned().contains(other)) {
                        continue;
                    }
                    String side = tier >= forkTier ? family : "";
                    String label = label(FULL, side, String.valueOf(tier), other);
                    builds.putIfAbsent(label, Build.of(label, FULL, full).weighing(other, "").inTier(tree, tier));
                    for (UpgradeNode node : tree.nodes()) {
                        if (node.tier() == tier && full.owned().contains(node.id())
                                && weighed.add(side + SEPARATOR + node.id())) {
                            String less = label(WITHOUT, side, node.id());
                            builds.put(less, Build.of(less, WITHOUT, resolve(type, candidates(tree, tier, picks, node.id())))
                                    .weighing(node.id(), label).inTier(tree, tier));
                        }
                        if (tier == top && full.owned().contains(node.id())
                                && weighed.add(WITHOUT_AT_TOP + side + SEPARATOR + node.id())) {
                            String less = label(WITHOUT_AT_TOP, side, node.id());
                            builds.put(less, Build.of(less, WITHOUT_AT_TOP,
                                    resolve(type, candidates(tree, tier, picks, node.id()))).weighing(node.id(), label)
                                    .inTier(tree, tier));
                        }
                    }
                }
            }
        }
        for (StrategyBuilds.Strategy strategy : StrategyBuilds.of(type)) {
            String label = label(STRATEGY, strategy.name());
            Resolved bought = resolve(type, strategy.nodes());
            int tier = bought.owned().stream().mapToInt(id -> tree.node(id).orElseThrow().tier()).max().orElse(0);
            builds.put(label, Build.of(label, STRATEGY, bought).inTier(tree, tier));
        }
        return List.copyOf(builds.values());
    }

    private static String label(String... parts) {
        return Arrays.stream(parts).filter(part -> !part.isEmpty()).collect(Collectors.joining(SEPARATOR));
    }

    private static int tierOf(UpgradeTree tree, UpgradeCluster cluster) {
        return tree.node(cluster.memberIds().getFirst()).orElseThrow().tier();
    }

    /**
     * Every node up to {@code maxTier} but {@code without}, each cluster and the fork giving the member {@code picks}
     * names or its first, and the unlocks of the paid tiers on the way.
     */
    private static Set<String> candidates(UpgradeTree tree, int maxTier, Map<UpgradeCluster, String> picks,
                                          String without) {
        Set<String> ids = new LinkedHashSet<>();
        for (int tier = 1; tier <= maxTier; tier++) {
            tree.unlockOf(tier).map(UpgradeNode::id).ifPresent(ids::add);
        }
        for (UpgradeNode node : tree.nodes()) {
            boolean picked = tree.clusterOf(node)
                    .map(cluster -> picks.getOrDefault(cluster, cluster.memberIds().getFirst()).equals(node.id()))
                    .orElse(true);
            if (node.tier() <= maxTier && picked && !node.id().equals(without) && !STRATEGY_ONLY.contains(node.id())) {
                ids.add(node.id());
            }
        }
        return ids;
    }

    /** Buys what a tower of {@code type} can of {@code wanted}, in the order it is offered, gates waived. */
    private static Resolved resolve(TowerFactory.Type type, Set<String> wanted) {
        GameEngine engine = idleEngine();
        int before = engine.getGameWorld().economy().getCredits();
        Tower tower = Turn.place(engine, type, SUBJECT).orElseThrow();
        boolean bought = true;
        while (bought) {
            bought = false;
            for (UpgradeNode node : tower.offeredUpgrades(engine.getGameWorld())) {
                if (wanted.contains(node.id()) && tower.buyUpgrade(node)) {
                    bought = true;
                    break;
                }
            }
        }
        return new Resolved(tower.upgrades().owned().stream().map(UpgradeNode::id).toList(),
                before - engine.getGameWorld().economy().getCredits());
    }

    /** The class a tower of {@code type} is an instance of, which names the source its prices are written in. */
    static Class<?> classOf(TowerFactory.Type type) {
        return Turn.place(idleEngine(), type, SUBJECT).orElseThrow().getClass();
    }

    /** The tree a tower of {@code type} is built with. */
    static UpgradeTree treeOf(TowerFactory.Type type) {
        return Turn.place(idleEngine(), type, SUBJECT).orElseThrow().upgradeTree();
    }

    private static GameEngine idleEngine() {
        return engine(EnemyPowerReport.NO_CONTENT, FIRST_SEED);
    }

    private static GameEngine engine(LevelDefinition level, long seed) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(level);
        engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
        engine.debugGrantCredits(GRANTED_CREDITS);
        return engine;
    }

    /** Whether {@code stands} let nothing of {@code scenario} through when each enemy has {@code health} points. */
    static boolean holds(Scenario scenario, List<Stand> stands, double health, long seed) {
        GameEngine engine = engine(scenario.lane(health), seed);
        for (Stand stand : stands) {
            Tower tower = Turn.place(engine, stand.type(), stand.cell()).orElseThrow();
            for (String id : stand.owned()) {
                if (!tower.upgradeTree().node(id).filter(tower::buyUpgrade).isPresent()) {
                    throw new IllegalStateException(stand.type() + " cannot buy " + id + " of " + stand.owned());
                }
            }
            tower.earnXp(stand.xp());
            MortarTower.drawMagazineAfresh(tower);
        }
        engine.requestNextWave();
        int tick = 1;
        engine.doTick(tick);
        while (tick < TICK_BUDGET && !engine.isWaveReady() && !engine.outcome().isOver()
                && engine.getGameWorld().enemies().leaks() == 0) {
            tick++;
            engine.doTick(tick);
        }
        return tick < TICK_BUDGET && engine.getGameWorld().enemies().leaks() == 0;
    }

    /**
     * The most health per enemy at which {@code stands} hold {@code scenario}, averaged over the seeds: 0 when they
     * hold nothing, infinite when no health within reason gets through.
     *
     * @param guess a health to try first
     */
    double threshold(Scenario scenario, List<Stand> stands, double guess) {
        double sum = 0;
        for (long seed = FIRST_SEED; seed < FIRST_SEED + this.seeds; seed++) {
            sum += this.thresholdAt(scenario, stands, guess, seed);
        }
        return sum / this.seeds;
    }

    private double thresholdAt(Scenario scenario, List<Stand> stands, double guess, long seed) {
        double health = Math.max(LEAST, guess);
        double low;
        double high;
        if (holds(scenario, stands, health, seed)) {
            do {
                low = health;
                health *= STEP;
                if (health > MOST) {
                    return Double.POSITIVE_INFINITY;
                }
            } while (holds(scenario, stands, health, seed));
            high = health;
        } else {
            do {
                high = health;
                if (health <= LEAST) {
                    return 0;
                }
                health = Math.max(LEAST, health / STEP);
            } while (!holds(scenario, stands, health, seed));
            low = health;
        }
        while (high > low * (1 + this.tolerance) && high - low > LEAST) {
            double middle = Math.sqrt(low * high);
            if (holds(scenario, stands, middle, seed)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return Math.sqrt(low * high);
    }

    private List<Double> thresholds(List<Job> jobs) {
        return jobs.parallelStream().map(job -> this.threshold(job.scenario(), job.stands(), job.guess())).toList();
    }

    private static List<Stand> stands(TowerFactory.Type type, Build build, Scenario scenario) {
        List<Stand> stands = new ArrayList<>(List.of(build.on(type, SUBJECT)));
        if (scenario.beside()) {
            stands.addAll(scenario.partners());
        }
        return stands;
    }

    List<String> header() {
        List<String> header = new ArrayList<>(List.of("tower", "name", "build", "role", "node", "nodeName", "tier", "versus",
                "cost"));
        this.scenarios.forEach(scenario -> header.add(scenario.name()));
        header.add("owned");
        return header;
    }

    /** {@code row} as the console shows it: without the owned nodes, in columns. */
    private List<String> printed(List<String> row) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < row.size() - 1; i++) {
            int width = switch (i) {
                case 0, 1, 5 -> 0;
                case 2, 7 -> -40;
                case 3 -> -8;
                case 4 -> -28;
                default -> 8;
            };
            if (width != 0) {
                cells.add(String.format(Locale.ROOT, "%" + width + "s", row.get(i)));
            }
        }
        return cells;
    }

    /** One row for each build of {@code type}: what it holds of every scenario, in Walkers. */
    List<List<String>> measured(TowerFactory.Type type) {
        List<Build> builds = buildsOf(type);
        Build bare = builds.getFirst();
        List<Double> bareHealth = this.thresholds(this.scenarios.stream()
                .map(scenario -> new Job(scenario, stands(type, bare, scenario), FIRST_GUESS)).toList());
        Map<String, Build> distinct = new LinkedHashMap<>();
        builds.forEach(build -> distinct.putIfAbsent(build.key(), build));
        distinct.remove(bare.key());
        List<Job> jobs = new ArrayList<>();
        for (Build build : distinct.values()) {
            double growth = Math.pow((double) build.cost() / bare.cost(), GUESS_GROWTH);
            for (int i = 0; i < this.scenarios.size(); i++) {
                double guess = bareHealth.get(i) > 0 && !Double.isInfinite(bareHealth.get(i))
                        ? bareHealth.get(i) * growth : FIRST_GUESS;
                jobs.add(new Job(this.scenarios.get(i), stands(type, build, this.scenarios.get(i)), guess));
            }
        }
        List<Double> health = this.thresholds(jobs);
        Map<String, List<Double>> byKey = new LinkedHashMap<>();
        byKey.put(bare.key(), bareHealth);
        int next = 0;
        for (Build build : distinct.values()) {
            byKey.put(build.key(), health.subList(next, next + this.scenarios.size()));
            next += this.scenarios.size();
        }
        UpgradeTree tree = treeOf(type);
        List<List<String>> rows = new ArrayList<>();
        for (Build build : builds) {
            Optional<UpgradeNode> node = tree.node(build.node());
            List<String> row = new ArrayList<>(List.of(type.name(), type.displayName(), build.label(), build.role(),
                    build.node(), node.map(UpgradeNode::displayName).orElse(""),
                    node.map(weighed -> String.valueOf(weighed.tier())).orElse(""), build.versus(),
                    String.valueOf(build.cost())));
            for (int i = 0; i < this.scenarios.size(); i++) {
                row.add(walkers(byKey.get(build.key()).get(i) * this.weights.walkersAPoint(this.scenarios.get(i))));
            }
            row.add(String.join(" ", build.owned()));
            rows.add(row);
        }
        return rows;
    }

    private List<Scenario> shared() {
        return this.scenarios.stream().filter(scenario -> !scenario.beside() && PAIRED.contains(scenario.name())).toList();
    }

    List<String> pairHeader() {
        List<String> header = new ArrayList<>(List.of("tower", "build", "with", "withBuild", "cost"));
        for (Scenario scenario : this.shared()) {
            header.addAll(List.of(scenario.name(), scenario.name() + " apart", scenario.name() + " synergy"));
        }
        return header;
    }

    /** The builds a tower enters a pair with: bare, and each of its families bought whole. */
    private static List<Build> pairBuilds(TowerFactory.Type type) {
        List<Build> builds = buildsOf(type);
        List<Build> wholes = builds.stream()
                .filter(build -> build.role().equals(FULL) && build.node().isEmpty() && !build.owned().isEmpty()).toList();
        int top = wholes.stream().mapToInt(TowerBench::tierOf).max().orElse(0);
        List<Build> ends = new ArrayList<>(List.of(builds.getFirst()));
        wholes.stream().filter(build -> tierOf(build) == top).forEach(ends::add);
        return ends;
    }

    /** The tier a full build is bought up to, which ends its label. */
    private static int tierOf(Build full) {
        return Integer.parseInt(full.label().substring(full.label().lastIndexOf(SEPARATOR) + 1));
    }

    /**
     * One row for each two builds of the same standing (both bare, or both bought whole) of each two towers at least
     * one of which is in {@code selected}: what they hold side by side against what each holds alone on its cell.
     */
    List<List<String>> paired(List<TowerFactory.Type> selected) {
        List<Scenario> shared = this.shared();
        List<TowerFactory.Type> all = Arrays.asList(TowerFactory.Type.values());
        Map<TowerFactory.Type, List<Build>> ends = new LinkedHashMap<>();
        all.forEach(type -> ends.put(type, pairBuilds(type)));

        List<Job> aloneJobs = new ArrayList<>();
        List<String> aloneKeys = new ArrayList<>();
        for (TowerFactory.Type type : all) {
            for (Build build : ends.get(type)) {
                for (Point cell : List.of(SUBJECT, SECOND)) {
                    for (Scenario scenario : shared) {
                        aloneJobs.add(new Job(scenario, List.of(build.on(type, cell)), FIRST_GUESS));
                        aloneKeys.add(type + SEPARATOR + build.label() + SEPARATOR + cell + SEPARATOR + scenario.name());
                    }
                }
            }
        }
        List<Double> aloneHealth = this.thresholds(aloneJobs);
        Map<String, Double> alone = new LinkedHashMap<>();
        IntStream.range(0, aloneKeys.size()).forEach(i -> alone.put(aloneKeys.get(i), aloneHealth.get(i)));

        List<Job> jobs = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        List<double[]> apart = new ArrayList<>();
        for (int a = 0; a < all.size(); a++) {
            for (int b = a; b < all.size(); b++) {
                TowerFactory.Type first = all.get(a);
                TowerFactory.Type second = all.get(b);
                if (!selected.contains(first) && !selected.contains(second)) {
                    continue;
                }
                for (int i = 0; i < ends.get(first).size(); i++) {
                    for (int j = first == second ? i : 0; j < ends.get(second).size(); j++) {
                        Build left = ends.get(first).get(i);
                        Build right = ends.get(second).get(j);
                        if (left.role().equals(BARE) != right.role().equals(BARE)) {
                            continue;
                        }
                        double[] sums = new double[shared.size()];
                        for (int s = 0; s < shared.size(); s++) {
                            String name = shared.get(s).name();
                            sums[s] = alone.get(first + SEPARATOR + left.label() + SEPARATOR + SUBJECT + SEPARATOR + name)
                                    + alone.get(second + SEPARATOR + right.label() + SEPARATOR + SECOND + SEPARATOR + name);
                            jobs.add(new Job(shared.get(s), List.of(left.on(first, SUBJECT), right.on(second, SECOND)),
                                    Math.max(FIRST_GUESS, sums[s])));
                        }
                        apart.add(sums);
                        rows.add(new ArrayList<>(List.of(first.name(), left.label(), second.name(), right.label(),
                                String.valueOf(left.cost() + right.cost()))));
                    }
                }
            }
        }
        List<Double> together = this.thresholds(jobs);
        for (int row = 0; row < rows.size(); row++) {
            for (int s = 0; s < shared.size(); s++) {
                double weight = this.weights.walkersAPoint(shared.get(s));
                double pair = together.get(row * shared.size() + s);
                double sum = apart.get(row)[s];
                rows.get(row).addAll(List.of(walkers(pair * weight), walkers(sum * weight),
                        sum > 0 ? ReportPage.number(pair / sum) : "-"));
            }
        }
        return rows;
    }

    /** One row for each scenario: its wave, what its enemy weighs, and what the partners hold of it by themselves. */
    List<List<String>> described() {
        List<Scenario> beside = this.scenarios.stream().filter(Scenario::beside).toList();
        List<Double> partners = this.thresholds(beside.stream()
                .map(scenario -> new Job(scenario, scenario.partners(), FIRST_GUESS)).toList());
        return this.scenarios.stream().map(scenario -> List.of(scenario.name(),
                "w" + scenario.spacing() + " " + WaveScript.headed(scenario.rank(), scenario.count() + " "
                        + scenario.affix().map(added -> added.word() + " ").orElse("") + scenario.kind()),
                scenario.kind(), scenario.rank().name().toLowerCase(Locale.ROOT), String.valueOf(scenario.count()),
                ReportPage.number(PowerBench.passage(scenario.lane(1)).walkTicks() / (double) TickRate.TICKS_PER_SECOND),
                ReportPage.number(this.weights.toughnessOf(scenario)),
                String.format(Locale.ROOT, "%.4f", this.weights.walkersAPoint(scenario)),
                scenario.beside() ? walkers(partners.get(beside.indexOf(scenario)) * this.weights.walkersAPoint(scenario)) : "-",
                scenario.tests())).toList();
    }

    private static String walkers(double value) {
        return Double.isInfinite(value) ? "inf" : String.format(Locale.ROOT, "%.2f", value);
    }

    /** The scenarios a full run fights. */
    static List<Scenario> allScenarios() {
        return SCENARIOS;
    }

    /** Every build label of {@code builds} that weighs {@code node} in the role {@code role}. */
    static Stream<Build> weighing(List<Build> builds, String role, String node) {
        return builds.stream().filter(build -> build.role().equals(role) && build.node().equals(node));
    }
}
