package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.damage.DamageMix;
import td.enemy.Rank;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeTier;
import td.util.TickRate;
import td.wave.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;

/**
 * Weighs every round of every level and sets it against what the level has paid by then: how strong the round is,
 * lane by lane, in Walkers of the lowest rank; which parts of a defence it asks for; what it pays; what a tower
 * that has stood on a lane from the start has earned by then; and what the defence that just holds it costs when it
 * is built of the towers as the tower bench measured them. A round is measured as an enemy is, under a
 * {@link ProbeRig}, on the level's own board, so its lanes' length and what its enemies do to each other count.
 * Run it after the tower bench:
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=td.balance.WavePowerReport [-Dexec.args="--levels rift,hive"]
 * </pre>
 *
 * It prints one table per level, stores every row in {@code target/balance/wave-power.tsv} and publishes
 * {@code target/balance/waves.html}. The cost is a yardstick, not a forecast: it buys the average tower, for the
 * round alone.
 */
public final class WavePowerReport {

    static final String SHEET = "wave-power";
    private static final String PAGE = "waves";
    /** How many times their first size a defence's hits have grown by a level's last round. */
    private static final float LAST_HIT_SCALE = 1.5f;
    /** A round asks for a part of a defence it leans on this many times as much as Walkers do. */
    private static final double ASKS_FROM = 1.4;
    private static final String REVEAL = "reveal";
    private static final String HOLD = "hold";
    /** How far above what just holds a round the rig stands when the round's pay is counted. */
    private static final double PAY_HEADROOM = 1.5;
    private static final long PAY_SEED = 1L;
    private static final double DONE = 1e-6;
    /** A round this far under its level's line is a gate; one further off it than the band is flagged. */
    private static final double GATE = 0.92;
    private static final double BAND_LOW = 0.7;
    private static final double BAND_HIGH = 1.3;
    private static final List<String> HEADER = List.of("level", "round", "walkers", "bounty", "bonus", "pays", "bank",
            "need", "room", "even", "xp", "opens", "asks for", "verdict", "lanes");

    private WavePowerReport() {
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        Set<String> only = new LinkedHashSet<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--levels")) {
                only.addAll(Arrays.asList(args[i + 1].toLowerCase(Locale.ROOT).split(",")));
            }
        }
        Bench bench = Bench.measured();
        TowerSheet sheet = TowerSheet.stored();
        Yardstick yardstick = Yardstick.of(sheet);
        List<List<String>> stored = new ArrayList<>();
        List<ReportPage.Table> tables = new ArrayList<>();
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            String name = level.name().toLowerCase(Locale.ROOT);
            if (!only.isEmpty() && only.stream().noneMatch(name::contains)) {
                continue;
            }
            Ground ground = Ground.of(level, yardstick);
            int rounds = level.paths().getFirst().waves().size();
            List<RoundPower> powers = IntStream.range(0, rounds).parallel()
                    .mapToObj(round -> RoundPower.of(level, round, bench, ground)).toList();
            List<List<String>> rows = rows(level, powers);
            stored.addAll(rows);
            tables.add(ReportPage.Table.of(level.name(), HEADER.subList(1, HEADER.size()),
                    rows.stream().map(row -> row.subList(1, row.size())).toList())
                    .noted(String.format(Locale.ROOT, "Starts with %d credits: %.1f towers at the average price of %.0f. "
                                    + "%d lives, %d lanes.", level.startingCredits(),
                            level.startingCredits() / yardstick.steps().getFirst().cost(),
                            yardstick.steps().getFirst().cost(), level.startingLives(), level.paths().size()))
                    .heating("room", "even"));
            System.out.printf(Locale.ROOT, "%n%s: starts with %d credits, %d lives, %d lanes%n", level.name(),
                    level.startingCredits(), level.startingLives(), level.paths().size());
            System.out.println(String.join("\t", HEADER.subList(1, HEADER.size())));
            rows.forEach(row -> System.out.println(String.join("\t", row.subList(1, row.size()))));
        }
        if (only.isEmpty()) {
            ReportPage.store(SHEET, ReportPage.Table.of("Waves", HEADER, stored));
            ReportPage.publish(PAGE, "Waves", String.format(Locale.ROOT, """
                    Every round of every level, measured with no tower on the board, against what the level has paid \
                    by then. <b>walkers</b>: the round's strength in Walkers of the lowest rank. <b>pays</b>: bounty \
                    and clear bonus per Walker of strength. <b>bank</b>: the starting credits and everything earlier \
                    rounds paid. <b>need</b>: what the cheapest defence that holds the round costs, built of the \
                    average measured tower (bare %.0f credits for %.1f Walkers on the bench; each later copy dearer; \
                    tiers after), best cell first. <b>room</b>: bank over need. <b>even</b>: room over the line the \
                    level's rooms follow; under %.1f is a gate. <b>xp</b>: what a tower that has stood on each lane \
                    since the first round has earned before this one; <b>opens</b>: the tier the richest lane's has \
                    opened at the standard gates. <b>asks for</b>: the part of a defence the round leans on more \
                    than Walkers do.""", yardstick.steps().getFirst().cost(), yardstick.steps().getFirst().walkers(), GATE),
                    tables);
            System.out.println("\nStored in " + ReportPage.FOLDER + ": " + SHEET + ".tsv, " + PAGE + ".html");
        } else {
            System.out.println("\nNothing stored: a run of some levels only prints.");
        }
    }

    /** The stored rows of {@code level}, one a round, in {@link #HEADER}'s order. */
    private static List<List<String>> rows(LevelDefinition level, List<RoundPower> powers) {
        double[] banks = new double[powers.size()];
        double bank = level.startingCredits();
        for (RoundPower power : powers) {
            banks[power.round() - 1] = bank;
            bank += power.bounty() + power.bonus();
        }
        Trend trend = Trend.through(powers, banks);
        int[] earned = new int[level.paths().size()];
        List<List<String>> rows = new ArrayList<>();
        for (RoundPower power : powers) {
            double had = banks[power.round() - 1];
            double even = had / power.need() / trend.at(power.round());
            StringBuilder xp = new StringBuilder();
            StringBuilder scripts = new StringBuilder();
            for (int lane = 0; lane < earned.length; lane++) {
                xp.append(lane == 0 ? "" : " | ").append(earned[lane]);
                LanePower sent = power.lanes().get(lane);
                scripts.append(lane == 0 ? "" : " | ")
                        .append(String.format(Locale.ROOT, "[%.0f] %s", sent.walkers(), sent.script()));
            }
            rows.add(List.of(level.name(), String.valueOf(power.round()), ReportPage.number(power.walkers()),
                    String.valueOf(power.bounty()), String.valueOf(power.bonus()),
                    ReportPage.number((power.bounty() + power.bonus()) / power.walkers()),
                    String.format(Locale.ROOT, "%.0f", had), String.format(Locale.ROOT, "%.0f", power.need()),
                    ReportPage.number(had / power.need()), ReportPage.number(even), xp.toString(),
                    opened(Arrays.stream(earned).max().orElse(0)), power.asksFor(), verdict(even), scripts.toString()));
            for (int lane = 0; lane < earned.length; lane++) {
                earned[lane] += power.lanes().get(lane).bounty();
            }
        }
        return rows;
    }

    /** The highest tier a tower with {@code xp} has opened at the standard gates. */
    static String opened(int xp) {
        List<UpgradeTier> tiers = UpgradeTier.standard();
        int open = 0;
        for (int tier = 0; tier < tiers.size(); tier++) {
            if (tiers.get(tier).xp() <= xp) {
                open = tier;
            }
        }
        return UpgradeTier.nameOf(open);
    }

    static String verdict(double even) {
        if (Double.isNaN(even) || even < BAND_LOW) {
            return "WARN far under the line";
        }
        if (even > BAND_HIGH) {
            return "WARN far over the line";
        }
        return even <= GATE ? "gate" : TowerReport.OK;
    }

    /**
     * The test lane's group of Walkers, which every round is counted in.
     *
     * @param mix          the balanced mix at scale one, with nothing holding the enemies back
     * @param mixScale     the scale of {@code mix} that holds the group
     * @param pure         what holds the group of each probe alone, in damage a second
     * @param workAWalker  the damage a second, times the seconds it has, that one Walker of the group takes
     * @param leans        how much the group leans on each part of a defence, as {@link #leansOf} counts it
     */
    record Bench(ProbeRig.Loadout mix, double mixScale, Map<PowerProbe, Double> pure, double workAWalker,
                 Map<String, Double> leans) {

        static Bench measured() {
            EnemyPowerReport.Baseline walker = EnemyPowerReport.Baseline.of(Rank.GRUNT);
            ProbeRig.Loadout mix = walker.mixAt().apply(1.0).holding(ProbeRig.Hold.NONE);
            LevelDefinition lane = PowerBench.lane(EnemyPowerReport.NO_CONTENT,
                    EnemyPowerReport.group(EnemyPowerReport.BASELINE, Rank.GRUNT));
            double scale = PowerBench.threshold(lane, mix::scaledBy, 1, DamageMix.none());
            double work = walker.pure().get(PowerProbe.SHOT) * seconds(PowerBench.passage(lane));
            return new Bench(mix, scale, walker.pure(), work / EnemyPowerReport.groupAt(Rank.GRUNT),
                    leansOf(lane, mix::scaledBy, scale));
        }

        /**
         * How much the first round of {@code level} leans on each part of a defence: how many times the mix it
         * takes once one kind of damage is taken out of it, and how many times less once every enemy is kept in
         * sight or held back.
         *
         * @param whole the scale of the mix that holds the round
         */
        static Map<String, Double> leansOf(LevelDefinition level, DoubleFunction<ProbeRig.Loadout> mixAt, double whole) {
            Map<String, Double> leans = new LinkedHashMap<>();
            for (PowerProbe probe : PowerProbe.values()) {
                double without = PowerBench.threshold(level, scale -> mixAt.apply(scale).with(probe, 0), whole,
                        DamageMix.none());
                leans.put(probe.name().toLowerCase(Locale.ROOT), without / whole);
            }
            leans.put(REVEAL, whole / PowerBench.threshold(level, scale -> mixAt.apply(scale).revealing(), whole,
                    DamageMix.none()));
            leans.put(HOLD, whole / PowerBench.threshold(level,
                    scale -> mixAt.apply(scale).holding(ProbeRig.Hold.CHILL), whole, DamageMix.none()));
            return leans;
        }

        /** The single shots a second that hold what a scale of the mix holds. */
        double shotsFor(double scale) {
            return scale / this.mixScale * this.pure.get(PowerProbe.SHOT);
        }
    }

    /**
     * The line a level's room follows from round to round, drawn through the logarithms of every round's room: what a
     * round is held against, so a level may grow tighter or looser as it goes and still be even.
     */
    record Trend(double first, double perRound) {

        static Trend through(List<RoundPower> powers, double[] banks) {
            double rounds = 0;
            double sumRound = 0;
            double sumRoom = 0;
            double sumSquare = 0;
            double sumProduct = 0;
            for (RoundPower power : powers) {
                double room = Math.log(banks[power.round() - 1] / power.need());
                if (Double.isInfinite(room) || Double.isNaN(room)) {
                    continue;
                }
                rounds++;
                sumRound += power.round();
                sumRoom += room;
                sumSquare += (double) power.round() * power.round();
                sumProduct += power.round() * room;
            }
            double spread = rounds * sumSquare - sumRound * sumRound;
            double slope = spread == 0 ? 0 : (rounds * sumProduct - sumRound * sumRoom) / spread;
            return new Trend(rounds == 0 ? 0 : (sumRoom - slope * sumRound) / rounds, slope);
        }

        /** The room the line gives round {@code round}. */
        double at(int round) {
            return Math.exp(this.first + this.perRound * round);
        }
    }

    private static double seconds(PowerBench.Passage passage) {
        return (passage.walkTicks() + passage.streamTicks()) / (double) TickRate.TICKS_PER_SECOND;
    }

    /**
     * What a defence is bought of: the average of the towers the bench measured, bare and bought whole to each tier.
     * It is no tower a player can buy; it prices a defence by what towers hold on average, so one tower's retune
     * moves every level's rooms a little and no level's much.
     *
     * @param steps  the tower bare, then at each tier in order
     * @param types  how many towers it is the average of: that many are bought before one is a copy
     * @param growth what each further copy of a tower costs, as a multiple of the one before
     */
    record Yardstick(List<Step> steps, int types, double growth) {

        Yardstick {
            steps = List.copyOf(steps);
        }

        /**
         * One state of the tower.
         *
         * @param cost    every credit spent on it so far, at the first copy's price
         * @param walkers the Walkers of a mixed wave it adds to a defence on the bench's lane
         */
        record Step(double cost, double walkers) {
        }

        static Yardstick of(TowerSheet sheet) {
            List<Step> steps = new ArrayList<>();
            for (int stage = -1; stage <= sheet.topStage(); stage++) {
                int at = stage;
                List<TowerSheet.Held> best = sheet.judged().stream()
                        .flatMap(tower -> sheet.stage(tower, at).stream()
                                .max((left, right) -> Double.compare(sheet.power(left) / left.cost(),
                                        sheet.power(right) / right.cost())).stream())
                        .toList();
                steps.add(new Step(best.stream().mapToInt(TowerSheet.Held::cost).average().orElse(0),
                        best.stream().mapToDouble(sheet::power).average().orElse(0)));
            }
            return new Yardstick(steps, sheet.judged().size(), TowerFactory.Type.COPY_PRICE_GROWTH);
        }

        /** What moving a tower from {@code built} steps to one more adds, in Walkers. */
        double more(int built) {
            return this.steps.get(built).walkers() - (built == 0 ? 0 : this.steps.get(built - 1).walkers());
        }

        /** What that step costs when {@code towers} stand already: a new tower pays for the copies before it. */
        double price(int built, int towers) {
            if (built > 0) {
                return this.steps.get(built).cost() - this.steps.get(built - 1).cost();
            }
            return this.steps.getFirst().cost() * Math.pow(this.growth, towers / Math.max(1, this.types));
        }
    }

    /**
     * A level's board as the yardstick tower sees it.
     *
     * @param cells every cell a tower may stand on
     * @param reach for each cell and lane, the lane the tower reaches from it over what it reaches on the bench: how
     *              many times as long an enemy is under its fire
     */
    record Ground(Yardstick yardstick, List<Point> cells, List<double[]> reach) {

        static Ground of(LevelDefinition level, Yardstick yardstick) {
            BoardSurvey survey = BoardSurvey.of(level);
            BoardSurvey bench = BoardSurvey.of(PowerBench.lane(EnemyPowerReport.NO_CONTENT, "walker"));
            double cells = Arrays.stream(TowerFactory.Type.values())
                    .filter(type -> TowerSheet.isJudged(type.name()))
                    .mapToDouble(type -> bench.reach(type) / bench.scale()).average().orElse(1);
            double onBench = bench.pathWithin(TowerBench.SUBJECT, cells * bench.scale(), 0) / bench.scale();
            List<double[]> reach = new ArrayList<>();
            for (Point cell : survey.buildable()) {
                double[] shares = new double[survey.pathCount()];
                for (int lane = 0; lane < shares.length; lane++) {
                    shares[lane] = survey.pathWithin(cell, cells * survey.scale(), lane) / survey.scale() / onBench;
                }
                reach.add(shares);
            }
            return new Ground(yardstick, survey.buildable(), reach);
        }

        /**
         * The credits the cheapest defence found costs that holds each lane's Walkers. A tower serves one lane, and
         * holds of it what it holds on the bench times how much more of the lane it reaches. Past what the board has
         * room for, the rest is priced as the last tower bought was, so the figure still says how far out of reach
         * the round is.
         */
        double cost(List<LanePower> lanes) {
            double[] left = lanes.stream().mapToDouble(LanePower::walkers).toArray();
            int[] built = new int[this.cells.size()];
            int[] serves = new int[this.cells.size()];
            Arrays.fill(serves, -1);
            int steps = this.yardstick.steps().size();
            int towers = 0;
            double spent = 0;
            double lastPrice = Double.POSITIVE_INFINITY;
            while (Arrays.stream(left).anyMatch(need -> need > DONE)) {
                int bestCell = -1;
                int bestLane = -1;
                double bestGain = 0;
                for (int cell = 0; cell < built.length; cell++) {
                    if (built[cell] >= steps) {
                        continue;
                    }
                    double more = this.yardstick.more(built[cell]);
                    double price = this.yardstick.price(built[cell], towers);
                    for (int lane = 0; lane < left.length; lane++) {
                        if (left[lane] <= DONE || serves[cell] >= 0 && serves[cell] != lane) {
                            continue;
                        }
                        double gain = Math.min(left[lane], more * this.reach.get(cell)[lane]) / price;
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestCell = cell;
                            bestLane = lane;
                        }
                    }
                }
                if (bestCell < 0) {
                    return spent + Arrays.stream(left).filter(need -> need > DONE).sum() * lastPrice;
                }
                spent += this.yardstick.price(built[bestCell], towers);
                left[bestLane] -= this.yardstick.more(built[bestCell]) * this.reach.get(bestCell)[bestLane];
                lastPrice = 1 / bestGain;
                serves[bestCell] = bestLane;
                towers += built[bestCell] == 0 ? 1 : 0;
                built[bestCell]++;
            }
            return spent;
        }
    }

    /**
     * What one lane's wave of a round takes.
     *
     * @param script  the wave as written
     * @param walkers its strength
     * @param bounty  what its kills pay, and so what a tower that reaches all of the lane earns of it
     */
    record LanePower(String script, double walkers, int bounty) {
    }

    /**
     * One round of a level.
     *
     * @param round   counted from one
     * @param walkers its strength, every lane's added up
     * @param answers how many times as much as Walkers it leans on each part of a defence; empty when nothing
     *                holds it
     * @param bounty  what its kills pay
     * @param bonus   what clearing it with no leak pays
     * @param need    what the yardstick's defence costs
     */
    record RoundPower(int round, List<LanePower> lanes, double walkers, Map<String, Double> answers, int bounty,
                      int bonus, double need) {

        static RoundPower of(LevelDefinition level, int round, Bench bench, Ground ground) {
            int last = level.paths().getFirst().waves().size() - 1;
            float hits = 1f + (LAST_HIT_SCALE - 1f) * round / Math.max(1, last);
            DoubleFunction<ProbeRig.Loadout> mixAt = scale -> bench.mix().scaledBy(scale).hitting(hits);
            List<LanePower> lanes = new ArrayList<>();
            for (int lane = 0; lane < level.paths().size(); lane++) {
                LevelDefinition alone = PowerBench.oneLane(level, round, lane);
                PowerBench.Passage passage = PowerBench.passage(alone);
                double scale = passage.empty() ? 0 : PowerBench.threshold(alone, mixAt, 1, DamageMix.none());
                double work = passage.empty() ? 0 : bench.shotsFor(scale) * seconds(passage);
                int pays = passage.empty() || Double.isInfinite(scale) ? 0
                        : PowerBench.run(alone, mixAt.apply(scale * PAY_HEADROOM), DamageMix.none(), PAY_SEED).credits();
                lanes.add(new LanePower(level.paths().get(lane).waves().get(round).enemies(),
                        work / bench.workAWalker(), pays));
            }
            double walkers = lanes.stream().mapToDouble(LanePower::walkers).sum();

            LevelDefinition whole = PowerBench.oneRound(level, round);
            double mix = PowerBench.threshold(whole, mixAt, 1, DamageMix.none());
            Map<String, Double> answers = new LinkedHashMap<>();
            if (!Double.isInfinite(mix)) {
                Bench.leansOf(whole, mixAt, mix)
                        .forEach((part, lean) -> answers.put(part, lean / bench.leans().get(part)));
            }
            int bounty = Double.isInfinite(mix) ? 0
                    : PowerBench.run(whole, mixAt.apply(mix * PAY_HEADROOM), DamageMix.none(), PAY_SEED).credits();
            int bonus = level.paths().stream().mapToInt(path -> path.waves().get(round).clearBonus()).sum();
            return new RoundPower(round + 1, lanes, walkers, answers, bounty, bonus, ground.cost(lanes));
        }

        /** The parts of a defence it leans on more than Walkers do, the most first. */
        String asksFor() {
            return this.answers.entrySet().stream()
                    .filter(answer -> answer.getValue() >= ASKS_FROM)
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(answer -> Double.isInfinite(answer.getValue()) ? "needs " + answer.getKey()
                            : String.format(Locale.ROOT, "%s %.1f", answer.getKey(), answer.getValue()))
                    .reduce((left, right) -> left + ", " + right).orElse("");
        }
    }
}
