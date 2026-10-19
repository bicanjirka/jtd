package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.util.ThreadConfined;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Checks one level wave by wave against what each wave is meant to ask of the player. The strong player is the
 * reference: its margin, the round it loses in, its pressure on each round, and how its margin moves with the
 * bounty. Each signature wave is checked by denying the competent player the role it asks for, from the start:
 * the wave should be where that player first leaks, and no other denial should break it first. A strip wave is
 * checked on the strong player's own army: taking any role's towers off just before it should make it leak.
 * <p>
 * Usage: {@code WaveCheckReport [--level N] [--seeds N] [--checks 6:single,12:control+sustain] [--strip 17]
 * [--parts strong,replayed,margin,bounty,bans,strip]}. A tool run on demand, not a test: the strong player rehearses
 * every round a dozen ways, so the margin and the bounty sweep take many minutes.
 */
public final class WaveCheckReport {

    /** A round in which at least this many enemies leak was a real loss of ground, not a close call. */
    static final int HEAVY = 1;
    /** A condition holds for a check when it holds on at least this share of the seeds. */
    static final double MOST = 2.0 / 3;
    /** The bounty sweep moves every kill's pay this far either way, in percent. */
    static final int BOUNTY_STEP = 10;
    private static final long FIRST_SEED = 20261019L;

    private final Options options;
    private final PrintStream out;

    private WaveCheckReport(Options options, PrintStream out) {
        this.options = options;
        this.out = out;
    }

    /** The parts of the report, each at its own cost. */
    enum Part {
        STRONG,
        /** The strong player's own actions played back at tougher health: a fast stand-in for its margin. */
        REPLAYED,
        MARGIN,
        BOUNTY,
        BANS,
        STRIP
    }

    /**
     * What to check.
     *
     * @param level  index into the built-in levels
     * @param seeds  seeds per player
     * @param checks each signature wave (1-based) and the roles it asks for
     * @param strip  the wave the strip check runs on, 0 for none
     * @param parts  which parts to run
     */
    record Options(int level, int seeds, Map<Integer, Set<Role>> checks, int strip, Set<Part> parts) {

        static Options parse(String[] args) {
            int level = 0;
            int seeds = 12;
            Map<Integer, Set<Role>> checks = new LinkedHashMap<>();
            int strip = 0;
            Set<Part> parts = EnumSet.allOf(Part.class);
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--level" -> level = Integer.parseInt(args[++i]);
                    case "--seeds" -> seeds = Integer.parseInt(args[++i]);
                    case "--checks" -> {
                        for (String check : args[++i].split(",")) {
                            String[] waveAndRoles = check.split(":");
                            Set<Role> roles = EnumSet.noneOf(Role.class);
                            Arrays.stream(waveAndRoles[1].split("\\+")).map(Role::named).forEach(roles::add);
                            checks.put(Integer.parseInt(waveAndRoles[0]), roles);
                        }
                    }
                    case "--strip" -> strip = Integer.parseInt(args[++i]);
                    case "--parts" -> {
                        parts = EnumSet.noneOf(Part.class);
                        for (String part : args[++i].split(",")) {
                            parts.add(Part.valueOf(part.toUpperCase(Locale.ROOT)));
                        }
                    }
                    default -> throw new IllegalArgumentException("Unknown option " + args[i]
                            + "; use --level N --seeds N --checks 6:single,12:control+sustain --strip 17"
                            + " --parts strong,replayed,margin,bounty,bans,strip");
                }
            }
            return new Options(level, seeds, Map.copyOf(checks), strip, Set.copyOf(parts));
        }
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        new WaveCheckReport(Options.parse(args), new PrintStream(System.out, true, StandardCharsets.UTF_8)).run();
    }

    private void run() {
        long started = System.nanoTime();
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(this.options.level());
        BoardSurvey survey = BoardSurvey.of(level);
        TowerLab lab = TowerLab.of(survey);
        Map<String, TowerLab> labs = Map.of(level.name(), lab);
        PlayerSpec strong = StrongPlayer.spec(BalanceReport.STRONG, labs);
        PlayerSpec competent = CompetentPlayer.spec(BalanceReport.COMPETENT, labs, CompetentPlayer.Style.standard());
        int rounds = level.paths().getFirst().waves().size();
        this.out.printf("=== %s: %d credits, %d lives, %d rounds; %d seeds ===%n", level.name(),
                level.startingCredits(), level.startingLives(), rounds, this.options.seeds());

        List<PlayRecord> strongPlays = List.of();
        if (this.options.parts().contains(Part.STRONG) || this.options.parts().contains(Part.STRIP)
                || this.options.parts().contains(Part.REPLAYED)) {
            strongPlays = this.plays(survey, strong);
            List<PlayRecord> competentPlays = this.plays(survey, competent);
            this.printPlays(level, strongPlays, competentPlays);
        }
        if (this.options.parts().contains(Part.REPLAYED)) {
            this.printMargin("strong play replayed, margin", strongPlays.parallelStream()
                    .mapToInt(play -> Margin.healthPercent(survey, ScriptedPlayer.spec("replayed", play.actions()),
                            play.play().seed(), LevelPlay.Pacing.standard()))
                    .toArray());
        }
        if (this.options.parts().contains(Part.MARGIN)) {
            this.printMargin("strong margin", this.margins(survey, strong, Rules.standard()));
        }
        if (this.options.parts().contains(Part.BOUNTY)) {
            this.printMargin("strong margin at " + (100 - BOUNTY_STEP) + "% bounty",
                    this.margins(survey, strong, Rules.standard().withBountyPercent(100 - BOUNTY_STEP)));
            this.printMargin("strong margin at " + (100 + BOUNTY_STEP) + "% bounty",
                    this.margins(survey, strong, Rules.standard().withBountyPercent(100 + BOUNTY_STEP)));
        }
        if (this.options.parts().contains(Part.BANS)) {
            this.printBans(survey, labs, strongPlays, rounds);
        }
        if (this.options.parts().contains(Part.STRIP) && this.options.strip() > 0) {
            this.printStrip(survey, strongPlays);
        }
        this.out.printf("%n(%.0fs)%n", (System.nanoTime() - started) / 1e9);
    }

    private List<PlayRecord> plays(BoardSurvey survey, PlayerSpec player) {
        return LongStream.range(FIRST_SEED, FIRST_SEED + this.options.seeds()).parallel().boxed()
                .map(seed -> LevelPlay.play(survey, player, seed, LevelPlay.Pacing.standard())).toList();
    }

    private int[] margins(BoardSurvey survey, PlayerSpec player, Rules rules) {
        return LongStream.range(FIRST_SEED, FIRST_SEED + this.options.seeds()).parallel()
                .mapToInt(seed -> Margin.healthPercent(survey, player, seed, LevelPlay.Pacing.standard(), rules))
                .toArray();
    }

    private void printMargin(String what, int[] margins) {
        Estimate estimate = Estimate.of(Arrays.stream(margins).asDoubleStream().toArray());
        this.out.printf("%n%s: %s%%  per seed %s%n", what, estimate.format(0), Arrays.toString(margins));
    }

    private void printPlays(LevelDefinition level, List<PlayRecord> strong, List<PlayRecord> competent) {
        int lives = level.startingLives();
        this.out.println();
        this.out.printf("strong wins %d of %d at authored health; competent %d of %d%n",
                strong.stream().filter(play -> play.result().won()).count(), strong.size(),
                competent.stream().filter(play -> play.result().won()).count(), competent.size());
        this.out.println("strong loses in round: " + lossRounds(strong, lives));
        this.out.println("competent loses in round: " + lossRounds(competent, lives));
        this.out.println("strong sold towers in " + strong.stream()
                .filter(play -> play.actions().stream().anyMatch(action -> isSale(action.action())))
                .count() + " of " + strong.size() + " plays");
        this.out.println();
        this.out.println("PRESSURE BY ROUND (closest: how far the furthest enemy got, 0-9; leaked: per play)");
        int rounds = level.paths().getFirst().waves().size();
        this.out.println("  round            " + IntStream.rangeClosed(1, rounds)
                .mapToObj(round -> String.format("%5d", round)).collect(Collectors.joining()));
        this.printRow("strong closest   ", strong, rounds, round -> round.pressure().closest() * 10, "%5.0f");
        this.printRow("strong leaked    ", strong, rounds, round -> round.pressure().leaked(), "%5.1f");
        this.printRow("competent closest", competent, rounds, round -> round.pressure().closest() * 10, "%5.0f");
        this.printRow("competent leaked ", competent, rounds, round -> round.pressure().leaked(), "%5.1f");
        this.printRow("strong earned    ", strong, rounds, round -> round.economy().earned(), "%5.0f");
        this.printRow("strong seconds   ", strong, rounds, round -> round.ticks() / 20.0, "%5.0f");
        this.out.println("  strong towers at the end, per play: " + strong.stream()
                .flatMap(play -> play.towers().stream())
                .collect(Collectors.groupingBy(PlayRecord.TowerLog::type, Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> String.format("%s %.1f", entry.getKey().displayName(),
                        entry.getValue() / (double) strong.size()))
                .collect(Collectors.joining(", ")));
        this.printRolesBrought(strong);
        this.out.println("  strong leaks by kind, per play: " + strong.stream()
                .flatMap(play -> play.ends().stream()).filter(PlayRecord.EnemyEnd::leaked)
                .collect(Collectors.groupingBy(end -> end.round() + ":" + TowerLab.kindKey(end.foe().kind(),
                        end.foe().rank()), Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> String.format("r%s %.1f", entry.getKey(), entry.getValue() / (double) strong.size()))
                .collect(Collectors.joining(", ")));
    }

    private static boolean isSale(PlayerAction action) {
        return switch (action) {
            case PlayerAction.Sell ignored -> true;
            case PlayerAction.Build ignored -> false;
            case PlayerAction.Upgrade ignored -> false;
        };
    }

    /** For each signature wave, how many strong plays came to it with a tower doing each role it asks for. */
    private void printRolesBrought(List<PlayRecord> strong) {
        for (Map.Entry<Integer, Set<Role>> check : new java.util.TreeMap<>(this.options.checks()).entrySet()) {
            int wave = check.getKey();
            this.out.printf("  strong brings to wave %d: %s%n", wave, check.getValue().stream()
                    .map(role -> String.format("%s %d/%d", role.label(),
                            strong.stream().filter(play -> brings(play, wave, role)).count(), strong.size()))
                    .collect(Collectors.joining(", ")));
        }
    }

    /** Whether {@code play} had built a tower of the role's types, or bought one of its nodes, before {@code wave}. */
    private static boolean brings(PlayRecord play, int wave, Role role) {
        return play.actions().stream()
                .filter(action -> action.round() < wave
                        || action.round() == wave && action.offset() == PlayRecord.TimedAction.BEFORE_ROUND)
                .anyMatch(action -> switch (action.action()) {
                    case PlayerAction.Build build -> !role.allowedTypes().contains(build.type());
                    case PlayerAction.Upgrade upgrade -> role.bannedNodes().contains(upgrade.nodeId());
                    case PlayerAction.Sell ignored -> false;
                });
    }

    private void printRow(String label, List<PlayRecord> plays, int rounds,
                          java.util.function.ToDoubleFunction<PlayRecord.Round> value, String format) {
        double[] sums = new double[rounds];
        for (PlayRecord play : plays) {
            for (PlayRecord.Round round : play.rounds()) {
                sums[round.number() - 1] += value.applyAsDouble(round) / plays.size();
            }
        }
        this.out.println("  " + label + Arrays.stream(sums).mapToObj(sum -> String.format(format, sum))
                .collect(Collectors.joining()));
    }

    /** The round each lost play was lost in, counted. */
    private static Map<Integer, Long> lossRounds(List<PlayRecord> plays, int lives) {
        Map<Integer, Long> rounds = new java.util.TreeMap<>();
        for (PlayRecord play : plays) {
            int leaked = 0;
            for (PlayRecord.Round round : play.rounds()) {
                leaked += round.pressure().leaked();
                if (leaked >= lives) {
                    rounds.merge(round.number(), 1L, Long::sum);
                    break;
                }
            }
        }
        return rounds;
    }

    /**
     * For each role, the competent player denied it from the start: the share of seeds in which each round leaked
     * heavily, and the round of the first heavy leak. Then each signature wave against its three conditions.
     */
    private void printBans(BoardSurvey survey, Map<String, TowerLab> labs, List<PlayRecord> strong, int rounds) {
        Map<Role, List<PlayRecord>> banned = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            banned.put(role, this.plays(survey, CompetentPlayer.spec("no-" + role.label(), labs,
                    CompetentPlayer.Style.without(role))));
        }
        this.out.println();
        this.out.println("ROLE BANS (competent player denied the role: share of seeds leaking heavily each round)");
        this.out.println("  round          " + IntStream.rangeClosed(1, rounds)
                .mapToObj(round -> String.format("%4d", round)).collect(Collectors.joining()));
        banned.forEach((role, plays) -> this.out.println(String.format("  %-15s", "no-" + role.label())
                + IntStream.rangeClosed(1, rounds).mapToObj(round -> String.format("%4.0f",
                        100 * share(plays, play -> leakedIn(play, round) >= HEAVY)))
                        .collect(Collectors.joining())
                + "   first heavy leak: " + firstLeaks(plays)));
        for (Map.Entry<Integer, Set<Role>> check : new java.util.TreeMap<>(this.options.checks()).entrySet()) {
            int wave = check.getKey();
            this.out.printf("  wave %d asks for %s%n", wave,
                    check.getValue().stream().map(Role::label).collect(Collectors.joining(" and ")));
            for (Role role : check.getValue()) {
                double breaks = share(banned.get(role), play -> firstHeavyLeak(play) == wave);
                this.out.printf("    a) denied %s, first heavy leak there: %.0f%% %s%n", role.label(), 100 * breaks,
                        verdict(breaks >= MOST));
            }
            if (!strong.isEmpty()) {
                double holds = share(strong, play -> leakedIn(play, wave) < HEAVY);
                this.out.printf("    b) strong holds it: %.0f%% %s%n", 100 * holds, verdict(holds >= MOST));
            }
            for (Role role : Role.values()) {
                if (!check.getValue().contains(role)) {
                    double breaks = share(banned.get(role), play -> firstHeavyLeak(play) == wave);
                    this.out.printf("    c) denied %s instead, first heavy leak there: %.0f%% %s%n", role.label(),
                            100 * breaks, verdict(breaks < MOST));
                }
            }
        }
    }

    private static String verdict(boolean ok) {
        return ok ? "ok" : "FAILS";
    }

    private static String firstLeaks(List<PlayRecord> plays) {
        return plays.stream().map(play -> {
            int first = firstHeavyLeak(play);
            return first == 0 ? "-" : String.valueOf(first);
        }).collect(Collectors.joining(" "));
    }

    /** The first round that leaked heavily, 0 for none. */
    static int firstHeavyLeak(PlayRecord play) {
        return play.rounds().stream().filter(round -> round.pressure().leaked() >= HEAVY)
                .mapToInt(PlayRecord.Round::number).findFirst().orElse(0);
    }

    static int leakedIn(PlayRecord play, int round) {
        return play.rounds().stream().filter(played -> played.number() == round)
                .mapToInt(played -> played.pressure().leaked()).sum();
    }

    private static double share(List<PlayRecord> plays, java.util.function.Predicate<PlayRecord> hit) {
        return plays.isEmpty() ? 0 : plays.stream().filter(hit).count() / (double) plays.size();
    }

    /**
     * Replays each strong play up to the strip wave, takes one role's towers off just before it, and plays the
     * wave with what is left and no further purchases.
     */
    private void printStrip(BoardSurvey survey, List<PlayRecord> strong) {
        int wave = this.options.strip();
        this.out.println();
        this.out.printf("STRIP (the strong army before wave %d, one role's towers taken off: share leaking heavily)%n",
                wave);
        List<Role> none = List.of();
        List<List<Role>> strips = new ArrayList<>();
        strips.add(none);
        Arrays.stream(Role.values()).map(List::of).forEach(strips::add);
        for (List<Role> stripped : strips) {
            List<PlayRecord> plays = strong.parallelStream().map(play -> LevelPlay.playThrough(survey,
                    new PlayerSpec("strip", (s, seed) -> new StripPlayer(play.actions(), wave, stripped)),
                    play.play().seed(), LevelPlay.Pacing.standard(), wave)).toList();
            double leaks = share(plays, play -> leakedIn(play, wave) >= HEAVY);
            String name = stripped.isEmpty() ? "full army" : "without " + stripped.getFirst().label();
            IntPredicate ok = percent -> stripped.isEmpty() ? percent < 100 * (1 - MOST) + 1e-9
                    : percent >= 100 * MOST - 1e-9;
            this.out.printf("  %-24s %3.0f%%  (%.1f leaked per play) %s%n", name, 100 * leaks,
                    plays.stream().mapToInt(play -> leakedIn(play, wave)).average().orElse(0),
                    verdict(ok.test((int) Math.round(100 * leaks))));
        }
    }

    /** Plays a script back up to a wave, takes the given roles' towers off before it, and buys nothing more. */
    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    private static final class StripPlayer implements Player {

        private final ScriptedPlayer script;
        private final int wave;
        private final List<Role> stripped;

        private boolean done;

        StripPlayer(List<PlayRecord.TimedAction> actions, int wave, List<Role> stripped) {
            this.script = new ScriptedPlayer(actions.stream().filter(action -> action.round() < wave
                    || action.round() == wave && action.offset() == PlayRecord.TimedAction.BEFORE_ROUND).toList());
            this.wave = wave;
            this.stripped = stripped;
        }

        @Override
        public void act(Turn turn) {
            this.script.act(turn);
            if (!this.done && turn.round() == this.wave && turn.betweenRounds()) {
                this.done = true;
                List<Tower> gone = turn.towers().stream()
                        .filter(tower -> this.stripped.stream().anyMatch(role -> role.doneBy(tower))).toList();
                gone.forEach(turn::discard);
            }
        }
    }
}
