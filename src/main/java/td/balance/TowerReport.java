package td.balance;

import td.balance.TowerSheet.Held;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Stage 3 of the balance pipeline, judged: what the bench measured, turned into prices and verdicts.
 *
 * <p>A build's power is worth credits at the going rate, the power a credit buys of a bare tower. A tower's fair
 * price is its bare worth. A node's worth is its share of what its tree adds to the bare tower, and its price is that
 * worth times a premium that grows with its tier: prices follow what the nodes do, and a tier's nodes come to what
 * they come to. Each tier's total is then set against the pace the game wants of its tiers and against the same tier
 * of the other towers, which is where a tier whose nodes do too little or too much shows. The towers' roles, the
 * balance inside each cluster, towers that pay better than every rival, and pairs that hold far more together than
 * apart are each a table with a verdict. A node that moves nothing measured is listed as unmeasured and keeps its
 * price.
 *
 * <p>Run it after the bench, which stores what this reads.
 */
public final class TowerReport {

    static final String PRICE_SHEET = "tower-prices";
    static final String STAGE_SHEET = "tower-stages";
    static final String OK = "ok";
    private static final String PAGE = "towers";
    private static final String NOT_JUDGED = "not judged";
    private static final String NONE = "-";
    /** Two readings closer than this are one reading: what a rerun of the bench moves a cell by. */
    static final double NOISE = 0.05;
    /** A node that moves a build's power by less than this, whatever it is bought on, is not measured. */
    static final double QUIET = 0.025;
    /** How far a price may sit from the one suggested and still be right. */
    static final double PRICE_BAND = 0.25;
    /** The credits a small price may be off by: under it, a rerun of the bench moves the suggestion as far. */
    static final int SMALL_CHANGE = 2;
    private static final double TOWER_BAND = 0.2;
    /** How far above and below its rivals of the same tier a tower's power per credit may sit. */
    private static final double STRONG = 1.25;
    private static final double WEAK = 0.8;
    /** The share of a mixed wave one tower may be the best buy against before it is the answer to everything. */
    private static final double GO_TO = 0.6;
    /** The least a cluster member may hold of what its best sibling holds. */
    static final double PICK_BAND = 0.85;
    /** How far apart two towers a rule calls about equal may be. */
    static final double NEAR = 0.75;
    /** The share of a whole tree's power one node may carry. */
    private static final double KEYSTONE = 0.35;
    /** The share of a whole tree's power a node may cost it before it is a trap. */
    private static final double SINKS = 2 * NOISE;
    private static final double SYNERGY_HIGH = 2;
    private static final double SYNERGY_LOW = 0.7;
    private static final double MOVE = 1.12;
    private static final int MOVES = 3;
    private static final int READABLE = 5;
    private static final int PRICE = 4;
    private static final int SUGGESTED = 10;
    /** The least a node costs, in tower prices, for each tier it is up from the tower: no node is next to free. */
    private static final double FLOOR = 0.1;
    /** The most the first tier may cost, and the band the second sits in, in tower prices. */
    private static final double FIRST_TIER_MOST = 1.0;
    private static final double SECOND_TIER_LEAST = 0.6;
    private static final double SECOND_TIER_MOST = 1.6;
    /** How far a tier's cost may sit from the same tier of the other towers, each in its own tower's prices. */
    private static final double TIER_APART = 2;
    private static final List<Rule> RULES = List.of(
            Rule.of("The Sniper is the top single-target tower", Relation.CEILING, "SNIPER", "single", "stream", "boss"),
            Rule.of("Against armor and plating the Aerie is the top single-target tower", Relation.CEILING, "SEEKER",
                    "armored", "plated"),
            Rule.of("The Burst before its fork is the weakest area tower", Relation.FLOOR, "SPLASH", "group")
                    .beforeFork().against("MORTAR", "CINDER"),
            Rule.of("The Burst before its fork is about equal to the Obelisk on a group", Relation.EQUAL, "SPLASH", "group")
                    .beforeFork().against("PULSE"),
            Rule.of("No tower is weaker on one target than the Burst before its fork", Relation.FLOOR, "SPLASH",
                    "single", "stream").beforeFork());

    private final TowerSheet sheet;
    private final Map<String, List<Cluster>> clusters;
    private final List<List<String>> pairs;

    TowerReport(TowerSheet sheet, Map<String, List<Cluster>> clusters, List<List<String>> pairs) {
        this.sheet = sheet;
        this.clusters = Map.copyOf(clusters);
        this.pairs = List.copyOf(pairs);
    }

    /** Judges the stored bench sheets, stores the prices and stages, and writes the page. */
    public static void main(String[] args) {
        TowerSheet sheet = TowerSheet.stored();
        Map<String, List<Cluster>> clusters = new LinkedHashMap<>();
        for (String tower : sheet.towers()) {
            UpgradeTree tree = TowerBench.treeOf(TowerFactory.Type.valueOf(tower));
            List<Cluster> found = new ArrayList<>();
            tree.fork().ifPresent(fork -> found.add(new Cluster(fork.name(), fork.memberIds(), true)));
            tree.clusters().forEach(cluster -> found.add(new Cluster(cluster.name(), cluster.memberIds(), false)));
            clusters.put(tower, found);
        }
        TowerReport report = new TowerReport(sheet, clusters, ReportPage.load(TowerBench.PAIR_SHEET));
        List<ReportPage.Table> tables = new ArrayList<>(List.of(report.towers(), report.stages(), report.roles(),
                report.profiles(), report.tiers(), report.nodes(), report.clusterPicks(), report.synergies()));
        Optional<TowerRounds> rounds = TowerRounds.stored(sheet);
        rounds.ifPresentOrElse(byRound -> {
            Map<String, List<Integer>> proposal = byRound.proposed();
            tables.add(byRound.table());
            tables.add(byRound.proposal(proposal));
            tables.add(byRound.withGates(proposal));
            ReportPage.store(TowerRounds.SHEET, byRound.table());
        }, () -> System.out.println("No " + TowerRounds.LEVEL + " rounds in " + WavePowerReport.SHEET
                + ".tsv: run WavePowerReport for the Rounds tables"));
        ReportPage.Table findings = report.findings(tables);
        ReportPage.store(PRICE_SHEET, report.prices());
        ReportPage.store(STAGE_SHEET, tables.get(1));
        List<ReportPage.Table> page = new ArrayList<>(List.of(findings));
        page.addAll(tables);
        ReportPage.publish(PAGE, "Towers", String.format(Locale.ROOT, """
                Stage 3 of the balance pipeline, judged. A build's <b>power</b> is the Walkers of a mixed wave it adds to
                two fixed partners; its <b>worth</b> is that power in credits at the going rate of %.3f Walkers a credit,
                what the judged towers sell bare. Every table ends in a verdict; the first table gathers the ones that
                break a rule. Click a column to sort.""", sheet.rate()), page);
        System.out.printf(Locale.ROOT, "Going rate: %.3f Walkers of a mixed wave a credit%n%n", sheet.rate());
        print(tables.get(0));
        print(tables.get(1));
        print(findings);
        tables.stream().filter(table -> table.title().contains("Rounds") || table.title().contains("gates"))
                .forEach(TowerReport::print);
        tables.stream().filter(table -> table.header().getLast().equals("verdict"))
                .forEach(table -> System.out.printf(Locale.ROOT, "%-34s %s%n", table.title(), tally(table)));
        System.out.println("Stored in " + ReportPage.FOLDER + ": " + PRICE_SHEET + ".tsv, " + STAGE_SHEET + ".tsv, "
                + PAGE + ".html");
    }

    private static void print(ReportPage.Table table) {
        System.out.println("== " + table.title());
        System.out.println(String.join(" | ", table.header()));
        table.rows().forEach(row -> System.out.println(String.join(" | ", row)));
        System.out.println();
    }

    /** How many of {@code table}'s verdicts are of each kind. */
    private static String tally(ReportPage.Table table) {
        Map<String, Long> kinds = table.rows().stream()
                .collect(Collectors.groupingBy(row -> kindOf(row.getLast()), LinkedHashMap::new, Collectors.counting()));
        return kinds.entrySet().stream().map(kind -> kind.getValue() + " " + kind.getKey()).collect(Collectors.joining(", "));
    }

    private static String kindOf(String verdict) {
        return Stream.of("BREACH", "DOMINANT", "WARN", "UNMEASURED", OK, NOT_JUDGED).filter(verdict::startsWith)
                .findFirst().orElse(verdict);
    }

    /** A cluster of a tree, or its fork: the members a tower takes one of. */
    record Cluster(String name, List<String> members, boolean fork) {
    }

    /** What a rule asks of its subject against each rival: none above it, none below it, or each about level. */
    enum Relation { CEILING, FLOOR, EQUAL }

    /**
     * A role a tower is meant to keep, checked at every stage both it and a rival are bought to.
     *
     * @param base   whether only the subject's stages before its fork count
     * @param rivals the towers it is set against; none for every other judged tower
     */
    private record Rule(String claim, Relation relation, String subject, List<String> scenarios, boolean base,
                        List<String> rivals) {

        static Rule of(String claim, Relation relation, String subject, String... scenarios) {
            return new Rule(claim, relation, subject, List.of(scenarios), false, List.of());
        }

        Rule beforeFork() {
            return new Rule(this.claim, this.relation, this.subject, this.scenarios, true, this.rivals);
        }

        Rule against(String... towers) {
            return new Rule(this.claim, this.relation, this.subject, this.scenarios, this.base, List.of(towers));
        }
    }

    /** The least a node of {@code tier} costs on a tower that costs {@code towerPrice}. */
    static int floorOf(int tier, int towerPrice) {
        return atLeastOne(FLOOR * (tier + 1) * towerPrice);
    }

    /** What the bench read of a node: nothing, a gain in one build and a loss in another, a loss in each, or a worth. */
    enum Reading { QUIET, MIXED, LOWERS, MEASURED }

    /**
     * A node's price as measured.
     *
     * @param fair      its worth times its tier's premium, and its tier's floor at the least; nothing when it has no
     *                  measured worth
     * @param suggested the price to give it
     */
    record Price(int fair, int suggested, String verdict) {
    }

    /**
     * What a credit spent on a node of {@code tier} is set against: the copy of its tower a player would be buying
     * by then, each copy dearer than the last.
     */
    static double premium(int tier) {
        return Math.pow(TowerFactory.Type.COPY_PRICE_GROWTH, tier + 1);
    }

    /**
     * The price of a node worth {@code worth} credits in {@code tier} of a tower that costs {@code towerPrice}. A
     * node with no measured worth keeps the price it has: there is nothing to move it by.
     */
    static Price priced(Reading reading, double worth, int tier, int towerPrice, int now) {
        int floor = floorOf(tier, towerPrice);
        if (reading == Reading.QUIET) {
            return new Price(0, now, "UNMEASURED utility: nothing the bench fights shows it; its price is kept");
        }
        if (reading == Reading.MIXED) {
            return new Price(0, floor, "UNMEASURED: it adds to one build and takes from another");
        }
        if (reading == Reading.LOWERS) {
            return new Price(0, floor, "BREACH lowers what its builds hold");
        }
        int fair = Math.max(floor, atLeastOne(worth * premium(tier)));
        return new Price(fair, fair, priceVerdict(now, fair));
    }

    /** Whether a price of {@code now} is near enough the {@code suggested} one. */
    static String priceVerdict(int now, int suggested) {
        return Math.abs(now - suggested) <= Math.max(SMALL_CHANGE, PRICE_BAND * suggested) ? OK
                : "WARN price " + now + " -> " + suggested;
    }

    /** How {@code subject} stands to {@code rivals} under {@code relation}, each a number where more is stronger. */
    static String judged(Relation relation, double subject, Map<String, Double> rivals) {
        List<String> breaking = new ArrayList<>();
        rivals.forEach((rival, value) -> {
            boolean breaks = switch (relation) {
                case CEILING -> value > subject * (1 + NOISE);
                case FLOOR -> value < subject * (1 - NOISE);
                case EQUAL -> value < subject * NEAR || value * NEAR > subject;
            };
            if (breaks) {
                breaking.add(rival + " " + times(value / subject));
            }
        });
        if (breaking.isEmpty()) {
            return OK;
        }
        String where = switch (relation) {
            case CEILING -> "above it";
            case FLOOR -> "below it";
            case EQUAL -> "apart";
        };
        return "BREACH " + where + ": " + String.join(", ", breaking);
    }

    /**
     * One way to take a cluster.
     *
     * @param value      the power a credit of the whole build buys
     * @param byScenario the same, scenario by scenario
     */
    record Pick(String name, double value, Map<String, Double> byScenario) {

        /** The scenarios it is clearly the best member at. */
        List<String> aheadAt(List<Pick> all) {
            return this.byScenario.keySet().stream().filter(scenario -> all.stream().filter(other -> other != this)
                    .allMatch(other -> this.byScenario.get(scenario) > other.byScenario().getOrDefault(scenario, 0.0) * (1 + NOISE)))
                    .toList();
        }

        private boolean neverBehind(List<Pick> all) {
            return this.byScenario.keySet().stream().allMatch(scenario -> all.stream()
                    .allMatch(other -> this.byScenario.get(scenario) >= other.byScenario().getOrDefault(scenario, 0.0) * (1 - NOISE)));
        }
    }

    /** A verdict for each of {@code picks}, the members of one cluster: within a band of the best, and none ahead everywhere. */
    static List<String> pickVerdicts(List<Pick> picks) {
        double best = picks.stream().mapToDouble(Pick::value).max().orElse(0);
        List<String> verdicts = new ArrayList<>();
        for (Pick pick : picks) {
            double second = picks.stream().filter(other -> other != pick).mapToDouble(Pick::value).max().orElse(0);
            if (pick.value() == best && second < best * PICK_BAND && pick.neverBehind(picks)) {
                verdicts.add("DOMINANT pick: ahead or level in every scenario");
            } else if (pick.value() < best * PICK_BAND) {
                verdicts.add("WARN weaker pick: " + times(pick.value() / best) + " the best");
            } else {
                verdicts.add(OK);
            }
        }
        return verdicts;
    }

    /** Every tower's price against its bare worth, and what its whole tree comes to. */
    ReportPage.Table towers() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            Held bare = this.sheet.bare(tower);
            double worth = this.sheet.worth(bare);
            Held top = this.best(tower, this.sheet.topStage(tower));
            Map<String, Integer> suggested = new LinkedHashMap<>();
            this.sheet.buildsOf(tower).stream().filter(build -> build.role().equals(TowerBench.LINE))
                    .forEach(line -> suggested.put(line.node(), Integer.parseInt(this.nodeRow(tower, line).get(SUGGESTED))));
            int tree = top.owned().stream().mapToInt(node -> suggested.getOrDefault(node, 0)).sum();
            rows.add(List.of(tower, bare.name(), String.valueOf(bare.cost()), ReportPage.number(this.sheet.power(bare)),
                    ReportPage.number(worth), String.valueOf(this.towerPrice(tower)), top.label(), String.valueOf(top.cost()),
                    ReportPage.number(this.sheet.worth(top)), ReportPage.number(this.value(top)),
                    String.valueOf(this.towerPrice(tower) + tree),
                    ReportPage.number(this.sheet.worth(top) / (this.towerPrice(tower) + tree)),
                    this.towerVerdict(tower, bare.cost(), worth)));
        }
        return ReportPage.Table.of("Tower prices", List.of("tower", "name", "price", "power", "fair", "suggested", "top build",
                "top cost", "top worth", "top value", "top suggested", "value then", "verdict"), rows).noted("""
                power: the Walkers of a mixed wave the bare tower adds to the partners. fair: that power in credits
                at the going rate. suggested: the price it has when that is near its fair one, else the fair one as
                a round number. top build: its best way to buy the whole tree, what that costs now, what it is
                worth, and value, worth over cost: under 1 the tree costs more than the same power bought as bare
                towers, which is the premium an upgrade carries. top suggested and value then: the same build at
                the suggested tower and node prices.""").heating("top value", "value then");
    }

    /** The price to give {@code tower}: the one it has when that is near its bare worth, else that worth, rounded. */
    private int towerPrice(String tower) {
        Held bare = this.sheet.bare(tower);
        double worth = this.sheet.worth(bare);
        return !TowerSheet.isJudged(tower) || Math.abs(bare.cost() - worth) <= TOWER_BAND * worth ? bare.cost() : readable(worth);
    }

    private String towerVerdict(String tower, int price, double worth) {
        if (!TowerSheet.isJudged(tower)) {
            return NOT_JUDGED;
        }
        return Math.abs(price - worth) <= TOWER_BAND * worth ? OK : "WARN price " + price + " -> " + readable(worth);
    }

    /** Every tower at every stage it is bought to, by its best build: what a credit buys there, against its rivals. */
    ReportPage.Table stages() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            for (int stage = -1; stage <= this.sheet.topStage(tower); stage++) {
                Held best = this.best(tower, stage);
                double against = this.value(best) / this.medianValue(stage);
                List<String> bestAt = this.bestAt(tower, stage);
                rows.add(List.of(tower, best.name(), String.valueOf(stage), stageName(stage), best.label(),
                        String.valueOf(best.cost()), ReportPage.number(this.sheet.power(best)),
                        ReportPage.number(this.sheet.worth(best)), ReportPage.number(this.value(best)),
                        ReportPage.number(against), bestAt.isEmpty() ? NONE : String.join(", ", bestAt),
                        this.stageVerdict(tower, against, bestAt)));
            }
        }
        return ReportPage.Table.of("Stages", List.of("tower", "name", "stage", "bought to", "build", "cost", "power", "worth",
                "value", "vs rivals", "best buy against", "verdict"), rows).noted("""
                One row for each tier a tower is bought whole to, by the build of that tier that pays best. value:
                worth over cost. vs rivals: its value over the median of the judged towers bought to the same tier.
                best buy against: the scenarios where no judged tower of that tier holds more for a credit alone.""")
                .heating("value", "vs rivals");
    }

    private String stageVerdict(String tower, double against, List<String> bestAt) {
        if (!TowerSheet.isJudged(tower)) {
            return NOT_JUDGED;
        }
        double share = bestAt.stream().mapToDouble(TowerSheet::weightOf).sum()
                / this.sheet.scenarios().stream().mapToDouble(TowerSheet::weightOf).sum();
        if (share >= GO_TO) {
            return String.format(Locale.ROOT, "DOMINANT the best buy against %.0f%% of a mixed wave", share * 100);
        }
        if (against > STRONG) {
            return "DOMINANT pays " + times(against) + " its rivals";
        }
        return against < WEAK ? "WARN weak: pays " + times(against) + " its rivals" : OK;
    }

    /** The scenarios in which no judged tower bought to {@code stage} holds more alone for a credit than {@code tower}. */
    private List<String> bestAt(String tower, int stage) {
        return this.sheet.scenarios().stream().filter(scenario -> {
            double most = this.rivalsAt(stage).stream().mapToDouble(rival -> this.perCredit(rival, stage, scenario)).max().orElse(0);
            return most > 0 && this.perCredit(tower, stage, scenario) >= most * (1 - NOISE);
        }).toList();
    }

    /** Every rule a tower's role sets, at every stage, in every scenario it names. */
    ReportPage.Table roles() {
        List<List<String>> rows = new ArrayList<>();
        for (Rule rule : RULES) {
            if (!this.sheet.towers().contains(rule.subject())) {
                continue;
            }
            int last = rule.base() ? this.forkStage(rule.subject()) - 1 : this.sheet.topStage(rule.subject());
            for (int stage = -1; stage <= last; stage++) {
                for (String scenario : rule.scenarios()) {
                    rows.add(this.roleRow(rule, stage, scenario));
                }
            }
        }
        return ReportPage.Table.of("Roles", List.of("rule", "tower", "bought to", "scenario", "per 100 credits", "alone",
                "strongest rival", "its per 100 credits", "its alone", "verdict"), rows).noted("""
                Each rule compares what a credit holds alone, by each tower's best build of the same tier: towers
                cost up to three times each other, so a tower is the top of its role when a credit spent on it holds
                the most there. per 100 credits and alone: the subject's Walkers held, for 100 credits and in all. The
                rival shown is the one that most breaks the rule, or comes closest to.""");
    }

    private List<String> roleRow(Rule rule, int stage, String scenario) {
        double subject = this.perCredit(rule.subject(), stage, scenario);
        Map<String, Double> rivals = new LinkedHashMap<>();
        for (String rival : this.rivalsAt(stage)) {
            boolean named = rule.rivals().isEmpty() || rule.rivals().contains(rival);
            if (named && !rival.equals(rule.subject())) {
                rivals.put(this.sheet.bare(rival).name(), this.perCredit(rival, stage, scenario));
            }
        }
        String closest = rivals.keySet().stream().reduce((a, b) -> {
            boolean first = switch (rule.relation()) {
                case CEILING -> rivals.get(a) >= rivals.get(b);
                case FLOOR -> rivals.get(a) <= rivals.get(b);
                case EQUAL -> Math.abs(Math.log(rivals.get(a) / subject)) >= Math.abs(Math.log(rivals.get(b) / subject));
            };
            return first ? a : b;
        }).orElse(NONE);
        String rivalTower = this.sheet.towers().stream().filter(tower -> this.sheet.bare(tower).name().equals(closest))
                .findFirst().orElse("");
        return List.of(rule.claim(), this.sheet.bare(rule.subject()).name(), stageName(stage), scenario,
                ReportPage.number(subject * 100), ReportPage.number(this.most(rule.subject(), stage, scenario)), closest,
                ReportPage.number(rivals.getOrDefault(closest, Double.NaN) * 100),
                rivalTower.isEmpty() ? NONE : ReportPage.number(this.most(rivalTower, stage, scenario)),
                judged(rule.relation(), subject, rivals));
    }

    /** What a credit holds alone of each scenario, for every tower at every stage, against the judged towers' median. */
    ReportPage.Table profiles() {
        List<List<String>> rows = new ArrayList<>();
        for (int stage = -1; stage <= this.sheet.topStage(); stage++) {
            for (String tower : this.sheet.towers()) {
                if (this.sheet.stage(tower, stage).isEmpty()) {
                    continue;
                }
                List<String> row = new ArrayList<>(List.of(stageName(stage), this.sheet.bare(tower).name()));
                for (String scenario : this.sheet.scenarios()) {
                    int at = stage;
                    double median = median(this.rivalsAt(stage).stream().mapToDouble(rival -> this.perCredit(rival, at, scenario)));
                    row.add(median > 0 ? ReportPage.number(this.perCredit(tower, stage, scenario) / median) : NONE);
                }
                rows.add(row);
            }
        }
        List<String> header = new ArrayList<>(List.of("bought to", "tower"));
        header.addAll(this.sheet.scenarios());
        return ReportPage.Table.of("Profiles", header, rows).noted("""
                What a credit holds alone in each scenario, over the median of the judged towers bought to the same
                tier: 1 is the middle tower, 2 holds twice as much for the money, 0 lets something through at any
                health. A tower's row should be warm where its role is and cool elsewhere; a row warm everywhere is a
                tower with no weakness.""").heating(this.sheet.scenarios().toArray(String[]::new));
    }

    /** Every node: what it is worth alone, in its tier and in the whole tree, and the price that follows. */
    ReportPage.Table nodes() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            this.sheet.buildsOf(tower).stream().filter(build -> build.role().equals(TowerBench.LINE))
                    .forEach(line -> rows.add(this.nodeRow(tower, line)));
        }
        return ReportPage.Table.of("Nodes", List.of("tower", "node", "name", "tier", "price", "alone", "in tier", "at top",
                "worth", "fair", "suggested", "share", "moves", "verdict"), rows).noted("""
                alone, in tier, at top: the credits of power the node adds on only the nodes it needs, to its tier
                bought whole, and to the whole tree; a node others need is not credited with them. Nodes multiply
                each other, so those grow with the build and add up to more than the tree. worth: the mean of the
                node's share of what its tier adds to the tiers below it, which is what it gives when bought, and its
                share of what the whole tree adds to the bare tower, which is what it gives in the end; a tower's
                worths add up to its tree. fair: worth times its tier's premium, the price of the copy of the tower a
                player would be buying instead, and never under the tier's floor. suggested: the fair price; the
                members of a cluster and the sides of a fork are switched for free, so they share the dearest
                member's. share: the part of the whole tree's power lost without it. moves: the scenarios it changes
                most at the top, alone.""");
    }

    private List<String> nodeRow(String tower, Held line) {
        NodeWorth worth = this.worthOf(tower, line);
        int price = line.cost() - this.versus(line).cost();
        Price own = this.ownPrice(tower, line);
        int suggested = this.sharedPrice(tower, line);
        String verdict = own.fair() > 0 ? priceVerdict(price, suggested) : own.verdict();
        if (!TowerSheet.isJudged(tower)) {
            verdict = NOT_JUDGED;
        } else if (worth.share() <= -SINKS && !verdict.startsWith("BREACH")) {
            verdict = String.format(Locale.ROOT, "BREACH the whole tree holds %.0f%% more without it; %s", -worth.share() * 100,
                    verdict);
        } else if (worth.share() >= KEYSTONE && !verdict.startsWith("BREACH")) {
            verdict = String.format(Locale.ROOT, "DOMINANT node: %.0f%% of the whole tree's power; %s", worth.share() * 100, verdict);
        }
        return List.of(tower, line.node(), line.nodeName(), line.tier(), String.valueOf(price), ReportPage.number(worth.alone()),
                number(worth.inTier()), number(worth.atTop()), ReportPage.number(worth.worth()),
                own.fair() == 0 ? NONE : String.valueOf(own.fair()), String.valueOf(suggested),
                ReportPage.number(worth.share()), this.moves(tower, line.node()), verdict);
    }

    /** The price the node {@code line} weighs comes to by itself. */
    private Price ownPrice(String tower, Held line) {
        NodeWorth worth = this.worthOf(tower, line);
        return priced(worth.reading(), worth.worth(), Integer.parseInt(line.tier()), this.sheet.bare(tower).cost(),
                line.cost() - this.versus(line).cost());
    }

    /**
     * The price to give the node {@code line} weighs: its own, or, in a cluster or a fork, the dearest measured
     * member's, since a tower that owns one member switches to another for nothing.
     */
    private int sharedPrice(String tower, Held line) {
        List<Price> members = this.clusters.getOrDefault(tower, List.of()).stream()
                .filter(cluster -> cluster.members().contains(line.node()))
                .flatMap(cluster -> cluster.members().stream())
                .flatMap(member -> this.weighing(tower, TowerBench.LINE, member).stream())
                .map(member -> this.ownPrice(tower, member)).toList();
        int measured = members.stream().filter(price -> price.fair() > 0).mapToInt(Price::suggested).max().orElse(0);
        if (measured > 0) {
            return measured;
        }
        return members.stream().mapToInt(Price::suggested).max().orElse(this.ownPrice(tower, line).suggested());
    }

    /**
     * What a node adds, in credits.
     *
     * @param alone  on only the nodes it needs
     * @param inTier to its tier bought whole
     * @param atTop  to the whole tree
     * @param worth  its share of the tree: of what its tier adds when bought, and of what the whole tree adds
     * @param share  the part of the whole tree's power lost without it
     */
    private record NodeWorth(double alone, OptionalDouble inTier, OptionalDouble atTop, double worth, double share,
                             Reading reading) {

        static NodeWorth of(double alone, OptionalDouble inTier, OptionalDouble atTop) {
            return new NodeWorth(alone, inTier, atTop, alone, 0, Reading.MEASURED);
        }

        NodeWorth read(double shareOfTree, double lost, Reading as) {
            return new NodeWorth(this.alone, this.inTier, this.atTop, shareOfTree, lost, as);
        }
    }

    private NodeWorth worthOf(String tower, Held line) {
        List<Held> inTier = this.weighing(tower, TowerBench.WITHOUT, line.node());
        List<Held> atTop = this.weighing(tower, TowerBench.WITHOUT_AT_TOP, line.node());
        double rate = this.sheet.rate();
        double added = this.sheet.power(line) - this.sheet.power(this.versus(line));
        NodeWorth worth = NodeWorth.of(added / rate, inTier.stream().mapToDouble(less -> this.ownPower(less) / rate).average(),
                atTop.stream().mapToDouble(less -> this.ownPower(less) / rate).average());
        OptionalDouble ofTier = inTier.stream().mapToDouble(less -> this.tierScale(less) * this.ownPower(less) / rate).average();
        OptionalDouble ofTree = atTop.stream().mapToDouble(less -> this.treeScale(less) * this.ownPower(less) / rate).average();
        double ofBoth = DoubleStream.concat(ofTier.stream(), ofTree.stream()).average().orElse(added / rate);
        double share = atTop.stream().mapToDouble(less -> this.ownPower(less) / this.sheet.power(this.versus(less))).average()
                .orElse(0);
        List<Double> moves = new ArrayList<>(List.of(added / Math.max(this.sheet.power(line), this.sheet.power(this.versus(line)))));
        List<Double> clean = new ArrayList<>();
        for (Held less : Stream.concat(inTier.stream(), atTop.stream()).toList()) {
            double move = this.ownPower(less) / this.sheet.power(this.versus(less));
            moves.add(move);
            if (this.wentWith(less).isEmpty()) {
                clean.add(move);
            }
        }
        Reading reading = Reading.MEASURED;
        if (moves.stream().allMatch(move -> Math.abs(move) < QUIET)) {
            reading = Reading.QUIET;
        } else if (!clean.isEmpty() && clean.stream().allMatch(move -> move < -QUIET)) {
            reading = Reading.LOWERS;
        } else if (ofBoth <= 0) {
            reading = Reading.MIXED;
        }
        return worth.read(ofBoth, share, reading);
    }

    /** The nodes a build lost along with the one {@code less} is there to weigh: the ones that need it. */
    private List<String> wentWith(Held less) {
        return this.versus(less).owned().stream()
                .filter(node -> !less.owned().contains(node) && !node.equals(less.node())).toList();
    }

    /**
     * The power the node {@code less} lacks adds by itself: what the build loses without it, less what the nodes
     * that went with it add. A node others need is not credited with them; one that only opens the way adds nothing.
     */
    private double ownPower(Held less) {
        Held whole = this.versus(less);
        double lost = this.sheet.power(whole) - this.sheet.power(less);
        List<String> with = this.wentWith(less);
        for (String node : with) {
            List<Held> views = this.weighing(less.tower(), less.role(), node);
            lost -= views.stream().filter(view -> view.versus().equals(less.versus())).findFirst()
                    .or(() -> views.stream().findFirst()).map(view -> Math.max(0, this.ownPower(view))).orElse(0.0);
        }
        return with.isEmpty() ? lost : Math.max(0, lost);
    }

    /**
     * What turns the power a node adds to its tier into its share of the tier's step. Nodes multiply each other, so
     * what a tier loses without each of its nodes adds up to more than the tier adds to the tiers below it; the
     * scale brings the sum back to that step, by the tier's first build on the same side of the fork.
     */
    private double tierScale(Held less) {
        Held whole = this.versus(less);
        Held first = this.sheet.stage(less.tower(), whole.stage()).stream()
                .filter(build -> build.side().equals(whole.side()) && build.member().isEmpty()).findFirst().orElse(whole);
        List<Held> below = this.sheet.stage(less.tower(), whole.stage() - 1);
        Held before = below.stream().filter(build -> build.side().equals(whole.side()) && build.member().isEmpty()).findFirst()
                .or(() -> below.stream().filter(build -> build.side().isEmpty() && build.member().isEmpty()).findFirst())
                .orElse(this.sheet.bare(less.tower()));
        double step = this.sheet.power(first) - this.sheet.power(before);
        double sum = this.sheet.buildsOf(less.tower()).stream()
                .filter(view -> view.role().equals(TowerBench.WITHOUT) && view.versus().equals(first.label()))
                .mapToDouble(view -> Math.max(0, this.ownPower(view))).sum();
        return step > 0 && sum > 0 ? step / sum : 1;
    }

    /**
     * What turns the power a node adds to the whole tree into its share of what the tree adds to the bare tower, by
     * the first whole tree on the same side of the fork.
     */
    private double treeScale(Held less) {
        Held whole = this.versus(less);
        Held first = this.sheet.stage(less.tower(), whole.stage()).stream()
                .filter(build -> build.side().equals(whole.side()) && build.member().isEmpty()).findFirst().orElse(whole);
        double step = this.sheet.power(first) - this.sheet.power(this.sheet.bare(less.tower()));
        double sum = this.sheet.buildsOf(less.tower()).stream()
                .filter(view -> view.role().equals(TowerBench.WITHOUT_AT_TOP) && view.versus().equals(first.label()))
                .mapToDouble(view -> Math.max(0, this.ownPower(view))).sum();
        return step > 0 && sum > 0 ? step / sum : 1;
    }

    /** What each tier of each tower's best whole tree costs, now and as suggested, against the pace and its rivals. */
    ReportPage.Table tiers() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            for (int tier = 0; tier <= this.sheet.topStage(tower); tier++) {
                double mine = this.tierCost(tower, tier, SUGGESTED) / this.towerPrice(tower);
                int at = tier;
                double rivals = median(this.rivalsAt(tier).stream()
                        .mapToDouble(rival -> this.tierCost(rival, at, SUGGESTED) / this.towerPrice(rival)));
                rows.add(List.of(tower, this.sheet.bare(tower).name(), UpgradeTier.nameOf(tier),
                        String.valueOf(this.tierNodes(tower, tier).size()), ReportPage.number(this.tierCost(tower, tier, PRICE)),
                        ReportPage.number(this.tierCost(tower, tier, SUGGESTED)), ReportPage.number(mine),
                        ReportPage.number(mine / rivals), this.tierVerdict(tower, tier, mine, mine / rivals)));
            }
        }
        return ReportPage.Table.of("Tiers", List.of("tower", "name", "tier", "nodes", "cost", "suggested", "tower prices",
                "vs rivals", "verdict"), rows).noted("""
                Each tier of the tower's best whole tree: how many nodes it buys there, what they cost now and at
                the suggested prices, that in suggested tower prices, and that over the median of the judged towers'
                same tier. The pace: the first tier comes to less than the tower, the second to about the tower, and
                each later tier to more than the one before. Prices follow what nodes do, so a tier off the pace or
                far from its rivals is one whose nodes do too little or too much for where they sit.""")
                .heating("vs rivals");
    }

    /** The rows of the nodes of {@code tier} that {@code tower}'s best whole tree owns. */
    private List<List<String>> tierNodes(String tower, int tier) {
        Held top = this.best(tower, this.sheet.topStage(tower));
        return this.sheet.buildsOf(tower).stream()
                .filter(line -> line.role().equals(TowerBench.LINE) && Integer.parseInt(line.tier()) == tier
                        && top.owned().contains(line.node()))
                .map(line -> this.nodeRow(tower, line)).toList();
    }

    private double tierCost(String tower, int tier, int column) {
        return this.tierNodes(tower, tier).stream().mapToInt(node -> Integer.parseInt(node.get(column))).sum();
    }

    private String tierVerdict(String tower, int tier, double towerPrices, double againstRivals) {
        if (!TowerSheet.isJudged(tower)) {
            return NOT_JUDGED;
        }
        if (tier == 0 && towerPrices > FIRST_TIER_MOST) {
            return "WARN dearer than its tower: " + times(towerPrices);
        }
        if (tier == 1 && (towerPrices < SECOND_TIER_LEAST || towerPrices > SECOND_TIER_MOST)) {
            return "WARN not about its tower's price: " + times(towerPrices);
        }
        if (tier > 1 && towerPrices <= this.tierCost(tower, tier - 1, SUGGESTED) / this.towerPrice(tower)) {
            return "WARN no dearer than the tier before";
        }
        if (againstRivals > TIER_APART || againstRivals * TIER_APART < 1) {
            return "WARN " + times(againstRivals) + " the same tier of its rivals";
        }
        return OK;
    }

    /** The scenarios the node changes most in the whole tree, alone. */
    private String moves(String tower, String node) {
        List<Held> views = this.weighing(tower, TowerBench.WITHOUT_AT_TOP, node);
        if (views.isEmpty()) {
            return NONE;
        }
        Held less = views.getFirst();
        Held whole = this.versus(less);
        Map<String, Double> ratios = new LinkedHashMap<>();
        for (String scenario : this.sheet.scenarios()) {
            double with = whole.alone(scenario);
            double without = less.alone(scenario);
            if (with > 0 && without > 0 && (with / without >= MOVE || without / with >= MOVE)) {
                ratios.put(scenario, with / without);
            } else if (with > 0 && without == 0) {
                ratios.put(scenario, Double.POSITIVE_INFINITY);
            }
        }
        String text = ratios.entrySet().stream()
                .sorted((a, b) -> Double.compare(Math.abs(Math.log(b.getValue())), Math.abs(Math.log(a.getValue()))))
                .limit(MOVES).map(move -> move.getKey() + " " + times(move.getValue())).collect(Collectors.joining(", "));
        return text.isEmpty() ? NONE : text;
    }

    /** Every cluster and fork, member by member, as the whole tree holds with each. */
    ReportPage.Table clusterPicks() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            for (Cluster cluster : this.clusters.getOrDefault(tower, List.of())) {
                Map<String, Held> taken = new LinkedHashMap<>();
                cluster.members().forEach(member -> this.taking(tower, cluster, member).ifPresent(build -> taken.put(member, build)));
                if (taken.size() < 2) {
                    continue;
                }
                List<Pick> picks = taken.entrySet().stream().map(entry -> this.pick(tower, entry.getKey(), entry.getValue())).toList();
                List<String> verdicts = pickVerdicts(picks);
                double best = picks.stream().mapToDouble(Pick::value).max().orElse(0);
                int next = 0;
                for (Map.Entry<String, Held> entry : taken.entrySet()) {
                    Pick pick = picks.get(next);
                    List<String> ahead = pick.aheadAt(picks);
                    rows.add(List.of(tower, cluster.fork() ? "fork: " + cluster.name() : cluster.name(), pick.name(),
                            entry.getValue().label(), String.valueOf(entry.getValue().cost()),
                            ReportPage.number(this.sheet.power(entry.getValue())), ReportPage.number(pick.value() / best),
                            ahead.isEmpty() ? NONE : String.join(", ", ahead),
                            TowerSheet.isJudged(tower) ? verdicts.get(next) : NOT_JUDGED));
                    next++;
                }
            }
        }
        return ReportPage.Table.of("Clusters and forks", List.of("tower", "cluster", "member", "build", "cost", "power",
                "vs best", "ahead at", "verdict"), rows).noted("""
                Each member of a cluster, and each side of a fork, in the whole tree bought with it. vs best: the
                power a credit of that build buys, over the best member's. ahead at: the scenarios where it adds
                clearly more than every other member. Members should sit within a band of each other and each be
                ahead somewhere.""").heating("vs best");
    }

    /** The whole tree bought with {@code member} of {@code cluster}, by the build of it that pays best. */
    private Optional<Held> taking(String tower, Cluster cluster, String member) {
        boolean first = cluster.members().getFirst().equals(member);
        return this.sheet.stage(tower, this.sheet.topStage(tower)).stream()
                .filter(build -> cluster.fork()
                        ? build.side().equals(member) && build.member().isEmpty()
                        : build.owned().contains(member) && build.member().equals(first ? "" : member))
                .max((a, b) -> Double.compare(this.value(a), this.value(b)));
    }

    private Pick pick(String tower, String member, Held build) {
        Map<String, Double> byScenario = new LinkedHashMap<>();
        this.sheet.scenarios().forEach(scenario -> byScenario.put(scenario, this.sheet.contribution(build, scenario) / build.cost()));
        String name = this.sheet.buildsOf(tower).stream().filter(other -> other.node().equals(member) && !other.nodeName().isEmpty())
                .map(Held::nodeName).findFirst().orElse(member);
        return new Pick(name, this.sheet.power(build) / build.cost(), byScenario);
    }

    /** Every two towers side by side: what they hold together over what they hold apart. */
    ReportPage.Table synergies() {
        List<String> stored = this.pairs.getFirst();
        List<String> columns = stored.stream().filter(column -> column.endsWith(" synergy")).toList();
        List<List<String>> rows = new ArrayList<>();
        for (List<String> pair : this.pairs.subList(1, this.pairs.size())) {
            List<String> row = new ArrayList<>(pair.subList(0, 5));
            List<Double> read = new ArrayList<>();
            for (String column : columns) {
                String cell = pair.get(stored.indexOf(column));
                row.add(cell);
                if (!NONE.equals(cell) && Double.parseDouble(cell) > 0) {
                    read.add(Double.parseDouble(cell));
                }
            }
            double mean = Math.exp(read.stream().mapToDouble(Math::log).average().orElse(0));
            row.add(ReportPage.number(mean));
            boolean judged = TowerSheet.isJudged(pair.get(0)) && TowerSheet.isJudged(pair.get(2));
            String verdict = OK;
            if (mean >= SYNERGY_HIGH) {
                verdict = "WARN strong pair: " + times(mean) + " what they hold apart";
            } else if (mean <= SYNERGY_LOW) {
                verdict = "WARN clash: " + times(mean) + " what they hold apart";
            }
            row.add(judged ? verdict : NOT_JUDGED);
            rows.add(row);
        }
        List<String> header = new ArrayList<>(stored.subList(0, 5));
        header.addAll(columns);
        header.addAll(List.of("mean", "verdict"));
        List<String> heat = new ArrayList<>(columns);
        heat.add("mean");
        return ReportPage.Table.of("Synergies", header, rows).noted("""
                Two towers on neighbouring cells: what they hold together over what each holds alone, added up, by
                scenario, and the geometric mean. Above 1 they help each other; below 1 they waste hits on the same
                enemy or undo each other's work.""").heating(heat.toArray(String[]::new));
    }

    /** The rows of {@code tables} whose verdict breaks a rule, gathered. */
    ReportPage.Table findings(List<ReportPage.Table> tables) {
        List<List<String>> rows = new ArrayList<>();
        for (ReportPage.Table table : tables) {
            for (List<String> row : table.rows()) {
                String verdict = row.getLast();
                if (verdict.startsWith("BREACH") || verdict.startsWith("DOMINANT")) {
                    rows.add(List.of(table.title(), String.join(" / ", row.subList(0, Math.min(4, row.size() - 1))), verdict));
                }
            }
        }
        return ReportPage.Table.of("Breaches and dominant picks", List.of("table", "what", "verdict"), rows).noted("""
                Every row of the tables below that breaks a role, lowers a build or outclasses its rivals. Warnings
                (prices off, nodes weak or strong for their tier, unmeasured nodes) stay in their own tables.""");
    }

    /** The prices as the next stage and the retune read them: one row for each tower and each node. */
    ReportPage.Table prices() {
        List<List<String>> rows = new ArrayList<>();
        for (String tower : this.sheet.towers()) {
            Held bare = this.sheet.bare(tower);
            double worth = this.sheet.worth(bare);
            rows.add(List.of(tower, "", bare.name(), "", String.valueOf(bare.cost()), ReportPage.number(worth),
                    String.valueOf(atLeastOne(worth)), String.valueOf(this.towerPrice(tower)),
                    this.towerVerdict(tower, bare.cost(), worth)));
            for (Held line : this.sheet.buildsOf(tower)) {
                if (line.role().equals(TowerBench.LINE)) {
                    List<String> node = this.nodeRow(tower, line);
                    rows.add(List.of(tower, node.get(1), node.get(2), node.get(3), node.get(PRICE), node.get(8), node.get(9),
                            node.get(SUGGESTED), node.getLast()));
                }
            }
        }
        return ReportPage.Table.of("Prices", List.of("tower", "node", "name", "tier", "price", "worth", "fair", "suggested",
                "verdict"), rows);
    }

    private List<Held> weighing(String tower, String role, String node) {
        return this.sheet.buildsOf(tower).stream().filter(build -> build.role().equals(role) && build.node().equals(node)).toList();
    }

    private Held versus(Held build) {
        return this.sheet.build(build.tower(), build.versus()).orElseThrow();
    }

    /** Worth over cost: what a credit spent on {@code build} buys, in credits of bare tower. */
    private double value(Held build) {
        return this.sheet.worth(build) / build.cost();
    }

    /** {@code tower} bought whole to {@code stage}, by the build that pays best. */
    private Held best(String tower, int stage) {
        return this.sheet.stage(tower, stage).stream().max((a, b) -> Double.compare(this.value(a), this.value(b))).orElseThrow();
    }

    /** The middle of what a credit buys of the judged towers bought to {@code stage}. */
    private double medianValue(int stage) {
        return median(this.rivalsAt(stage).stream().mapToDouble(rival -> this.value(this.best(rival, stage))));
    }

    /** The judged towers that are bought to {@code stage}. */
    private List<String> rivalsAt(int stage) {
        return this.sheet.judged().stream().filter(tower -> !this.sheet.stage(tower, stage).isEmpty()).toList();
    }

    /** The Walkers of {@code scenario} a credit holds alone, by {@code tower}'s best build of {@code stage} for it. */
    private double perCredit(String tower, int stage, String scenario) {
        return this.sheet.stage(tower, stage).stream().mapToDouble(build -> build.alone(scenario) / build.cost()).max()
                .orElse(Double.NaN);
    }

    private double most(String tower, int stage, String scenario) {
        return this.sheet.stage(tower, stage).stream().mapToDouble(build -> build.alone(scenario)).max().orElse(Double.NaN);
    }

    /** The first stage at which {@code tower}'s builds take a side of its fork; past its top when it has none. */
    private int forkStage(String tower) {
        return this.sheet.buildsOf(tower).stream().filter(build -> build.isFull() && !build.side().isEmpty())
                .mapToInt(Held::stage).min().orElse(this.sheet.topStage(tower) + 1);
    }

    private static double median(DoubleStream values) {
        double[] sorted = values.sorted().toArray();
        if (sorted.length == 0) {
            return Double.NaN;
        }
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2] : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2;
    }

    private static String stageName(int stage) {
        return stage < 0 ? TowerBench.BARE : UpgradeTier.nameOf(stage);
    }

    private static String number(OptionalDouble value) {
        return value.isPresent() ? ReportPage.number(value.getAsDouble()) : NONE;
    }

    private static String times(double ratio) {
        return Double.isFinite(ratio) ? String.format(Locale.ROOT, "x%.2f", ratio) : "from nothing";
    }

    private static int atLeastOne(double credits) {
        return Math.max(1, (int) Math.round(credits));
    }

    private static int readable(double credits) {
        return Math.max(READABLE, (int) Math.round(credits / READABLE) * READABLE);
    }
}
