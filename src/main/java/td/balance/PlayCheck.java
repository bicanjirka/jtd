package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.save.GameSave;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.wave.Point;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The play check: one level played headlessly by players that try before they buy, each allowed a different part of
 * the shop. It answers what the sheets cannot: whether the level is held with no leak by the best play found, whether
 * it is lost on bare towers and on one kind of tower alone, and whether upgrades are bought at their prices.
 * <p>
 * {@code mvn -q compile exec:java -Dexec.mainClass=td.balance.PlayCheck}; {@code --level rift} picks the level,
 * {@code --seed 7} the play's randomness, {@code --ahead 1} makes every player hold the round after the coming one
 * too, {@code --players best,sniper} plays only those and {@code --verbose} prints
 * every purchase as it is made. A full run stores {@code play.tsv}, {@code play-rounds.tsv},
 * {@code play-script.tsv} and {@code play.html}.
 */
public final class PlayCheck {

    static final String SHEET = "play";
    static final String ROUND_SHEET = "play-rounds";
    static final String SCRIPT_SHEET = "play-script";
    static final String PAGE = "play";
    static final String BEST = "best effort";
    static final String BARE = "bare towers only";

    private static final List<String> HEADER = List.of("player", "may buy", "leaks", "first leak", "lost in", "towers",
            "on towers", "on upgrades", "upgrade share", "unspent", "verdict");
    private static final List<String> ROUND_HEADER = List.of("player", "round", "unspent", "idle", "paid", "on towers",
            "on upgrades", "leaks", "leaked", "closest", "room", "bought");
    private static final List<String> SCRIPT_HEADER = List.of("player", "round", "offset", "action");
    private static final List<String> TOWER_HEADER = List.of("player", "tower", "cell", "damage", "kills", "xp",
            "nodes");
    private static final List<String> NODE_HEADER = List.of("tower", "node", "name", "tier", "price", "bought",
            "first in round");
    /** Spending under this share on upgrades is a shop whose upgrades lose to another tower. */
    private static final double FEW_UPGRADES = 0.15;
    /** The share of its pay a level may leave the best effort with as its last round starts. */
    private static final double SPARE = 0.05;
    /** Rounds the best effort may play in all while it looks for a play with no leak, the level's own included. */
    private static final int BACKTRACK_ROUNDS = 80;

    private PlayCheck() {
    }

    /** What a player is there to show. */
    enum Expect { CLEARS, LOSES }

    /** A player of the check: its name, what it may buy and what it should show. */
    record Contestant(String name, String mayBuy, SearchingPlayer.Limits limits, Expect expect) {
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        String levelName = option(args, "--level").orElse("iron");
        long seed = Long.parseLong(option(args, "--seed").orElse("1"));
        int ahead = Integer.parseInt(option(args, "--ahead").orElse("0"));
        Optional<String> only = option(args, "--players");
        if (Arrays.asList(args).contains("--verbose")) {
            ((Logger) LoggerFactory.getLogger(SearchingPlayer.class)).setLevel(Level.INFO);
        }
        LevelDefinition level = new BuiltInLevelCatalog().levels().stream()
                .filter(one -> one.name().toLowerCase(Locale.ROOT).contains(levelName.toLowerCase(Locale.ROOT)))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No level named like " + levelName));
        BoardSurvey survey = BoardSurvey.of(level);
        List<Contestant> contestants = contestants(ahead).stream()
                .filter(one -> only.isEmpty() || Arrays.stream(only.get().toLowerCase(Locale.ROOT).split(","))
                        .anyMatch(part -> one.name().toLowerCase(Locale.ROOT).contains(part)))
                .toList();
        Map<String, String> rooms = rooms(level.name());

        Map<Contestant, PlayRecord> plays = new LinkedHashMap<>();
        for (Contestant contestant : contestants) {
            long started = System.nanoTime();
            PlayRecord play = playByRounds(survey, contestant, seed);
            plays.put(contestant, play);
            System.out.printf(Locale.ROOT, "%-18s %s  (%.0f s)%n", contestant.name(),
                    verdict(contestant.expect(), play), (System.nanoTime() - started) / 1e9);
        }

        List<List<String>> verdicts = new ArrayList<>();
        List<List<String>> rounds = new ArrayList<>();
        List<List<String>> script = new ArrayList<>();
        List<List<String>> towers = new ArrayList<>();
        plays.forEach((contestant, play) -> {
            verdicts.add(row(contestant, play));
            rounds.addAll(roundRows(contestant.name(), play, rooms));
            play.actions().forEach(action -> script.add(List.of(contestant.name(), String.valueOf(action.round()),
                    String.valueOf(action.offset()), action.action().toLine())));
            play.towers().forEach(tower -> towers.add(List.of(contestant.name(), tower.type().displayName(),
                    tower.cell().x() + "," + tower.cell().y(), String.valueOf(tower.score().damage()),
                    String.valueOf(tower.score().kills()), String.valueOf(tower.score().xp()),
                    String.join(" ", tower.nodes()))));
        });
        List<ReportPage.Table> tables = new ArrayList<>();
        tables.add(ReportPage.Table.of("Verdicts", HEADER, verdicts).noted("""
                <b>leaks</b>: lives lost over the whole level, played on past the last life. <b>lost in</b>: the round \
                the level's own lives ran out. <b>on towers / on upgrades</b>: credits spent. <b>unspent</b>: credits \
                left at the end. The best effort must clear with no leak and need what the level paid before its last round; \
                every other player must lose."""));
        Optional<Contestant> best = contestants.stream().filter(one -> one.name().equals(BEST)).findFirst();
        best.ifPresent(contestant -> tables.add(ReportPage.Table.of("Upgrades the best effort bought", NODE_HEADER,
                nodeRows(plays.get(contestant))).noted(upgradeVerdict(plays.get(contestant)))));
        tables.add(ReportPage.Table.of("Rounds", ROUND_HEADER, rounds).noted("""
                <b>unspent</b>: credits idle as the round started. <b>idle</b>: those as a share of everything paid \
                before the round: near 0 is a round that took all the level had paid. <b>paid</b>: what the round paid. <b>closest</b>: \
                how far the furthest enemy got, 1 once one leaked. <b>room</b>: the wave stage's bank over need for \
                the round, when its sheet is stored.""").heating("room"));
        tables.add(ReportPage.Table.of("Towers at the end", TOWER_HEADER, towers));

        System.out.println();
        System.out.println(String.join("\t", HEADER));
        verdicts.forEach(row -> System.out.println(String.join("\t", row)));
        best.ifPresent(contestant -> {
            System.out.println();
            System.out.println(upgradeVerdict(plays.get(contestant)));
            System.out.println(String.join("\t", ROUND_HEADER));
            roundRows(contestant.name(), plays.get(contestant), rooms)
                    .forEach(row -> System.out.println(String.join("\t", row)));
        });
        if (only.isEmpty()) {
            ReportPage.store(SHEET, ReportPage.Table.of("Verdicts", HEADER, verdicts));
            ReportPage.store(ROUND_SHEET, ReportPage.Table.of("Rounds", ROUND_HEADER, rounds));
            ReportPage.store(SCRIPT_SHEET, ReportPage.Table.of("Script", SCRIPT_HEADER, script));
            ReportPage.publish(PAGE, "Play check: " + level.name(), String.format(Locale.ROOT, """
                    %s played headlessly, seed %d, by players that try before they buy: before each round a player \
                    replays its own play as far as that round, and while it leaks it tries every purchase it can \
                    afford and makes the cheapest that holds, or the one that takes the most off the leak for its \
                    price. Each player may buy a different part of the shop. Lives never run out in these plays, so \
                    a lost level still shows by how much. %d credits to start, %d lives.""", level.name(), seed,
                    level.startingCredits(), level.startingLives()), tables);
        }
    }

    /**
     * {@code contestant}'s play of the level taken round by round, each round from the game the one before it left,
     * put together as one play. A round is played once under each policy and the play goes on from the one that
     * leaked less, or with as few leaks kept more credits: as a person who saved before the round would. A player
     * that should clear also goes back: when no way of playing a round holds it, the round before is taken the other
     * way, and so on back, while the budget of rounds replayed lasts.
     */
    static PlayRecord playByRounds(BoardSurvey survey, Contestant contestant, long seed) {
        int count = survey.level().paths().getFirst().waves().size();
        List<LevelPlay.Segment> chain = List.of();
        if (contestant.expect() == Expect.CLEARS) {
            chain = clean(survey, contestant, seed, 1, Optional.empty(), new int[]{BACKTRACK_ROUNDS}).orElse(List.of());
        }
        if (chain.isEmpty()) {
            List<LevelPlay.Segment> played = new ArrayList<>();
            Optional<GameSave> from = Optional.empty();
            for (int round = 1; round <= count && (round == 1 || from.isPresent()); round++) {
                LevelPlay.Segment segment = ways(survey, contestant, seed, from, round).getFirst();
                played.add(segment);
                from = segment.after();
            }
            chain = played;
        }
        List<PlayRecord.Round> rounds = new ArrayList<>();
        List<PlayRecord.EnemyEnd> ends = new ArrayList<>();
        List<PlayRecord.TimedAction> actions = new ArrayList<>();
        for (LevelPlay.Segment segment : chain) {
            rounds.addAll(segment.record().rounds());
            ends.addAll(segment.record().ends());
            actions.addAll(segment.record().actions());
        }
        PlayRecord last = chain.getLast().record();
        int leaks = rounds.stream().mapToInt(round -> round.pressure().leaked()).sum();
        return new PlayRecord(new PlayRecord.Play(survey.level().name(), seed, contestant.name()),
                new PlayRecord.Result(survey.level().startingLives(), leaks, rounds.size(), count), rounds,
                new PlayRecord.Log(ends, actions, last.towers(), last.watch()));
    }

    /** Round {@code round} from {@code from}, played once under each policy, the better way first. */
    private static List<LevelPlay.Segment> ways(BoardSurvey survey, Contestant contestant, long seed,
                                                Optional<GameSave> from, int round) {
        List<LevelPlay.Segment> ways = new ArrayList<>();
        for (SearchingPlayer.Policy policy : SearchingPlayer.Policy.values()) {
            ways.add(LevelPlay.playRound(survey, SearchingPlayer.spec(contestant.name(), contestant.limits(), policy),
                    seed, LevelPlay.Pacing.standard(), from, round));
        }
        ways.sort((one, other) -> isBetter(one, other) ? -1 : isBetter(other, one) ? 1 : 0);
        return ways;
    }

    /**
     * A play from round {@code round} on that leaks nowhere, if one is found before {@code budget} runs out: each
     * round played is one off it. A way that leaks is not followed.
     */
    private static Optional<List<LevelPlay.Segment>> clean(BoardSurvey survey, Contestant contestant, long seed,
                                                           int round, Optional<GameSave> from, int[] budget) {
        int count = survey.level().paths().getFirst().waves().size();
        if (budget[0]-- <= 0) {
            return Optional.empty();
        }
        for (LevelPlay.Segment way : ways(survey, contestant, seed, from, round)) {
            if (way.record().result().leaks() > 0) {
                continue;
            }
            if (round == count) {
                return Optional.of(List.of(way));
            }
            Optional<List<LevelPlay.Segment>> rest = clean(survey, contestant, seed, round + 1, way.after(), budget);
            if (rest.isPresent()) {
                List<LevelPlay.Segment> chain = new ArrayList<>(List.of(way));
                chain.addAll(rest.get());
                return Optional.of(chain);
            }
        }
        return Optional.empty();
    }

    /** Whether {@code one} left its round better than {@code other}: fewer leaks, then more credits. */
    private static boolean isBetter(LevelPlay.Segment one, LevelPlay.Segment other) {
        int leaks = one.record().result().leaks();
        int otherLeaks = other.record().result().leaks();
        return leaks < otherLeaks || (leaks == otherLeaks && credits(one) > credits(other));
    }

    private static int credits(LevelPlay.Segment segment) {
        PlayRecord.Round round = segment.record().rounds().getLast();
        return round.economy().creditsAtStart() + round.economy().income().total();
    }

    private static Optional<String> option(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return Optional.of(args[i + 1]);
            }
        }
        return Optional.empty();
    }

    /** The best effort, the same on bare towers, and one player per judged tower that builds nothing else. */
    static List<Contestant> contestants(int ahead) {
        Set<TowerFactory.Type> judged = EnumSet.noneOf(TowerFactory.Type.class);
        Arrays.stream(TowerFactory.Type.values()).filter(type -> TowerSheet.isJudged(type.name())).forEach(judged::add);
        List<Contestant> contestants = new ArrayList<>();
        contestants.add(new Contestant(BEST, "every judged tower, every node",
                new SearchingPlayer.Limits(judged, true, ahead), Expect.CLEARS));
        contestants.add(new Contestant(BARE, "every judged tower, no node",
                new SearchingPlayer.Limits(judged, false, ahead), Expect.LOSES));
        for (TowerFactory.Type type : judged) {
            contestants.add(new Contestant(type.displayName() + " only", "the " + type.displayName() + " and its nodes",
                    new SearchingPlayer.Limits(EnumSet.of(type), true, ahead), Expect.LOSES));
        }
        return contestants;
    }

    /** The round {@code play} first leaked in, 0 for none. */
    static int firstLeak(PlayRecord play) {
        return play.rounds().stream().filter(round -> round.pressure().leaked() > 0)
                .mapToInt(PlayRecord.Round::number).findFirst().orElse(0);
    }

    /** The round {@code play}'s leaks used up the level's lives in, 0 for none. */
    static int lostIn(PlayRecord play) {
        int leaked = 0;
        for (PlayRecord.Round round : play.rounds()) {
            leaked += round.pressure().leaked();
            if (leaked >= play.result().lives()) {
                return round.number();
            }
        }
        return 0;
    }

    /** What {@code play} shows of a player that should {@code expect}. */
    static String verdict(Expect expect, PlayRecord play) {
        int leaks = play.result().leaks();
        boolean finished = play.result().roundsCleared() == play.result().roundCount();
        if (expect == Expect.CLEARS) {
            if (!finished) {
                return "FAIL: the play ran out of time in round " + (play.result().roundsCleared() + 1);
            }
            return leaks == 0 ? "ok: clears with no leak"
                    : "FAIL: leaks " + leaks + ", first in round " + firstLeak(play);
        }
        if (lostIn(play) > 0) {
            return "ok: loses in round " + lostIn(play);
        }
        return leaks == 0 && finished ? "BREACH: clears with no leak"
                : "WARN: leaks " + leaks + " of " + play.result().lives() + " lives and survives";
    }

    private static List<String> row(Contestant contestant, PlayRecord play) {
        int onTowers = play.rounds().stream().mapToInt(round -> round.economy().spent().towers()).sum();
        int onUpgrades = play.rounds().stream().mapToInt(round -> round.economy().spent().upgrades()).sum();
        PlayRecord.Round last = play.rounds().getLast();
        return List.of(contestant.name(), contestant.mayBuy(), String.valueOf(play.result().leaks()),
                dashed(firstLeak(play)), dashed(lostIn(play)), String.valueOf(play.towers().size()),
                String.valueOf(onTowers), String.valueOf(onUpgrades),
                ReportPage.number(onUpgrades / (double) Math.max(1, onTowers + onUpgrades)),
                String.valueOf(last.economy().creditsAtStart() + last.economy().income().total()),
                contestant.expect() == Expect.CLEARS && play.result().leaks() == 0
                        ? verdict(contestant.expect(), play) + "; " + slackVerdict(play)
                        : verdict(contestant.expect(), play));
    }

    /**
     * The share of everything {@code play} had been paid before round {@code round} that sat unspent as the round
     * started: what the level had paid and the rounds so far had not asked for.
     */
    static double idleShare(PlayRecord play, int round) {
        int spent = 0;
        for (PlayRecord.Round one : play.rounds()) {
            spent += one.economy().spent().total();
            if (one.number() == round) {
                int unspent = one.economy().creditsAtStart();
                return unspent / (double) Math.max(1, unspent + spent);
            }
        }
        return 0;
    }

    /**
     * Whether a play that held the level needed what the level paid. What the last round pays buys nothing, so the
     * measure is what sat unspent as the last round started.
     */
    static String slackVerdict(PlayRecord play) {
        PlayRecord.Round last = play.rounds().getLast();
        double idle = idleShare(play, last.number());
        String share = percent(idle) + " of what the level paid before its last round";
        if (idle <= SPARE) {
            return "ok: all but " + share + " is needed";
        }
        return "WARN: " + last.economy().creditsAtStart() + " credits, " + share
                + ", are never needed: the level pays more than it asks";
    }

    private static String dashed(int round) {
        return round == 0 ? "-" : String.valueOf(round);
    }

    private static List<List<String>> roundRows(String player, PlayRecord play, Map<String, String> rooms) {
        List<List<String>> rows = new ArrayList<>();
        for (PlayRecord.Round round : play.rounds()) {
            String bought = play.actions().stream().filter(action -> action.round() == round.number())
                    .map(action -> action.action().toLine()).collect(Collectors.joining("; "));
            rows.add(List.of(player, String.valueOf(round.number()), String.valueOf(round.economy().creditsAtStart()),
                    ReportPage.number(idleShare(play, round.number())),
                    String.valueOf(round.economy().income().total()), String.valueOf(round.economy().spent().towers()),
                    String.valueOf(round.economy().spent().upgrades()), String.valueOf(round.pressure().leaked()),
                    leaked(play, round.number()),
                    ReportPage.number(round.pressure().closest()),
                    rooms.getOrDefault(String.valueOf(round.number()), ""), bought));
        }
        return rows;
    }

    /** What leaked in round {@code round} of {@code play}: each kind at its rank, with how many. */
    private static String leaked(PlayRecord play, int round) {
        Map<String, Integer> kinds = new LinkedHashMap<>();
        play.ends().stream().filter(end -> end.leaked() && end.round() == round)
                .forEach(end -> kinds.merge(end.foe().rank().name().toLowerCase(Locale.ROOT) + " " + end.foe().kind(), 1,
                        Integer::sum));
        return kinds.entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    /** Each node {@code play} bought: on how many towers, and the round it first was. */
    private static List<List<String>> nodeRows(PlayRecord play) {
        Map<Point, TowerFactory.Type> built = new LinkedHashMap<>();
        Map<String, int[]> bought = new LinkedHashMap<>();
        Map<String, TowerFactory.Type> owner = new LinkedHashMap<>();
        for (PlayRecord.TimedAction action : play.actions()) {
            switch (action.action()) {
                case PlayerAction.Build build -> built.put(build.cell(), build.type());
                case PlayerAction.Upgrade upgrade -> {
                    owner.put(upgrade.nodeId(), built.get(upgrade.cell()));
                    bought.computeIfAbsent(upgrade.nodeId(), id -> new int[]{0, action.round()})[0]++;
                }
                case PlayerAction.Sell sell -> built.remove(sell.cell());
            }
        }
        List<List<String>> rows = new ArrayList<>();
        bought.forEach((id, count) -> {
            TowerFactory.Type type = owner.get(id);
            Optional<UpgradeNode> node = TowerBench.treeOf(type).nodes().stream()
                    .filter(one -> one.id().equals(id)).findFirst();
            rows.add(List.of(type.displayName(), id, node.map(UpgradeNode::displayName).orElse(""),
                    node.map(one -> String.valueOf(one.tier())).orElse(""),
                    node.map(one -> String.valueOf(one.price())).orElse(""), String.valueOf(count[0]),
                    String.valueOf(count[1])));
        });
        return rows;
    }

    /** Whether the best effort spent on upgrades at all, and how its spending moved to them through the level. */
    static String upgradeVerdict(PlayRecord play) {
        int thirds = 3;
        int[] towers = new int[thirds];
        int[] upgrades = new int[thirds];
        for (PlayRecord.Round round : play.rounds()) {
            int third = Math.min(thirds - 1, (round.number() - 1) * thirds / play.result().roundCount());
            towers[third] += round.economy().spent().towers();
            upgrades[third] += round.economy().spent().upgrades();
        }
        int all = Arrays.stream(towers).sum() + Arrays.stream(upgrades).sum();
        double share = Arrays.stream(upgrades).sum() / (double) Math.max(1, all);
        StringBuilder text = new StringBuilder(share == 0 ? "WARN: no upgrade is bought at these prices"
                : share < FEW_UPGRADES ? "WARN: upgrades take only " + percent(share) + " of what is spent"
                : "ok: upgrades take " + percent(share) + " of what is spent");
        text.append("; by third of the level:");
        for (int i = 0; i < thirds; i++) {
            text.append(' ').append(percent(upgrades[i] / (double) Math.max(1, towers[i] + upgrades[i])));
        }
        return text.toString();
    }

    private static String percent(double share) {
        return Math.round(share * 100) + "%";
    }

    /** The wave stage's room per round of {@code level}, when its sheet is stored. */
    private static Map<String, String> rooms(String level) {
        Map<String, String> rooms = new LinkedHashMap<>();
        if (!Files.exists(ReportPage.FOLDER.resolve(WavePowerReport.SHEET + ".tsv"))) {
            return rooms;
        }
        List<List<String>> sheet = ReportPage.load(WavePowerReport.SHEET);
        int round = sheet.getFirst().indexOf("round");
        int room = sheet.getFirst().indexOf("room");
        sheet.stream().skip(1).filter(row -> row.getFirst().equals(level))
                .forEach(row -> rooms.put(row.get(round), row.get(room)));
        return rooms;
    }
}
