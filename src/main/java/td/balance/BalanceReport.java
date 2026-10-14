package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.effect.EffectKind;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.wave.Point;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Plays the built-in levels headlessly with players of several skills over many seeds, and answers, per level,
 * whether it is neither easy nor hard, whether positioning and upgrades matter, where the pressure comes, where
 * the credits go, which towers carry the defence, what leaks, and what the lab says counters what. Every number
 * comes with its 95% interval. A tool run on demand, not a test: a minute or two.
 * <p>
 * Usage: {@code BalanceReport [--seeds N] [--levels 0,2] [--strong] [--variety] [--sweep KNOB] [--save NAME]
 * [--compare NAME] [--html]}. {@code --strong} adds a player that rehearses each round before playing it, which
 * shows what the competent player's rules miss; {@code --variety} measures each tower alone and each tower banned;
 * {@code --sweep} (lives, bounty, pause or spacing, repeatable) shows how the competent player's margin moves with
 * one of the level's knobs. Each adds minutes. {@code --save} keeps
 * every per-seed number under {@code target/balance/NAME.tsv}; {@code --compare} plays again and says what moved
 * against that file, seed by seed, so a difference belongs to the change and not to luck.
 */
public final class BalanceReport {

    static final String NAIVE = "naive";
    static final String COMPETENT = "competent";
    static final String RANDOM_CELLS = "random-cells";
    static final String NO_UPGRADES = "no-upgrades";
    static final String STRONG = "strong";
    /**
     * The competent player's own actions played back, not re-decided: its margin is how much tougher the enemies
     * could be before the same build fails, which is what a change does to a fixed play.
     */
    static final String REPLAYED = "competent-replayed";

    /** A competent player should win this share of seeds, at least... */
    static final double WIN_LOW = 0.70;
    /** ...and at most this share. */
    static final double WIN_HIGH = 0.90;
    /** It should hold no less than the level as authored... */
    static final double MARGIN_LOW = 100;
    /** ...and not much more: past this the level cannot push it. */
    static final double MARGIN_HIGH = 150;
    /** A careless player should win no more often than this. */
    static final double NAIVE_WIN_HIGH = 0.20;
    /** A ratio of margins this far from 1 is a difference worth a word. */
    static final double MATTERS = 1.15;
    /** A kind a tower brings down at least this share of, against the plain mob's, has an answer in it. */
    static final double ANSWERS = 0.5;
    /** Two players' pick rates of a node this far apart is a choice one of them makes and the other does not. */
    static final double PICK_GAP = 0.5;
    /** A purchase that takes more than this many rounds of bounty to afford stalls the player. */
    static final int SLOW_ROUNDS = 2;
    /** Help to a neighbour below this share is within the lab's noise. */
    static final double SUPPORTS = 0.05;

    private static final long FIRST_SEED = 20261013L;
    private static final Path STORE = Path.of("target", "balance");

    private final Options options;
    private final PrintStream out;

    private BalanceReport(Options options, PrintStream out) {
        this.options = options;
        this.out = out;
    }

    /**
     * What to run and what to do with it.
     *
     * @param scope  how many seeds, on which levels
     * @param extras what to measure and write beyond the standard report
     * @param files  names to save under and compare with, each empty for none
     * @param sweeps the level knobs to sweep, in order
     */
    record Options(Scope scope, Set<Extra> extras, Names files, List<LevelKnob> sweeps) {

        /**
         * @param seeds  seeds per player and level
         * @param levels indices into the built-in levels
         */
        record Scope(int seeds, List<Integer> levels) {
        }

        int seeds() {
            return this.scope.seeds();
        }

        List<Integer> levels() {
            return this.scope.levels();
        }

        /** What a report can add, each at a cost in time. */
        enum Extra {
            STRONG,
            VARIETY,
            HTML
        }

        /** Where the results go and what they are compared with. */
        record Names(String save, String compare) {
        }

        static Options parse(String[] args) {
            int seeds = 20;
            List<Integer> levels = List.of(0, 1, 2);
            Set<Extra> extras = EnumSet.noneOf(Extra.class);
            String save = "";
            String compare = "";
            List<LevelKnob> sweeps = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--seeds" -> seeds = Integer.parseInt(args[++i]);
                    case "--levels" -> levels = Arrays.stream(args[++i].split(",")).map(Integer::parseInt).toList();
                    case "--strong" -> extras.add(Extra.STRONG);
                    case "--variety" -> extras.add(Extra.VARIETY);
                    case "--save" -> save = args[++i];
                    case "--compare" -> compare = args[++i];
                    case "--html" -> extras.add(Extra.HTML);
                    case "--sweep" -> sweeps.add(LevelKnob.named(args[++i]));
                    default -> throw new IllegalArgumentException("Unknown option " + args[i]
                            + "; use --seeds N --levels 0,2 --strong --variety --sweep lives|bounty|pause|spacing"
                            + " --save NAME --compare NAME --html");
                }
            }
            return new Options(new Scope(seeds, levels), extras, new Names(save, compare), List.copyOf(sweeps));
        }

        String save() {
            return this.files.save();
        }

        String compare() {
            return this.files.compare();
        }
    }

    public static void main(String[] args) throws IOException {
        quietLogs();
        Options options = Options.parse(args);
        new BalanceReport(options, new PrintStream(System.out, true, StandardCharsets.UTF_8)).run();
    }

    /** The engine logs every placement and round at INFO; thousands of plays would drown the report. */
    static void quietLogs() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    }

    private void run() throws IOException {
        long started = System.nanoTime();
        List<LevelDefinition> all = new BuiltInLevelCatalog().levels();
        List<LevelRun> runs = new ArrayList<>();
        for (int index : this.options.levels()) {
            LevelRun run = measure(all.get(index), this.options.seeds(), this.options.extras());
            runs.add(run);
            this.printLevel(run);
            for (LevelKnob knob : this.options.sweeps()) {
                this.printSweep(run, knob);
            }
        }
        this.out.printf("%n(%d seeds per player, %.0fs)%n", this.options.seeds(), (System.nanoTime() - started) / 1e9);
        ResultTable table = ResultTable.of(runs);
        if (!this.options.save().isEmpty()) {
            this.out.println("Saved to " + table.save(STORE.resolve(this.options.save() + ".tsv")));
            saveLogs(STORE.resolve(this.options.save()), runs);
        }
        if (!this.options.compare().isEmpty()) {
            ResultTable before = ResultTable.load(STORE.resolve(this.options.compare() + ".tsv"));
            this.out.println();
            this.out.print(before.compareWith(table, "Compared with '" + this.options.compare() + "'"));
            this.out.print(samePlay(before, STORE.resolve(this.options.compare()), runs));
        }
        if (this.options.extras().contains(Options.Extra.HTML)) {
            Path file = STORE.resolve("report.html");
            Files.createDirectories(STORE);
            Files.writeString(file, HtmlReport.of(runs), StandardCharsets.UTF_8);
            this.out.println("HTML report: " + file.toAbsolutePath());
        }
    }

    /** Keeps every competent play's action log, one file per level and seed, for a later same-play comparison. */
    private static void saveLogs(Path folder, List<LevelRun> runs) throws IOException {
        for (LevelRun run : runs) {
            for (PlayRecord play : run.playsOf(COMPETENT)) {
                Path file = logFile(folder, run.survey().level(), play.play().seed());
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.write(file, play.actions().stream().map(PlayRecord.TimedAction::toLine).toList(),
                        StandardCharsets.UTF_8);
            }
        }
    }

    private static Path logFile(Path folder, LevelDefinition level, long seed) {
        return folder.resolve(level.name().replaceAll("[^A-Za-z0-9]+", "-")).resolve(COMPETENT + "-" + seed + ".log");
    }

    /**
     * Replays the saved competent logs on this version and compares them with the saved competent plays: what the
     * change does to the same actions, apart from how the player adapts to it. Empty when no logs were saved.
     */
    private static String samePlay(ResultTable before, Path folder, List<LevelRun> runs) throws IOException {
        StringBuilder out = new StringBuilder();
        for (LevelRun run : runs) {
            LevelDefinition level = run.survey().level();
            List<Long> seeds = run.playsOf(COMPETENT).stream().map(play -> play.play().seed()).toList();
            if (seeds.isEmpty() || !java.nio.file.Files.exists(logFile(folder, level, seeds.getFirst()))) {
                continue;
            }
            List<List<PlayRecord.TimedAction>> logs = new ArrayList<>();
            for (long seed : seeds) {
                logs.add(java.nio.file.Files.readAllLines(logFile(folder, level, seed), StandardCharsets.UTF_8).stream()
                        .filter(line -> !line.isBlank()).map(PlayRecord.TimedAction::parse).toList());
            }
            List<PlayRecord> replayed = IntStream.range(0, seeds.size()).parallel().boxed()
                    .map(i -> LevelPlay.play(run.survey(), ScriptedPlayer.spec(COMPETENT, logs.get(i)), seeds.get(i),
                            LevelPlay.Pacing.standard()))
                    .toList();
            int[] margins = IntStream.range(0, seeds.size()).parallel()
                    .map(i -> Margin.healthPercent(run.survey(), ScriptedPlayer.spec(REPLAYED, logs.get(i)), seeds.get(i),
                            LevelPlay.Pacing.standard()))
                    .toArray();
            out.append(before.only(level.name(), COMPETENT).plus(before.only(level.name(), REPLAYED)).compareWith(
                    ResultTable.ofPlays(level.name(), COMPETENT, replayed).withMargins(level.name(), REPLAYED, margins),
                    "Same play: the competent actions saved as '" + folder.getFileName() + "', replayed on this version"));
        }
        return out.isEmpty() ? "" : "\n" + out;
    }

    /** The players every level is played by, the competent one's variants beside it to compare with. */
    static Map<String, PlayerSpec> players(Map<String, TowerLab> labs) {
        Map<String, PlayerSpec> players = new LinkedHashMap<>();
        players.put(NAIVE, NaivePlayer.spec());
        players.put(COMPETENT, CompetentPlayer.spec(COMPETENT, labs, CompetentPlayer.Style.standard()));
        players.put(RANDOM_CELLS, CompetentPlayer.spec(RANDOM_CELLS, labs,
                CompetentPlayer.Style.standard().withRandomCells()));
        players.put(NO_UPGRADES, CompetentPlayer.spec(NO_UPGRADES, labs,
                CompetentPlayer.Style.standard().withoutUpgrades()));
        return players;
    }

    /**
     * The competent player held to one tower type, and with one banned, by name: what the variety section measures.
     * The Beacon lands nothing alone, so it is only ever banned.
     */
    static Map<String, PlayerSpec> varietyPlayers(Map<String, TowerLab> labs) {
        Map<String, PlayerSpec> players = new LinkedHashMap<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            if (type != TowerFactory.Type.AURA) {
                players.put(alone(type), CompetentPlayer.spec(alone(type), labs,
                        CompetentPlayer.Style.standard().withTypes(EnumSet.of(type))));
            }
            EnumSet<TowerFactory.Type> others = EnumSet.allOf(TowerFactory.Type.class);
            others.remove(type);
            players.put(banned(type), CompetentPlayer.spec(banned(type), labs,
                    CompetentPlayer.Style.standard().withTypes(others)));
        }
        return players;
    }

    static String alone(TowerFactory.Type type) {
        return "only-" + type.name().toLowerCase(Locale.ROOT);
    }

    static String banned(TowerFactory.Type type) {
        return "no-" + type.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Runs the lab, then every player on every seed, at the level as authored and for its margin; with
     * {@code extras}, the strong player too, and the margins of each tower alone and each tower banned.
     */
    static LevelRun measure(LevelDefinition level, int seeds, Set<Options.Extra> extras) {
        BoardSurvey survey = BoardSurvey.of(level);
        TowerLab lab = TowerLab.of(survey);
        Map<String, TowerLab> labs = Map.of(level.name(), lab);
        Map<String, List<PlayRecord>> plays = new LinkedHashMap<>();
        Map<String, int[]> margins = new LinkedHashMap<>();
        Map<String, PlayerSpec> players = players(labs);
        if (extras.contains(Options.Extra.STRONG)) {
            players.put(STRONG, StrongPlayer.spec(STRONG, labs));
        }
        for (Map.Entry<String, PlayerSpec> player : players.entrySet()) {
            plays.put(player.getKey(), LongStream.range(FIRST_SEED, FIRST_SEED + seeds).parallel().boxed()
                    .map(seed -> LevelPlay.play(survey, player.getValue(), seed, LevelPlay.Pacing.standard())).toList());
            margins.put(player.getKey(), margins(survey, player.getValue(), seeds));
        }
        margins.put(REPLAYED, replayedMargins(survey, plays.get(COMPETENT)));
        if (extras.contains(Options.Extra.VARIETY)) {
            varietyPlayers(labs).forEach((name, player) -> margins.put(name, margins(survey, player, seeds)));
        }
        return new LevelRun(lab, plays, margins);
    }

    /** For each play, the margin of its own actions played back on its own seed. */
    private static int[] replayedMargins(BoardSurvey survey, List<PlayRecord> plays) {
        return plays.parallelStream()
                .mapToInt(play -> Margin.healthPercent(survey, ScriptedPlayer.spec(REPLAYED, play.actions()),
                        play.play().seed(), LevelPlay.Pacing.standard()))
                .toArray();
    }

    private static int[] margins(BoardSurvey survey, PlayerSpec player, int seeds) {
        return LongStream.range(FIRST_SEED, FIRST_SEED + seeds).parallel()
                .mapToInt(seed -> Margin.healthPercent(survey, player, seed, LevelPlay.Pacing.standard())).toArray();
    }

    private void printLevel(LevelRun run) {
        LevelDefinition level = run.survey().level();
        this.out.printf("%n=== %s: %d credits, %d lives, %d rounds, %d paths ===%n", level.name(),
                level.startingCredits(), level.startingLives(), level.paths().getFirst().waves().size(),
                level.paths().size());
        this.printDifficulty(run);
        this.printPositioning(run);
        this.printUpgrades(run);
        this.printPressure(run);
        this.printEconomy(run);
        this.printTowers(run);
        this.printVariety(run);
        this.printCounters(run);
        this.printCoverage(run);
    }

    private void printCoverage(LevelRun run) {
        PlayRecord.EffectUse use = run.effectUse(COMPETENT);
        int plays = run.playsOf(COMPETENT).size();
        this.out.println();
        this.out.println("COVERAGE (what the plays triggered: the competent player's, and anything no player's did)");
        this.out.println("  effects on enemies, share of enemy time (times one took hold per play): "
                + Arrays.stream(EffectKind.values()).filter(kind -> use.starts().containsKey(kind))
                .sorted(Comparator.comparingDouble(use::uptime).reversed())
                .map(kind -> String.format("%s %.0f%% (%.0f)", name(kind), use.uptime(kind) * 100,
                        use.starts().get(kind) / (double) Math.max(1, plays)))
                .collect(Collectors.joining(", ")));
        List<EffectKind> neverStarted = run.effectsNeverStarted();
        this.out.println("  effects no play put on an enemy: " + (neverStarted.isEmpty() ? "none"
                : neverStarted.stream().map(BalanceReport::name).collect(Collectors.joining(", "))));
        String used = run.abilityUses(COMPETENT).entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> String.format("%s %.1f", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
        this.out.println("  enemy abilities used per play: " + (used.isEmpty() ? "none" : used));
        List<String> neverUsed = run.abilitiesNeverUsed();
        this.out.println("  enemy abilities no play saw used: " + (neverUsed.isEmpty() ? "none"
                : String.join(", ", neverUsed)));
        Map<TowerFactory.Type, List<String>> neverBought = run.nodesNeverBought();
        this.out.println("  nodes no play bought, of each tower's tree: " + Arrays.stream(TowerFactory.Type.values())
                .map(type -> String.format("%s %d/%d", type.displayName(), neverBought.get(type).size(),
                        run.lab().profile(type).nodeFactors().size()))
                .collect(Collectors.joining(", ")));
    }

    static String name(EffectKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    /**
     * The competent player's margin and win rate at each value of {@code knob}, the seeds the same at every value,
     * and how far the margin moves across the sweep.
     */
    private void printSweep(LevelRun run, LevelKnob knob) {
        PlayerSpec player = CompetentPlayer.spec(COMPETENT, Map.of(run.survey().level().name(), run.lab()),
                CompetentPlayer.Style.standard());
        this.out.println();
        this.out.println("SWEEP " + knob.name().toLowerCase(Locale.ROOT)
                + " (the competent player at each value; the level as authored is the standard one)");
        this.out.printf("  %-12s %-15s %s%n", knob.unit(), "won", "holds enemy health");
        for (int value : knob.settings()) {
            LevelKnob.Setting setting = knob.at(value);
            List<Long> seeds = LongStream.range(FIRST_SEED, FIRST_SEED + this.options.seeds()).boxed().toList();
            Estimate won = Estimate.proportion(seeds.parallelStream()
                    .map(seed -> LevelPlay.play(run.survey(), player, seed, setting.pacing(), setting.rules()))
                    .toList(), play -> play.result().won());
            Estimate margin = Estimate.of(seeds.parallelStream()
                    .mapToDouble(seed -> Margin.healthPercent(run.survey(), player, seed, setting.pacing(),
                            setting.rules()))
                    .toArray());
            this.out.printf("  %-12d %-15s %s%n", value, won.formatPercent(), margin.format(0) + "%");
        }
    }

    private void printVariety(LevelRun run) {
        if (!run.margins().containsKey(banned(TowerFactory.Type.SNIPER))) {
            return;
        }
        this.out.println();
        this.out.println("VARIETY (margins of the competent player with only one tower type, and with one banned)");
        this.out.printf("  %-10s %-12s %-12s %s%n", "tower", "alone", "banned", "banned against the full choice");
        List<String> carriers = new ArrayList<>();
        List<String> holdsAlone = new ArrayList<>();
        List<String> spare = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            String aloneMargin = type == TowerFactory.Type.AURA ? "-" : marginText(run.margin(alone(type)));
            Estimate ratio = run.marginRatio(banned(type), COMPETENT);
            this.out.printf("  %-10s %-12s %-12s x%.2f (%.2f-%.2f)%n", type.displayName(), aloneMargin,
                    marginText(run.margin(banned(type))), ratio.value(), ratio.low(), ratio.high());
            if (ratio.high() < 1 / MATTERS) {
                carriers.add(String.format("%s (x%.2f without it)", type.displayName(), ratio.value()));
            } else if (ratio.low() > 1 / MATTERS) {
                spare.add(type.displayName());
            }
            if (type != TowerFactory.Type.AURA && run.margin(alone(type)).low() > MARGIN_LOW) {
                holdsAlone.add(type.displayName());
            }
        }
        this.out.println("  carries the defence (the level gets much harder without it): "
                + (carriers.isEmpty() ? "none" : String.join(", ", carriers)));
        this.out.println("  holds the level alone: " + (holdsAlone.isEmpty() ? "none" : String.join(", ", holdsAlone)));
        this.out.println("  could be banned at little cost: " + (spare.isEmpty() ? "none" : String.join(", ", spare)));
    }

    private void printDifficulty(LevelRun run) {
        this.out.println();
        this.out.println("DIFFICULTY - " + difficultyVerdict(run));
        this.out.printf("  %-13s %-15s %-13s %-13s %s%n", "player", "won", "lives lost", "leaks", "holds enemy health");
        for (String player : shownPlayers(run)) {
            this.out.printf("  %-13s %-15s %-13s %-13s %s%n", player, run.winRate(player).formatPercent(),
                    run.livesLost(player).format(1), run.leaks(player).format(1), run.margin(player).format(0) + "%");
        }
        this.out.printf("  target: competent wins %.0f-%.0f%% and holds %.0f-%.0f%%; naive wins under %.0f%%%n",
                WIN_LOW * 100, WIN_HIGH * 100, MARGIN_LOW, MARGIN_HIGH, NAIVE_WIN_HIGH * 100);
        Estimate margin = run.margin(COMPETENT);
        if (margin.low() > MARGIN_HIGH || margin.high() < MARGIN_LOW) {
            double middle = (MARGIN_LOW + MARGIN_HIGH) / 2;
            this.out.printf("  to put the competent player at %.0f%%, scale every enemy's health by x%.2f (or make the"
                    + " level that much harder another way)%n", middle, margin.value() / middle);
        }
        this.printLossRounds("naive", run.lossRounds(NAIVE));
        this.printLossRounds("competent", run.lossRounds(COMPETENT));
        this.out.printf("  the competent player's own actions, played back as they were, hold %s%n",
                run.margin(REPLAYED).format(0) + "%");
        if (run.has(STRONG)) {
            Estimate gap = run.marginRatio(STRONG, COMPETENT);
            this.out.printf("  rehearsing each round holds x%.2f (%.2f-%.2f) the competent player's margin: what its"
                    + " rules miss%n", gap.value(), gap.low(), gap.high());
        }
    }

    /** The players the difficulty table lists, weakest first, the strong one only when it was measured. */
    static List<String> shownPlayers(LevelRun run) {
        return List.of(NAIVE, RANDOM_CELLS, NO_UPGRADES, COMPETENT, STRONG).stream().filter(run::has).toList();
    }

    private void printLossRounds(String player, Map<Integer, Integer> rounds) {
        if (!rounds.isEmpty()) {
            this.out.println("  " + player + " loses in round: " + rounds.entrySet().stream()
                    .map(entry -> entry.getKey() + " (" + entry.getValue() + "x)").collect(Collectors.joining(", ")));
        }
    }

    /** Too easy, too hard or in band, with the reason, judged on the competent and naive players. */
    static String difficultyVerdict(LevelRun run) {
        Estimate won = run.winRate(COMPETENT);
        Estimate margin = run.margin(COMPETENT);
        Estimate naive = run.winRate(NAIVE);
        List<String> notes = new ArrayList<>();
        String verdict;
        if (margin.low() > MARGIN_HIGH) {
            verdict = "TOO EASY";
            notes.add(String.format("a competent player holds %.1fx the enemy health", margin.value() / 100));
        } else if (margin.high() < MARGIN_LOW || won.high() < WIN_LOW) {
            verdict = "TOO HARD";
            notes.add(String.format("a competent player wins %.0f%%", won.value() * 100));
        } else if (won.low() > WIN_HIGH) {
            verdict = "EASY";
            notes.add("a competent player always wins");
        } else {
            verdict = "IN BAND";
        }
        if (naive.low() > NAIVE_WIN_HIGH) {
            notes.add(String.format("a careless player wins %.0f%%", naive.value() * 100));
        }
        return verdict + (notes.isEmpty() ? "" : ": " + String.join("; ", notes));
    }

    private void printPositioning(LevelRun run) {
        Estimate ratio = run.marginRatio(COMPETENT, RANDOM_CELLS);
        this.out.println();
        this.out.println("POSITIONING - " + mattersVerdict(ratio));
        this.out.printf("  on its chosen cells the competent player holds %s, on random cells that reach a path %s:%n",
                run.margin(COMPETENT).format(0) + "%", run.margin(RANDOM_CELLS).format(0) + "%");
        this.out.printf("  chosen cells are worth x%.2f (%.2f-%.2f); random cells still win %s%n", ratio.value(),
                ratio.low(), ratio.high(), run.winRate(RANDOM_CELLS).formatPercent());
        this.out.printf("  the five best cells of two tower types overlap %.0f%% (lower: each tower has places of its"
                + " own)%n", bestCellOverlap(run.lab()) * 100);
    }

    /** A margin as a percentage with its interval, or "never wins" when no seed won even the weakest enemies. */
    static String marginText(Estimate margin) {
        return margin.high() <= 0 ? "never wins" : margin.format(0) + "%";
    }

    static String mattersVerdict(Estimate ratio) {
        if (ratio.n() == 0) {
            return "not measured";
        }
        if (ratio.low() > MATTERS) {
            return String.format("MATTERS (x%.2f)", ratio.value());
        }
        if (ratio.high() < 1 / MATTERS) {
            return String.format("BACKFIRES (x%.2f)", ratio.value());
        }
        if (ratio.low() > 1 || ratio.high() < 1) {
            return String.format("matters a little (x%.2f)", ratio.value());
        }
        return "NO DIFFERENCE the seeds can show";
    }

    /** The mean share of their five best cells that two tower types have in common. */
    static double bestCellOverlap(TowerLab lab) {
        List<Set<Point>> tops = TowerLab.damageDealers().stream()
                .map(type -> lab.survey().buildable().stream()
                        .sorted(Comparator.comparingDouble((Point cell) -> lab.cellValue(type, cell)).reversed())
                        .limit(5).collect(Collectors.toSet()))
                .toList();
        double sum = 0;
        int pairs = 0;
        for (int i = 0; i < tops.size(); i++) {
            for (int j = i + 1; j < tops.size(); j++) {
                Set<Point> common = new HashSet<>(tops.get(i));
                common.retainAll(tops.get(j));
                sum += common.size() / 5.0;
                pairs++;
            }
        }
        return pairs == 0 ? 0 : sum / pairs;
    }

    private void printUpgrades(LevelRun run) {
        Estimate ratio = run.marginRatio(COMPETENT, NO_UPGRADES);
        this.out.println();
        this.out.println("UPGRADES - " + mattersVerdict(ratio));
        this.out.printf("  buying upgrades the competent player holds %s, building only %s: x%.2f (%.2f-%.2f)%n",
                run.margin(COMPETENT).format(0) + "%", run.margin(NO_UPGRADES).format(0) + "%", ratio.value(),
                ratio.low(), ratio.high());
        Map<String, Double> picks = run.nodePickRates(COMPETENT);
        String top = picks.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8).map(entry -> String.format("%s %.0f%%", entry.getKey(), entry.getValue() * 100))
                .collect(Collectors.joining(", "));
        this.out.println("  bought most: " + (top.isEmpty() ? "nothing" : top));
        long always = picks.values().stream().filter(rate -> rate >= 0.999).count();
        this.out.printf("  %d nodes bought in every play, %d in some, every other never%n", always, picks.size() - always);
        double[] byThird = run.upgradeShareByThird(COMPETENT);
        this.out.printf("  share of spending on upgrades, early / middle / late rounds: %s%n", thirds(byThird));
        if (run.has(STRONG)) {
            this.out.printf("  the strong player's: %s%n", thirds(run.upgradeShareByThird(STRONG)));
            this.out.println("  picked far more often by one player than the other (strong / competent): "
                    + pickGaps(picks, run.nodePickRates(STRONG)));
        }
    }

    private static String thirds(double[] shares) {
        return Arrays.stream(shares).mapToObj(share -> Double.isNaN(share) ? "-" : String.format("%.0f%%", share * 100))
                .collect(Collectors.joining(" / "));
    }

    /** The nodes whose pick rates differ by at least {@link #PICK_GAP} between the two players, widest first. */
    static String pickGaps(Map<String, Double> competent, Map<String, Double> strong) {
        Set<String> nodes = new java.util.LinkedHashSet<>(strong.keySet());
        nodes.addAll(competent.keySet());
        String gaps = nodes.stream()
                .filter(node -> Math.abs(strong.getOrDefault(node, 0.0) - competent.getOrDefault(node, 0.0)) >= PICK_GAP)
                .sorted(Comparator.comparingDouble((String node) ->
                        -Math.abs(strong.getOrDefault(node, 0.0) - competent.getOrDefault(node, 0.0))))
                .limit(8)
                .map(node -> String.format("%s %.0f%%/%.0f%%", node, strong.getOrDefault(node, 0.0) * 100,
                        competent.getOrDefault(node, 0.0) * 100))
                .collect(Collectors.joining(", "));
        return gaps.isEmpty() ? "none" : gaps;
    }

    private void printPressure(LevelRun run) {
        double[] closest = run.perRound(COMPETENT, round -> round.pressure().closest());
        double[] leaked = run.perRound(COMPETENT, round -> round.pressure().leaked());
        double[] naiveLeaked = run.perRound(NAIVE, round -> round.pressure().leaked());
        double[] health = run.perRound(COMPETENT, round -> round.pressure().healthIn());
        this.out.println();
        this.out.println("PRESSURE BY ROUND (competent unless named; closest: how far the furthest enemy got, 0-9)");
        this.out.println("  round    " + row(IntStream.rangeClosed(1, closest.length).asDoubleStream().toArray(),
                v -> String.format("%.0f", v)));
        this.out.println("  closest  " + row(closest, BalanceReport::digit));
        this.out.println("  leaks    " + row(leaked, BalanceReport::count));
        this.out.println("  naive    " + row(naiveLeaked, BalanceReport::count) + "   leaks of the naive player");
        this.out.println("  health   " + row(health, BalanceReport::compact) + "   enemy health walking in");
        this.out.println("  board    " + row(run.peakOnBoard(COMPETENT), BalanceReport::compact)
                + "   most enemy health on the board at once");
        List<Integer> walls = IntStream.range(0, closest.length).filter(i -> closest[i] >= 0.8 || leaked[i] > 0.05)
                .mapToObj(i -> i + 1).toList();
        List<Integer> breathers = IntStream.range(0, closest.length).filter(i -> closest[i] < 0.25)
                .mapToObj(i -> i + 1).toList();
        this.out.println("  walls (an enemy gets 80% of the way or leaks): " + (walls.isEmpty() ? "none" : walls));
        this.out.println("  breathers (nothing gets a quarter of the way): " + (breathers.isEmpty() ? "none" : breathers));
    }

    private static String row(double[] values, java.util.function.DoubleFunction<String> format) {
        return Arrays.stream(values).mapToObj(v -> String.format("%5s", Double.isNaN(v) ? "-" : format.apply(v)))
                .collect(Collectors.joining());
    }

    private static String digit(double share) {
        return String.valueOf((int) Math.min(9, Math.floor(share * 10)));
    }

    private static String count(double value) {
        return value < 0.05 ? "." : value < 9.5 ? String.format("%.0f", Math.max(1, value)) : "9+";
    }

    /** A number in at most four characters: {@code 950}, {@code 1.2k}, {@code 34k}, {@code 1.2M}. */
    static String compact(double value) {
        if (value < 1000) {
            return String.format("%.0f", value);
        }
        if (value < 1_000_000) {
            return value < 10_000 ? String.format("%.1fk", value / 1000) : String.format("%.0fk", value / 1000);
        }
        return String.format("%.1fM", value / 1_000_000);
    }

    private void printEconomy(LevelRun run) {
        double[] idle = run.perRound(COMPETENT, round -> round.economy().creditsAtStart());
        double[] earned = run.perRound(COMPETENT, round -> round.economy().earned());
        double[] wanted = run.perRound(COMPETENT, round -> round.economy().spent().wanted());
        this.out.println();
        this.out.println("ECONOMY (competent)");
        this.out.println("  idle     " + row(idle, BalanceReport::compact) + "   unspent as the round starts");
        this.out.println("  earned   " + row(earned, BalanceReport::compact) + "   bounty");
        this.out.println("  towers   " + row(run.perRound(COMPETENT, round -> round.economy().spent().towers()),
                BalanceReport::compact) + "   spent on towers");
        this.out.println("  upgrades " + row(run.perRound(COMPETENT, round -> round.economy().spent().upgrades()),
                BalanceReport::compact) + "   spent on upgrades");
        this.out.println("  wanted   " + row(wanted, BalanceReport::compact)
                + "   price of what it would buy next, as the round ends");
        this.out.println("  " + economyVerdict(earned, wanted));
        double totalEarned = Arrays.stream(earned).filter(v -> !Double.isNaN(v)).sum();
        double meanIdle = Arrays.stream(idle).filter(v -> !Double.isNaN(v)).average().orElse(0);
        this.out.printf("  %d starting credits and %.0f bounty over the level; %.0f idle at a round's start on average%n",
                run.survey().level().startingCredits(), totalEarned, meanIdle);
    }

    /**
     * Whether what a round pays keeps up with what the player wants next: the rounds where the purchase it wanted
     * cost more than two rounds' bounty, when the player waits longest between purchases.
     */
    static String economyVerdict(double[] earned, double[] wanted) {
        List<Integer> slow = IntStream.range(0, earned.length)
                .filter(i -> !Double.isNaN(wanted[i]) && wanted[i] > SLOW_ROUNDS * Math.max(1, earned[i]))
                .mapToObj(i -> i + 1).toList();
        return "what it wants next costs more than " + SLOW_ROUNDS + " rounds of bounty in round: "
                + (slow.isEmpty() ? "none" : slow);
    }

    private void printTowers(LevelRun run) {
        Map<TowerFactory.Type, Double> shares = run.damageShares(COMPETENT);
        Map<TowerFactory.Type, Double> counts = run.towerCounts(COMPETENT);
        this.out.println();
        this.out.println("TOWERS (competent: share of the damage landed, and towers standing at the end)");
        this.out.println("  " + Arrays.stream(TowerFactory.Type.values())
                .map(type -> String.format("%s %.0f%% (%.1f)", type.displayName(), shares.getOrDefault(type, 0.0) * 100,
                        counts.getOrDefault(type, 0.0)))
                .collect(Collectors.joining(", ")));
        List<String> unused = Arrays.stream(TowerFactory.Type.values())
                .filter(type -> counts.getOrDefault(type, 0.0) < 0.05).map(TowerFactory.Type::displayName).toList();
        if (!unused.isEmpty()) {
            this.out.println("  never built: " + String.join(", ", unused));
        }
        shares.entrySet().stream().filter(entry -> entry.getValue() > 0.5).findFirst().ifPresent(
                entry -> this.out.printf("  the %s lands over half of all damage%n", entry.getKey().displayName()));
        String leaks = run.leaksByKind(COMPETENT).entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> String.format("%s %.1f", entry.getKey(), entry.getValue())).collect(Collectors.joining(", "));
        this.out.println("  leaks per play by kind: " + (leaks.isEmpty() ? "none" : leaks));
    }

    private void printCounters(LevelRun run) {
        TowerLab lab = run.lab();
        this.out.println();
        this.out.println("COUNTERS (lab duels: the share of a wave a tower brings down, against the plain mob's as 1.00)");
        this.out.printf("  %-16s", "");
        TowerLab.damageDealers().forEach(type -> this.out.printf("%9s", type.displayName()));
        this.out.println();
        for (String key : lab.levelRanks()) {
            this.out.printf("  %-16s", key);
            List<TowerFactory.Type> answers = new ArrayList<>();
            for (TowerFactory.Type type : TowerLab.damageDealers()) {
                double factor = lab.kindFactor(type, key);
                this.out.printf("%9.2f", factor);
                if (factor >= ANSWERS) {
                    answers.add(type);
                }
            }
            this.out.println(answers.size() == 1 ? "   only the " + answers.getFirst().displayName()
                    : answers.isEmpty() ? "   NO ANSWER" : "");
        }
        List<String> outliers = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            lab.profile(type).nodeFactors().forEach((node, factor) -> {
                if (factor >= 1.75) {
                    outliers.add(String.format("%s x%.2f", node, factor));
                } else if (factor < 0.9) {
                    outliers.add(String.format("%s x%.2f", node, factor));
                }
            });
        }
        this.out.println("  nodes far from the rest alone (x1.75 and up, or under x0.9): "
                + (outliers.isEmpty() ? "none" : String.join(", ", outliers)));
        this.out.println("  support (the share more a Sniper beside it lands): " + supportText(lab));
    }

    /** Each tower that helps a neighbour, then the nodes that add the most help. */
    static String supportText(TowerLab lab) {
        List<String> towers = new ArrayList<>();
        List<Map.Entry<String, Double>> nodes = new ArrayList<>();
        for (TowerFactory.Type type : TowerLab.damageDealers()) {
            TowerLab.Partnering partners = lab.profile(type).partners();
            if (partners.supportGain() >= SUPPORTS) {
                towers.add(String.format("%s +%.0f%%", type.displayName(), partners.supportGain() * 100));
            }
            partners.supportSteps().entrySet().stream().filter(entry -> entry.getValue() >= SUPPORTS)
                    .forEach(nodes::add);
        }
        String bestNodes = nodes.stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(6)
                .map(entry -> String.format("%s +%.0f%%", entry.getKey(), entry.getValue() * 100))
                .collect(Collectors.joining(", "));
        return (towers.isEmpty() ? "no bare tower" : String.join(", ", towers))
                + (bestNodes.isEmpty() ? "" : "; nodes " + bestNodes);
    }
}
