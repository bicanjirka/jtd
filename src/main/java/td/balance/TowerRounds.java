package td.balance;

import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeTier;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Every tower at every round of the reference level: the most power the bank buys in copies of it, with upgrades only
 * of the tiers its XP gates have opened by then, against its rivals. XP is a lane's bounty since the tower was built,
 * so a gate is a round; the bank is the level's credits before the round. Copies add their power, which ignores
 * overlap: the same for every tower, so it ranks them and does not size a defence.
 */
final class TowerRounds {

    static final String LEVEL = "Iron Crossing";
    static final String SHEET = "tower-rounds";
    /** A tower whose best defence holds this much more or less than the median of the round is out of line. */
    static final double STRONG = 1.25;
    static final double WEAK = 0.8;
    /** A tower is assumed built before this round when the report says how long a late one waits for its top tier. */
    static final int LATE_BUILD = 10;
    private static final int TIERS = 3;
    private static final double EMPTY = 0.1;
    /** Gates whose spread is this close to the least are as good: the bench moves a build by more than that. */
    private static final double TOLERANCE = 0.01;
    private static final int ROUND_TO = 10;

    /** One way to buy the tower: its top tier, what it costs over the bare tower, and the power it adds. */
    record Build(String label, int stage, int extra, double power) {
    }

    /** One round: the credits banked before it and the XP a tower on each lane has by then, built before the first. */
    record Round(int round, int bank, int xp, int poorXp) {
    }

    /** {@code copies} of {@code build}, what they cost together and the power they add. */
    record Defence(int copies, String build, int cost, double power) {

        static Defence none() {
            return new Defence(0, "-", 0, 0);
        }

        String label() {
            return this.copies == 0 ? "-" : this.copies + " x " + this.build;
        }
    }

    private final List<Round> rounds;
    private final Map<String, String> names;
    private final Map<String, Integer> prices;
    private final Map<String, List<Build>> builds;
    private final Map<String, List<Integer>> gates;

    TowerRounds(List<Round> rounds, Map<String, String> names, Map<String, Integer> prices, Map<String, List<Build>> builds,
                Map<String, List<Integer>> gates) {
        this.rounds = List.copyOf(rounds);
        this.names = Map.copyOf(names);
        this.prices = Map.copyOf(prices);
        this.builds = new LinkedHashMap<>(builds);
        this.gates = Map.copyOf(gates);
    }

    /** The judged towers of {@code sheet} on the stored rounds of the reference level, or nothing before a wave run. */
    static Optional<TowerRounds> stored(TowerSheet sheet) {
        if (!Files.exists(ReportPage.FOLDER.resolve(WavePowerReport.SHEET + ".tsv"))) {
            return Optional.empty();
        }
        List<Round> rounds = roundsOf(ReportPage.load(WavePowerReport.SHEET), LEVEL);
        if (rounds.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, Integer> prices = new LinkedHashMap<>();
        Map<String, List<Build>> builds = new LinkedHashMap<>();
        Map<String, List<Integer>> gates = new LinkedHashMap<>();
        for (String tower : sheet.rivals()) {
            TowerSheet.Held bare = sheet.bare(tower);
            names.put(tower, bare.name());
            prices.put(tower, bare.cost());
            List<Build> own = new ArrayList<>();
            for (int stage = -1; stage <= sheet.topStage(tower); stage++) {
                for (TowerSheet.Held held : sheet.stage(tower, stage)) {
                    own.add(new Build(held.label(), stage, held.cost() - bare.cost(), sheet.power(held)));
                }
            }
            builds.put(tower, own);
            List<UpgradeTier> tiers = TowerBench.treeOf(TowerFactory.Type.valueOf(tower)).tiers();
            gates.put(tower, tiers.stream().skip(1).map(UpgradeTier::xp).toList());
        }
        return Optional.of(new TowerRounds(rounds, names, prices, builds, gates));
    }

    /** {@code level}'s rounds as the wave stage stored them; the richer lane is the one with the most XP at the end. */
    static List<Round> roundsOf(List<List<String>> sheet, String level) {
        List<String> header = sheet.getFirst();
        int name = header.indexOf("level");
        int round = header.indexOf("round");
        int bank = header.indexOf("bank");
        int xp = header.indexOf("xp");
        List<List<String>> own = sheet.stream().skip(1).filter(row -> row.get(name).equals(level)).toList();
        if (own.isEmpty()) {
            return List.of();
        }
        int[] last = lanes(own.getLast().get(xp));
        int rich = IntStream.range(0, last.length).reduce((a, b) -> last[b] > last[a] ? b : a).orElse(0);
        int poor = IntStream.range(0, last.length).reduce((a, b) -> last[b] < last[a] ? b : a).orElse(0);
        return own.stream().map(row -> {
            int[] lanes = lanes(row.get(xp));
            return new Round(Integer.parseInt(row.get(round)), (int) Math.round(Double.parseDouble(row.get(bank))),
                    lanes[rich], lanes[poor]);
        }).toList();
    }

    private static int[] lanes(String cell) {
        return Arrays.stream(cell.split("\\|")).map(String::trim).mapToInt(Integer::parseInt).toArray();
    }

    /** What {@code copies} towers listed at {@code price} cost bought one after another, each dearer than the last. */
    static int copiesCost(int price, int copies) {
        int total = 0;
        for (int i = 0; i < copies; i++) {
            total += Math.toIntExact(Math.round(price * Math.pow(TowerFactory.Type.COPY_PRICE_GROWTH, i)));
        }
        return total;
    }

    /** The highest tier open at {@code xp}, with {@code gates} the XP of tiers I and up; tier 0 is always open. */
    static int openTier(List<Integer> gates, int xp) {
        int open = 0;
        for (int tier = 0; tier < gates.size() && gates.get(tier) <= xp; tier++) {
            open = tier + 1;
        }
        return open;
    }

    /** The most power {@code bank} buys in copies of one build of a tower listed at {@code price}, up to tier {@code open}. */
    static Defence best(List<Build> builds, int price, int bank, int open) {
        Defence best = Defence.none();
        for (Build build : builds) {
            if (build.stage() > open) {
                continue;
            }
            for (int copies = 1; ; copies++) {
                int cost = copiesCost(price, copies) + copies * build.extra();
                if (cost > bank) {
                    break;
                }
                double power = copies * build.power();
                if (power > best.power() || power == best.power() && cost < best.cost()) {
                    best = new Defence(copies, build.label(), cost, power);
                }
            }
        }
        return best;
    }

    /** {@code tower}'s best defence before every round, with its tiers opening at {@code gates}. */
    private List<Defence> defences(String tower, List<Integer> gates) {
        return this.rounds.stream().map(round -> best(this.builds.get(tower), this.prices.get(tower), round.bank(),
                openTier(gates, round.xp()))).toList();
    }

    /** The median power of the round over {@code all}, one list of defences per tower. */
    private static double[] medians(List<List<Defence>> all, int rounds) {
        double[] medians = new double[rounds];
        for (int r = 0; r < rounds; r++) {
            int at = r;
            double[] sorted = all.stream().mapToDouble(defences -> defences.get(at).power()).sorted().toArray();
            int n = sorted.length;
            medians[r] = n == 0 ? 0 : n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
        }
        return medians;
    }

    /** How far {@code defences} stray from {@code reference} over the level: the root mean square of the log ratio. */
    static double spread(List<Defence> defences, double[] reference) {
        double sum = 0;
        for (int r = 0; r < reference.length; r++) {
            double ratio = reference[r] <= 0 ? 1 : Math.max(EMPTY, defences.get(r).power() / reference[r]);
            sum += Math.log(ratio) * Math.log(ratio);
        }
        return Math.sqrt(sum / reference.length);
    }

    private Map<String, List<Defence>> all(Map<String, List<Integer>> gating) {
        Map<String, List<Defence>> all = new LinkedHashMap<>();
        this.builds.keySet().forEach(tower -> all.put(tower, this.defences(tower, gating.get(tower))));
        return all;
    }

    /** Each tower round by round with the gates it has: its best defence against the median of the round. */
    ReportPage.Table table() {
        return this.pivot("Rounds", this.gates, """
                Each tower before each round of %s, built before the first and standing on its richer lane: the most \
                power the bank buys in copies of one build of it, upgraded only as far as its gates have opened, \
                over the median of the judged towers that round. Its own gates are in the second column (XP = round \
                it opens). Copies add their power, so the table ranks towers and does not size a defence.""");
    }

    private ReportPage.Table pivot(String title, Map<String, List<Integer>> gating, String note) {
        Map<String, List<Defence>> all = this.all(gating);
        double[] medians = medians(List.copyOf(all.values()), this.rounds.size());
        List<String> header = new ArrayList<>(List.of("tower", "name", "gates"));
        this.rounds.forEach(round -> header.add("r" + round.round()));
        header.add("spread");
        header.add("verdict");
        List<List<String>> rows = new ArrayList<>();
        for (Map.Entry<String, List<Defence>> entry : all.entrySet()) {
            List<String> row = new ArrayList<>(List.of(entry.getKey(), this.names.get(entry.getKey()),
                    this.gatesText(gating.get(entry.getKey()))));
            List<Integer> strong = new ArrayList<>();
            List<Integer> weak = new ArrayList<>();
            for (int r = 0; r < this.rounds.size(); r++) {
                double ratio = medians[r] <= 0 ? 1 : entry.getValue().get(r).power() / medians[r];
                row.add(ReportPage.number(ratio));
                if (ratio > STRONG) {
                    strong.add(this.rounds.get(r).round());
                } else if (ratio < WEAK) {
                    weak.add(this.rounds.get(r).round());
                }
            }
            row.add(ReportPage.number(spread(entry.getValue(), medians)));
            row.add(verdict(strong, weak));
            rows.add(row);
        }
        String[] heated = this.rounds.stream().map(round -> "r" + round.round()).toArray(String[]::new);
        return ReportPage.Table.of(title, header, rows).noted(String.format(Locale.ROOT, note, LEVEL)).heating(heated);
    }

    private static String verdict(List<Integer> strong, List<Integer> weak) {
        List<String> parts = new ArrayList<>();
        if (!strong.isEmpty()) {
            parts.add("DOMINANT rounds " + roundsText(strong));
        }
        if (!weak.isEmpty()) {
            parts.add((parts.isEmpty() ? "WARN " : "") + "weak rounds " + roundsText(weak));
        }
        return parts.isEmpty() ? TowerReport.OK : String.join("; ", parts);
    }

    private static String roundsText(List<Integer> rounds) {
        return String.join(",", rounds.stream().map(String::valueOf).toList());
    }

    private String gatesText(List<Integer> gating) {
        return String.join(" / ", gating.stream().map(xp -> xp + " = r" + this.opening(xp)).toList());
    }

    /** The first round a tower built before the first has {@code xp} by, on its richer lane; one past the last if never. */
    private int opening(int xp) {
        return this.rounds.stream().filter(round -> round.xp() >= xp).mapToInt(Round::round).findFirst()
                .orElse(this.rounds.getLast().round() + 1);
    }

    /**
     * The gates that keep each tower closest to the pace the towers have now: for every tower, the opening rounds of
     * its tiers whose defences stray least from the median defence of the round under today's gates. Today's median
     * anchors the pace; without it every gate would drift to never, where bare copies are all alike. Of the gates
     * within the bench's noise of the least spread, the nearest to today's rounds wins, so noise moves no gate, and no
     * top gate makes a late tower wait longer than today's does.
     */
    Map<String, List<Integer>> proposed() {
        double[] reference = medians(List.copyOf(this.all(this.gates).values()), this.rounds.size());
        Map<String, List<Integer>> proposal = new LinkedHashMap<>();
        int last = this.rounds.size();
        for (String tower : this.builds.keySet()) {
            List<Integer> now = this.gates.get(tower);
            int[] opens = now.stream().mapToInt(this::opening).toArray();
            int waitLimit = this.waitOf(now.get(TIERS - 1));
            List<List<Integer>> trials = new ArrayList<>();
            List<Double> spreads = new ArrayList<>();
            for (int first = 1; first <= last; first++) {
                for (int second = first; second <= last; second++) {
                    for (int third = second; third <= last; third++) {
                        List<Integer> trial = List.of(this.gateFor(first), this.gateFor(second), this.gateFor(third));
                        if (this.waitOf(trial.get(TIERS - 1)) <= waitLimit) {
                            trials.add(trial);
                            spreads.add(spread(this.defences(tower, trial), reference));
                        }
                    }
                }
            }
            double least = spreads.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            List<Integer> chosen = now;
            int nearest = Integer.MAX_VALUE;
            for (int i = 0; i < trials.size(); i++) {
                if (spreads.get(i) > least + TOLERANCE) {
                    continue;
                }
                List<Integer> trial = trials.get(i);
                int distance = IntStream.range(0, TIERS).map(tier -> Math.abs(this.opening(trial.get(tier)) - opens[tier]))
                        .sum();
                if (distance < nearest) {
                    nearest = distance;
                    chosen = trial;
                }
            }
            proposal.put(tower, this.keepingToday(now, chosen));
        }
        return proposal;
    }

    /** Rounds a tower built before {@link #LATE_BUILD} on the poorer lane waits until it has {@code xp}; past the level, one more than it has. */
    private int waitOf(int xp) {
        int from = this.rounds.stream().filter(round -> round.round() == LATE_BUILD).mapToInt(Round::poorXp).findFirst()
                .orElse(0);
        return this.rounds.stream().filter(round -> round.round() > LATE_BUILD && round.poorXp() - from >= xp)
                .mapToInt(round -> round.round() - LATE_BUILD).findFirst()
                .orElse(this.rounds.getLast().round() - LATE_BUILD + 1);
    }

    /** The lowest round XP that opens a tier before round {@code round} and not one round sooner, on the richer lane. */
    private int gateFor(int round) {
        int at = this.rounds.get(round - 1).xp();
        if (round == 1) {
            return 0;
        }
        int before = this.rounds.get(round - 2).xp();
        int rounded = (before / ROUND_TO + 1) * ROUND_TO;
        return rounded <= at ? rounded : at;
    }

    /** {@code trial}, keeping today's gate wherever the trial opens the tier in the same round. */
    private List<Integer> keepingToday(List<Integer> now, List<Integer> trial) {
        return IntStream.range(0, TIERS)
                .mapToObj(tier -> this.opening(trial.get(tier)) == this.opening(now.get(tier)) ? now.get(tier) : trial.get(tier))
                .toList();
    }

    /** The proposal against today's gates: spreads, and how long a tower built late on the poorer lane waits. */
    ReportPage.Table proposal(Map<String, List<Integer>> proposal) {
        double[] now = medians(List.copyOf(this.all(this.gates).values()), this.rounds.size());
        double[] then = medians(List.copyOf(this.all(proposal).values()), this.rounds.size());
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.builds.keySet()) {
            List<Integer> was = this.gates.get(tower);
            List<Integer> next = proposal.get(tower);
            rows.add(List.of(tower, this.names.get(tower), this.gatesText(was), this.gatesText(next),
                    ReportPage.number(spread(this.defences(tower, was), now)),
                    ReportPage.number(spread(this.defences(tower, next), then)),
                    this.lateWait(was.get(TIERS - 1)), this.lateWait(next.get(TIERS - 1))));
        }
        return ReportPage.Table.of("Proposed gates", List.of("tower", "name", "gates now", "proposed", "spread now",
                "spread proposed", "late wait now", "late wait proposed"), rows).noted(String.format(Locale.ROOT, """
                Each tower's tier I / II / III gates (XP = round it opens on the richer lane) that keep its best \
                defence closest to the median defence of each round under today's gates. spread: root mean square \
                of the log of its defence over the median of the round, 0 = always the median. late wait: rounds a \
                tower built before round %d on the poorer lane waits for its top tier.""", LATE_BUILD));
    }

    private String lateWait(int xp) {
        int wait = this.waitOf(xp);
        int most = this.rounds.getLast().round() - LATE_BUILD;
        return wait > most ? "> " + most : String.valueOf(wait);
    }

    /** The rounds table again with {@code proposal}'s gates. */
    ReportPage.Table withGates(Map<String, List<Integer>> proposal) {
        return this.pivot("Rounds with the proposed gates", proposal, """
                The Rounds table of %s again, every tower on its proposed gates, against the median of the round \
                under those gates.""");
    }
}
