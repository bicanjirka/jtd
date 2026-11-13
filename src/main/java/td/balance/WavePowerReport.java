package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.damage.DamageMix;
import td.enemy.Rank;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.util.TickRate;
import td.wave.Point;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * lane by lane, in Walkers of the lowest rank; which parts of a defence it asks for; what it pays; and what the
 * defence that just holds it costs when it is built of the paper tools' reference tower. A round is measured as an
 * enemy is, under a {@link ProbeRig}, on the level's own board, so its lanes' length and what its enemies do to
 * each other count. Run it:
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=td.balance.WavePowerReport [-Dexec.args="--levels rift,hive"]
 * </pre>
 *
 * It prints one table per level and stores every row in {@code target/balance/wave-power.tsv}. The cost is a
 * yardstick, not a forecast: read how it moves from round to round against the credits, not where it stands.
 */
public final class WavePowerReport {

    private static final Path STORE = Path.of("target", "balance", "wave-power.tsv");
    /** How many times their first size a defence's hits have grown by a level's last round. */
    private static final float LAST_HIT_SCALE = 1.5f;
    /** A round asks for a part of a defence it leans on this many times as much as Walkers do. */
    private static final double ASKS_FROM = 1.4;
    private static final String REVEAL = "reveal";
    private static final String HOLD = "hold";
    /** How far above what just holds a round the rig stands when the round's pay is counted. */
    private static final double PAY_HEADROOM = 1.5;
    private static final long PAY_SEED = 1L;
    /** What the weakest tower's defence costs against the reference tower's. */
    private static final double FLOOR_COST = 1.2;
    private static final double DONE = 1e-6;

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
        PaperYardstick yardstick = PaperYardstick.read(PaperYardstick.STORED);
        List<String> stored = new ArrayList<>(List.of(RoundPower.storedHeader()));
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            String name = level.name().toLowerCase(Locale.ROOT);
            if (!only.isEmpty() && only.stream().noneMatch(name::contains)) {
                continue;
            }
            Ground ground = Ground.of(level, yardstick);
            int rounds = level.paths().getFirst().waves().size();
            List<RoundPower> powers = IntStream.range(0, rounds).parallel()
                    .mapToObj(round -> RoundPower.of(level, round, bench, ground)).toList();
            System.out.printf(Locale.ROOT, "%n%s: starts with %d credits, %d lives, %d lanes%n", level.name(),
                    level.startingCredits(), level.startingLives(), level.paths().size());
            System.out.println(RoundPower.header());
            double[] banks = new double[rounds];
            double bank = level.startingCredits();
            for (RoundPower power : powers) {
                banks[power.round() - 1] = bank;
                bank += power.bounty() + power.bonus();
            }
            Trend trend = Trend.through(powers, banks);
            for (RoundPower power : powers) {
                double had = banks[power.round() - 1];
                System.out.println(power.row(had, trend.at(power.round())));
                stored.add(power.stored(level, had, trend.at(power.round())));
            }
        }
        try {
            Files.createDirectories(STORE.getParent());
            Files.write(STORE, stored, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println();
        System.out.println("walkers: the round's strength in Walkers of the lowest rank walking the test lane in a column;"
                + " need: the reference tower's cheapest defence that holds it; floor: the weakest tower's;"
                + " room: credits banked before the round over need; even: room over the line the level's rooms"
                + " follow, so 1 is a round as tight as its place in the level asks; asks for: the part of a defence it leans on"
                + " that many times as much as Walkers do (a kind of damage, keeping enemies in sight, holding them).");
        System.out.println("Stored in " + STORE);
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
     * A level's board as the reference tower sees it.
     *
     * @param cells every cell a tower may stand on
     * @param reach for each cell, the share of each lane the tower reaches from it
     */
    record Ground(PaperYardstick yardstick, List<Point> cells, List<double[]> reach) {

        static Ground of(LevelDefinition level, PaperYardstick yardstick) {
            BoardSurvey survey = BoardSurvey.of(level);
            double radius = yardstick.reachCells() * survey.scale();
            double everywhere = (double) survey.scale() * (level.width() + level.height()) * 2;
            List<double[]> reach = new ArrayList<>();
            for (Point cell : survey.buildable()) {
                double[] shares = new double[survey.pathCount()];
                for (int lane = 0; lane < shares.length; lane++) {
                    shares[lane] = survey.pathWithin(cell, radius, lane) / survey.pathWithin(cell, everywhere, lane);
                }
                reach.add(shares);
            }
            return new Ground(yardstick, survey.buildable(), reach);
        }

        /**
         * The credits the cheapest defence found costs that lands on each lane the shots a second it takes. A tower
         * fires while the stream passes it, so a short reach costs it only its share of the walk; it serves one lane.
         * Past what the board has room for, the rest is priced as the last tower bought was, so the figure still
         * says how far out of reach the round is.
         */
        double cost(List<LanePower> lanes) {
            double[] left = lanes.stream().mapToDouble(LanePower::shots).toArray();
            int[] built = new int[this.cells.size()];
            int[] serves = new int[this.cells.size()];
            Arrays.fill(serves, -1);
            List<PaperYardstick.Step> steps = this.yardstick.steps();
            double spent = 0;
            double lastPrice = Double.POSITIVE_INFINITY;
            while (Arrays.stream(left).anyMatch(need -> need > DONE)) {
                int bestCell = -1;
                int bestLane = -1;
                double bestGain = 0;
                for (int cell = 0; cell < built.length; cell++) {
                    if (built[cell] >= steps.size()) {
                        continue;
                    }
                    double more = steps.get(built[cell]).damagePerSecond() - damageAt(steps, built[cell]);
                    double price = steps.get(built[cell]).cost() - costAt(steps, built[cell]);
                    for (int lane = 0; lane < left.length; lane++) {
                        if (left[lane] <= DONE || serves[cell] >= 0 && serves[cell] != lane) {
                            continue;
                        }
                        double gain = Math.min(left[lane], more * lanes.get(lane).share(this.reach.get(cell)[lane])) / price;
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
                double more = steps.get(built[bestCell]).damagePerSecond() - damageAt(steps, built[bestCell]);
                spent += steps.get(built[bestCell]).cost() - costAt(steps, built[bestCell]);
                left[bestLane] -= more * lanes.get(bestLane).share(this.reach.get(bestCell)[bestLane]);
                lastPrice = 1 / bestGain;
                serves[bestCell] = bestLane;
                built[bestCell]++;
            }
            return spent;
        }

        private static double damageAt(List<PaperYardstick.Step> steps, int built) {
            return built == 0 ? 0 : steps.get(built - 1).damagePerSecond();
        }

        private static double costAt(List<PaperYardstick.Step> steps, int built) {
            return built == 0 ? 0 : steps.get(built - 1).cost();
        }
    }

    /**
     * What one lane's wave of a round takes.
     *
     * @param script  the wave as written
     * @param shots   the single shots a second, landed all along the lane, that hold it
     * @param passage how it walks the lane undefended
     */
    record LanePower(String script, double shots, PowerBench.Passage passage) {

        /** The share of {@link #shots} a tower lands that reaches {@code reach} of the lane. */
        double share(double reach) {
            double walk = this.passage.walkTicks();
            double stream = this.passage.streamTicks();
            return Math.min(1.0, (reach * walk + stream) / (walk + stream));
        }

        double work() {
            return this.passage.empty() ? 0 : this.shots * seconds(this.passage);
        }
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
     * @param need    what the reference tower's defence costs
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
                lanes.add(new LanePower(level.paths().get(lane).waves().get(round).enemies(), bench.shotsFor(scale), passage));
            }
            double walkers = lanes.stream().mapToDouble(LanePower::work).sum() / bench.workAWalker();

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

        static String header() {
            return String.format(Locale.ROOT, "%5s %8s %7s %6s %6s %7s %7s %6s %6s  %s", "round", "walkers",
                    "bounty", "bonus", "bank", "need", "floor", "room", "even", "asks for");
        }

        String row(double bank, double trend) {
            return String.format(Locale.ROOT, "%5d %8.1f %7d %6d %6.0f %7.0f %7.0f %6.2f %6.2f  %s", this.round, this.walkers,
                    this.bounty, this.bonus, bank, this.need, this.need * FLOOR_COST, bank / this.need,
                    bank / this.need / trend, this.asksFor());
        }

        static String storedHeader() {
            return "level\tround\twalkers\tbounty\tbonus\tbank\tneed\troom\teven\tasks\tlanes";
        }

        String stored(LevelDefinition level, double bank, double trend) {
            StringBuilder line = new StringBuilder(String.format(Locale.ROOT,
                    "%s\t%d\t%.1f\t%d\t%d\t%.0f\t%.0f\t%.2f\t%.2f", level.name(), this.round, this.walkers, this.bounty,
                    this.bonus, bank, this.need, bank / this.need, bank / this.need / trend));
            line.append('\t').append(this.asksFor());
            StringBuilder scripts = new StringBuilder();
            for (LanePower lane : this.lanes) {
                scripts.append(scripts.isEmpty() ? "" : " | ")
                        .append(String.format(Locale.ROOT, "[%.0f] %s", lane.work(), lane.script()));
            }
            return line.append('\t').append(scripts).toString();
        }
    }
}
