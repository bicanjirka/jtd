package td.balance;

import td.GameEngine;
import td.damage.DamageUnits;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SpawnEnemiesAction;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.GameHost;
import td.util.LoadedLevel;
import td.util.RandomSource;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Wave;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * What a tower is worth before anyone plays, measured rather than guessed, on the level's own board. Two kinds of
 * run:
 * <ul>
 *     <li>A <em>probe round</em>: enemies too tough to die walk a path past one tower. What it lands depends only
 *     on where it stands, so this ranks every cell for every tower type and path.</li>
 *     <li>A <em>duel</em>: one tower on its best cell against a wave of real enemies of one kind, all given the
 *     same health, set so the bare tower lands about a third of the wave. Enemies that die, turn invisible when
 *     hit, heal each other or split do so, so this measures each tower against each kind, and each upgrade line,
 *     as the share of the wave it brings down.</li>
 * </ul>
 * The Beacon lands nothing itself; it is measured by what towers beside it gain. What any other tower does for
 * its neighbours (a slow that keeps enemies in reach longer, a mark that makes them take more) is measured the
 * same way, on a fixed partner tower beside it in a probe round. The numbers compare towers, cells, kinds and
 * nodes with each other; they never stand for how a level plays.
 */
public final class TowerLab {

    /** The kind every other is compared with: the plain mob. */
    public static final String PLAIN = "c";
    private static final String AT = "@";
    /** Probes and duels are fought at this rank: armor and resistances have grown in. */
    private static final Rank LAB_RANK = Rank.VETERAN;
    private static final int PROBE_HEALTH = 1_000_000;
    private static final int PROBES_PER_PATH = 8;
    private static final int DUEL_ENEMIES_PER_PATH = 12;
    /** The share of a plain duel wave the bare tower is tuned to bring down, so a node can show up to 3x. */
    private static final double DUEL_SHARE = 0.3;
    private static final int CALIBRATION_STEPS = 12;
    private static final String PROBE_PREFIX = "probe_";
    private static final String DUEL_PREFIX = "duel_";
    private static final String NO_ENEMY = "e";
    private static final int TICK_BUDGET = 20_000;
    private static final long FIRST_SEED = 1L;
    /** Duels and node runs average this many seeds: what they compare differs by a few percent. */
    private static final int SEEDS = 3;
    /** Partners a Beacon's nodes are measured on: a node that picks whom it buffs needs several to choose from. */
    private static final int PARTNERS = 4;
    /**
     * The tower a Beacon's nodes, and what every other tower does for its neighbours, are measured on. Fixed, so
     * a change to some other tower never moves those numbers; the cheapest, so a cluster fits anywhere.
     */
    private static final TowerFactory.Type PARTNER = TowerFactory.Type.SNIPER;
    private static final int GRANTED_CREDITS = 10_000_000;
    /** Lab enemies leak by design; the round, not the lives, has to end the run. */
    private static final int UNLIMITED_LIVES = 1_000_000;

    private final BoardSurvey survey;
    private final Map<TowerFactory.Type, Map<Point, double[]>> cellValues;
    private final Map<TowerFactory.Type, Map<Point, Double>> cellTotals;
    private final Map<TowerFactory.Type, TypeProfile> profiles;
    private final List<String> levelKinds;
    private final List<String> levelRanks;

    /**
     * What the duels found for one tower type.
     *
     * @param duelHealth  the health each duel enemy had
     * @param kindFactors the share it brings down of each kind's wave, over the share of the plain one, keyed by
     *                    {@link #kindKey}
     * @param nodeFactors what each node multiplies that share by once the nodes before it are owned
     * @param partners    what it gains beside a Beacon, and what it gives a tower beside it
     */
    public record TypeProfile(double duelHealth, Map<String, Double> kindFactors, Map<String, Double> nodeFactors,
                              Partnering partners) {

        public TypeProfile {
            kindFactors = Collections.unmodifiableMap(new LinkedHashMap<>(kindFactors));
            nodeFactors = Collections.unmodifiableMap(new LinkedHashMap<>(nodeFactors));
        }

        /** The share more it lands with a Beacon beside it. */
        public double beaconGain() {
            return this.partners.beaconGain();
        }
    }

    /**
     * How a tower type and the towers beside it help each other.
     *
     * @param beaconGain   the share more it lands with a Beacon beside it
     * @param supportGain  the share more the partner tower lands with a bare one beside it
     * @param supportSteps what each node adds to {@code supportGain} once the nodes before it are owned
     */
    public record Partnering(double beaconGain, double supportGain, Map<String, Double> supportSteps) {

        public Partnering {
            supportSteps = Collections.unmodifiableMap(new LinkedHashMap<>(supportSteps));
        }

        static Partnering none() {
            return new Partnering(0, 0, Map.of());
        }
    }

    private TowerLab(BoardSurvey survey, Map<TowerFactory.Type, Map<Point, double[]>> cellValues,
                     Map<TowerFactory.Type, TypeProfile> profiles) {
        this.survey = survey;
        this.cellValues = cellValues;
        Map<TowerFactory.Type, Map<Point, Double>> totals = new EnumMap<>(TowerFactory.Type.class);
        cellValues.forEach((type, cells) -> {
            Map<Point, Double> byCell = new HashMap<>();
            cells.forEach((cell, byPath) -> byCell.put(cell, Arrays.stream(byPath).sum()));
            totals.put(type, byCell);
        });
        this.cellTotals = totals;
        this.profiles = profiles;
        this.levelKinds = kindsIn(survey.level());
        this.levelRanks = ranksIn(survey.level());
    }

    /** Runs every measurement for {@code survey}'s level, in parallel. Takes a few seconds. */
    public static TowerLab of(BoardSurvey survey) {
        LevelDefinition level = survey.level();
        List<String> kinds = new ArrayList<>(probedKinds(level).stream().map(kind -> kindKey(kind, LAB_RANK)).toList());
        ranksIn(level).stream().filter(key -> !kinds.contains(key)).forEach(kinds::add);
        Map<TowerFactory.Type, Map<Point, double[]>> cellValues = new EnumMap<>(TowerFactory.Type.class);
        for (TowerFactory.Type type : damageDealers()) {
            cellValues.put(type, cellValuesOf(survey, type));
        }
        cellValues.put(TowerFactory.Type.AURA, Map.of());

        LevelDefinition plainProbes = probeLevel(level, PLAIN, -1);
        List<TypeProfile> measured = damageDealers().parallelStream()
                .map(type -> profileOf(survey, plainProbes, type, best(cellValues.get(type)), kinds)).toList();
        Map<TowerFactory.Type, TypeProfile> profiles = new EnumMap<>(TowerFactory.Type.class);
        IntStream.range(0, measured.size()).forEach(i -> profiles.put(damageDealers().get(i), measured.get(i)));
        Point partnerCell = best(cellValues.get(PARTNER));
        Map<String, Double> beaconNodes = besideCell(survey, partnerCell)
                .map(beacon -> beaconNodeFactors(plainProbes, PARTNER, partnerCell, beacon, survey))
                .orElse(Map.of());
        profiles.put(TowerFactory.Type.AURA, new TypeProfile(0, Map.of(), beaconNodes, Partnering.none()));
        return new TowerLab(survey, Collections.unmodifiableMap(cellValues),
                Collections.unmodifiableMap(new EnumMap<>(profiles)));
    }

    private static TypeProfile profileOf(BoardSurvey survey, LevelDefinition plainProbes, TowerFactory.Type type,
                                         Point cell, List<String> kinds) {
        LevelDefinition level = survey.level();
        double health = calibrate(level, type, cell);
        double plainShare = Math.max(1e-6, duelShare(level, PLAIN, LAB_RANK, health, type, cell, Optional.empty()));
        Map<String, Double> kindFactors = new LinkedHashMap<>();
        for (String key : kinds) {
            String[] parts = key.split(AT);
            kindFactors.put(key, duelShare(level, parts[0], Rank.valueOf(parts[1]), health, type, cell,
                    Optional.empty()) / plainShare);
        }
        Tower sample = sampleOf(plainProbes, type, cell);
        Map<String, Double> wholeLines = new LinkedHashMap<>();
        for (UpgradeNode node : sample.upgradeTree().nodes()) {
            wholeLines.put(node.id(), duelShare(level, PLAIN, LAB_RANK, health, type, cell, Optional.of(node))
                    / plainShare);
        }
        double alone = Math.max(1, damageOf(plainProbes, type, cell, List.of(), Optional.empty(), FIRST_SEED));
        double beaconGain = besideCell(survey, cell)
                .map(beacon -> damageOf(plainProbes, type, cell, List.of(new Placement(TowerFactory.Type.AURA, beacon)),
                        Optional.empty(), FIRST_SEED) / alone - 1)
                .orElse(0.0);
        Partnering partners = besideCell(survey, cell)
                .map(partnerCell -> supportOf(plainProbes, sample, cell, partnerCell, beaconGain))
                .orElse(new Partnering(beaconGain, 0, Map.of()));
        return new TypeProfile(health, kindFactors, factorsFromLines(sample, wholeLines), partners);
    }

    /**
     * What {@code sample}'s type on {@code cell}, bare and with each whole line, adds to the partner on
     * {@code partnerCell}: the share more the partner lands in a probe round. A tower that only deals damage adds
     * nothing; a slow, a mark or a debuff shows here and nowhere else.
     */
    private static Partnering supportOf(LevelDefinition probes, Tower sample, Point cell, Point partnerCell,
                                        double beaconGain) {
        TowerFactory.Type type = sample.getType();
        double alone = Math.max(1, partnerDamage(probes, partnerCell, type, cell, Optional.empty(), false));
        double bare = Math.max(0, partnerDamage(probes, partnerCell, type, cell, Optional.empty(), true) / alone - 1);
        Map<String, Double> wholeLines = new LinkedHashMap<>();
        for (UpgradeNode node : sample.upgradeTree().nodes()) {
            wholeLines.put(node.id(),
                    Math.max(0, partnerDamage(probes, partnerCell, type, cell, Optional.of(node), true) / alone - 1));
        }
        Map<String, Double> steps = new LinkedHashMap<>();
        for (UpgradeNode node : sample.upgradeTree().nodes()) {
            List<UpgradeNode> line = sample.upgradeTree().pathTo(node);
            double before = line.size() < 2 ? bare : wholeLines.get(line.get(line.size() - 2).id());
            steps.put(node.id(), wholeLines.get(node.id()) - before);
        }
        return new Partnering(beaconGain, bare, steps);
    }

    /**
     * What the partner on {@code partnerCell} lands in one probe round, averaged over seeds, alone or with a
     * {@code type} owning {@code line} on {@code cell} beside it.
     */
    private static double partnerDamage(LevelDefinition probes, Point partnerCell, TowerFactory.Type type, Point cell,
                                        Optional<UpgradeNode> line, boolean withSubject) {
        return LongStream.range(FIRST_SEED, FIRST_SEED + SEEDS).mapToDouble(seed -> {
            GameEngine engine = labEngine(probes, seed);
            Tower partner = Turn.place(engine, PARTNER, partnerCell).orElseThrow();
            if (withSubject) {
                Tower subject = Turn.place(engine, type, cell).orElseThrow();
                line.ifPresent(node -> buyLine(subject, node));
            }
            runRound(engine);
            return partner.getDamageDealt();
        }).average().orElse(0);
    }

    /** The health per enemy at which the bare tower brings down {@link #DUEL_SHARE} of a plain duel wave. */
    private static double calibrate(LevelDefinition level, TowerFactory.Type type, Point cell) {
        double low = Math.log(1);
        double high = Math.log(PROBE_HEALTH);
        for (int step = 0; step < CALIBRATION_STEPS; step++) {
            double middle = (low + high) / 2;
            if (duelShare(level, PLAIN, LAB_RANK, Math.exp(middle), type, cell, Optional.empty()) > DUEL_SHARE) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return Math.exp((low + high) / 2);
    }

    /**
     * The share of a duel wave of {@code kind} the tower on {@code cell} brings down, averaged over seeds: what it
     * landed over the health of everything that walked in, reinforcements and splits included.
     */
    private static double duelShare(LevelDefinition level, String kind, Rank rank, double health,
                                    TowerFactory.Type type, Point cell, Optional<UpgradeNode> line) {
        LevelDefinition duel = duelLevel(level, kind, rank, (int) Math.max(1, Math.round(health)));
        return LongStream.range(FIRST_SEED, FIRST_SEED + SEEDS).mapToDouble(seed -> {
            GameEngine engine = labEngine(duel, seed);
            Tower subject = Turn.place(engine, type, cell).orElseThrow();
            line.ifPresent(node -> buyLine(subject, node));
            long healthIn = runRound(engine);
            return healthIn <= 0 ? 0 : Math.min(1, subject.getDamageDealt() / (healthIn * (double) DamageUnits.PER_POINT));
        }).average().orElse(0);
    }

    /** Every buildable cell's damage for each path alone; 0 where the tower reaches no path. */
    private static Map<Point, double[]> cellValuesOf(BoardSurvey survey, TowerFactory.Type type) {
        int paths = survey.pathCount();
        List<LevelDefinition> labs = IntStream.range(0, paths)
                .mapToObj(path -> probeLevel(survey.level(), PLAIN, path)).toList();
        List<double[]> values = survey.buildable().parallelStream()
                .map(cell -> IntStream.range(0, paths)
                        .mapToDouble(path -> survey.pathWithin(cell, survey.reach(type), path) <= 0 ? 0.0
                                : damageOf(labs.get(path), type, cell, List.of(), Optional.empty(), FIRST_SEED))
                        .toArray())
                .toList();
        Map<Point, double[]> byCell = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            byCell.put(survey.buildable().get(i), values.get(i));
        }
        return Collections.unmodifiableMap(byCell);
    }

    /** The Beacon's nodes, measured by what a cluster of partners around it lands in a probe round. */
    private static Map<String, Double> beaconNodeFactors(LevelDefinition probes, TowerFactory.Type partner, Point cell,
                                                         Point beacon, BoardSurvey survey) {
        Tower sample = sampleOf(probes, TowerFactory.Type.AURA, beacon);
        List<Point> partners = survey.buildable().stream().filter(other -> !other.equals(beacon))
                .filter(other -> Math.hypot(other.x() - beacon.x(), other.y() - beacon.y()) * survey.scale()
                        <= survey.reach(TowerFactory.Type.AURA))
                .sorted(Comparator.comparingDouble(other -> Math.hypot(other.x() - cell.x(), other.y() - cell.y())))
                .limit(PARTNERS).toList();
        double alone = Math.max(1, clusterDamage(probes, partner, partners, beacon, Optional.empty(), false));
        double gain = clusterDamage(probes, partner, partners, beacon, Optional.empty(), true) / alone - 1;
        Map<String, Double> wholeLines = new LinkedHashMap<>();
        for (UpgradeNode node : sample.upgradeTree().nodes()) {
            double with = clusterDamage(probes, partner, partners, beacon, Optional.of(node), true) / alone - 1;
            wholeLines.put(node.id(), gain <= 0 ? 1 : Math.max(0, with) / gain);
        }
        return factorsFromLines(sample, wholeLines);
    }

    /**
     * Turns what each whole line measured into what each node adds once the nodes before it are owned, so the
     * factors of the nodes a tower owns multiply up to what its whole build measures.
     */
    private static Map<String, Double> factorsFromLines(Tower sample, Map<String, Double> wholeLines) {
        Map<String, Double> factors = new LinkedHashMap<>();
        for (UpgradeNode node : sample.upgradeTree().nodes()) {
            double before = sample.upgradeTree().pathTo(node).stream()
                    .filter(step -> !step.equals(node))
                    .mapToDouble(step -> factors.getOrDefault(step.id(), 1.0))
                    .reduce(1.0, (a, b) -> a * b);
            factors.put(node.id(), before <= 0 ? 1.0 : wholeLines.get(node.id()) / before);
        }
        return factors;
    }

    private static Tower sampleOf(LevelDefinition lab, TowerFactory.Type type, Point cell) {
        return Turn.place(labEngine(lab, FIRST_SEED), type, cell).orElseThrow();
    }

    /** What {@code partner}s on {@code cells} land together, with a Beacon on {@code beacon} owning {@code node}'s line. */
    private static double clusterDamage(LevelDefinition lab, TowerFactory.Type partner, List<Point> cells,
                                        Point beacon, Optional<UpgradeNode> node, boolean withBeacon) {
        return LongStream.range(FIRST_SEED, FIRST_SEED + SEEDS).mapToDouble(seed -> {
            GameEngine engine = labEngine(lab, seed);
            List<Tower> subjects = cells.stream().map(cell -> Turn.place(engine, partner, cell).orElseThrow()).toList();
            if (withBeacon) {
                Tower aura = Turn.place(engine, TowerFactory.Type.AURA, beacon).orElseThrow();
                node.ifPresent(target -> buyLine(aura, target));
            }
            runRound(engine);
            return subjects.stream().mapToLong(Tower::getDamageDealt).sum();
        }).average().orElse(0);
    }

    /** How many probes walk each path in a probe round: a cell's value spread over one enemy is it over this. */
    public static int probesPerPath() {
        return PROBES_PER_PATH;
    }

    /** The towers that land damage themselves: every type but the Beacon. */
    public static List<TowerFactory.Type> damageDealers() {
        return Arrays.stream(TowerFactory.Type.values()).filter(type -> type != TowerFactory.Type.AURA).toList();
    }

    public BoardSurvey survey() {
        return this.survey;
    }

    /** Damage {@code type} alone on {@code cell} lands in one round of plain probes on every path; 0 for the Beacon. */
    public double cellValue(TowerFactory.Type type, Point cell) {
        return this.cellTotals.get(type).getOrDefault(cell, 0.0);
    }

    private static double cellValueOf(Map<TowerFactory.Type, Map<Point, double[]>> values, TowerFactory.Type type,
                                      Point cell) {
        double[] byPath = values.get(type).get(cell);
        return byPath == null ? 0 : Arrays.stream(byPath).sum();
    }

    /** Damage {@code type} alone on {@code cell} lands in one round of plain probes on path {@code path} alone. */
    public double cellValue(TowerFactory.Type type, Point cell, int path) {
        double[] byPath = this.cellValues.get(type).get(cell);
        return byPath == null ? 0 : byPath[path];
    }

    /** The cell where {@code type} lands the most over every path. */
    public Point bestCell(TowerFactory.Type type) {
        return best(this.cellValues.get(type));
    }

    public TypeProfile profile(TowerFactory.Type type) {
        return this.profiles.get(type);
    }

    /**
     * How well {@code type} does against {@code kind} at {@code rank}, as a share of how well it does against the
     * plain mob; at the lab's rank when that rank was not measured.
     */
    public double kindFactor(TowerFactory.Type type, String kind, Rank rank) {
        Map<String, Double> factors = this.profiles.get(type).kindFactors();
        Double atRank = factors.get(kindKey(kind, rank));
        return atRank != null ? atRank : factors.getOrDefault(kindKey(kind, LAB_RANK), 1.0);
    }

    /** The kinds the lab compares at one rank: every enemy a round can send. */
    public List<String> probedKinds() {
        return this.profiles.get(TowerFactory.Type.SNIPER).kindFactors().keySet().stream()
                .filter(key -> key.endsWith(AT + LAB_RANK)).map(key -> key.split(AT)[0]).toList();
    }

    /** Every kind and rank this level's rounds send, as {@link #kindKey}s. */
    public List<String> levelRanks() {
        return this.levelRanks;
    }

    /** {@link #kindFactor(TowerFactory.Type, String, Rank)} for a {@link #kindKey}. */
    public double kindFactor(TowerFactory.Type type, String key) {
        String[] parts = key.split(AT);
        return this.kindFactor(type, parts[0], Rank.valueOf(parts[1]));
    }

    /** The key a kind at a rank goes by in {@link TypeProfile#kindFactors()}. */
    public static String kindKey(String kind, Rank rank) {
        return kind + AT + rank;
    }

    /** The rank the lab compares kinds at. */
    public static Rank labRank() {
        return LAB_RANK;
    }

    /** The kinds this level's rounds send. */
    public List<String> levelKinds() {
        return this.levelKinds;
    }

    /** The share more {@code type} lands with a Beacon beside it. */
    public double beaconGain(TowerFactory.Type type) {
        return this.profiles.get(type).beaconGain();
    }

    /**
     * The share more a damage tower lands beside a {@code type} that owns {@code owned}, from what the type does
     * to the enemies they share: 0 for a tower that only deals damage, and for the Beacon, whose buff is
     * {@link #beaconGain}.
     */
    public double supportGain(TowerFactory.Type type, Collection<String> owned) {
        Partnering partners = this.profiles.get(type).partners();
        return Math.max(0, partners.supportGain()
                + owned.stream().mapToDouble(node -> partners.supportSteps().getOrDefault(node, 0.0)).sum());
    }

    /**
     * What owning {@code nodeId} multiplies a {@code type}'s share of a duel wave by, once the nodes before it are
     * owned; for the Beacon, what it multiplies the Beacon's buff by. 1 for a node a lone tower cannot show.
     */
    public double nodeFactor(TowerFactory.Type type, String nodeId) {
        return this.profiles.get(type).nodeFactors().getOrDefault(nodeId, 1.0);
    }

    private static Point best(Map<Point, double[]> values) {
        return values.entrySet().stream()
                .max(Comparator.<Map.Entry<Point, double[]>>comparingDouble(entry -> Arrays.stream(entry.getValue()).sum())
                        .thenComparing(entry -> -entry.getKey().x())
                        .thenComparing(entry -> -entry.getKey().y()))
                .map(Map.Entry::getKey).orElseThrow();
    }

    /** The free cell nearest {@code cell}, for a Beacon or a partner beside it. */
    private static Optional<Point> besideCell(BoardSurvey survey, Point cell) {
        return survey.buildable().stream().filter(other -> !other.equals(cell))
                .min(Comparator.comparingDouble(other -> Math.hypot(other.x() - cell.x(), other.y() - cell.y())));
    }

    /** A tower to stand beside the one measured. */
    record Placement(TowerFactory.Type type, Point cell) {
    }

    /** Damage the tower on {@code cell} lands in one round of {@code lab}, with {@code line} bought on it. */
    static long damageOf(LevelDefinition lab, TowerFactory.Type type, Point cell, List<Placement> beside,
                         Optional<UpgradeNode> line, long seed) {
        GameEngine engine = labEngine(lab, seed);
        Tower subject = Turn.place(engine, type, cell).orElseThrow();
        for (Placement placement : beside) {
            Turn.place(engine, placement.type(), placement.cell());
        }
        line.ifPresent(node -> buyLine(subject, node));
        runRound(engine);
        return subject.getDamageDealt();
    }

    private static GameEngine labEngine(LevelDefinition lab, long seed) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(lab);
        engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
        engine.debugGrantCredits(GRANTED_CREDITS);
        return engine;
    }

    /** Buys {@code target} and what leads to it; a step already owned is refused at no cost. */
    private static void buyLine(Tower tower, UpgradeNode target) {
        tower.upgradeTree().pathTo(target).forEach(tower::buyUpgrade);
    }

    /** Plays the lab's one round out; the summed full health of every enemy that walked in, in points. */
    private static long runRound(GameEngine engine) {
        RunRecorder recorder = new RunRecorder(engine, UNLIMITED_LIVES);
        engine.requestNextWave();
        engine.doTick(1);
        recorder.roundStarted(1);
        recorder.afterTick(1);
        for (int t = 2; t <= TICK_BUDGET && !engine.isWaveReady() && !engine.outcome().isOver(); t++) {
            engine.doTick(t);
            recorder.afterTick(t);
        }
        return recorder.healthInSoFar();
    }

    /**
     * {@code level} with its one round made of {@code kind}'s probes, on path {@code onlyPath} alone, or on every
     * path for -1.
     */
    static LevelDefinition probeLevel(LevelDefinition level, String kind, int onlyPath) {
        return withOneRound(level, PROBES_PER_PATH, PROBE_PREFIX + kind, onlyPath, LAB_RANK,
                catalogOf(level).ranked(kind).cloneAs(PROBE_PREFIX + kind,
                        definition -> definition.withHealthAndPrice(PROBE_HEALTH, 0)));
    }

    /** {@code level} with its one round a duel wave of {@code kind} at {@code rank}, each with {@code health} points. */
    static LevelDefinition duelLevel(LevelDefinition level, String kind, Rank rank, int health) {
        return withOneRound(level, DUEL_ENEMIES_PER_PATH, DUEL_PREFIX + kind, -1, rank,
                catalogOf(level).ranked(kind).cloneAs(DUEL_PREFIX + kind,
                        definition -> definition.withHealthAndPrice(Math.round(health * definition.healthDivisor()), 0)));
    }

    private static LevelDefinition withOneRound(LevelDefinition level, int count, String id, int onlyPath, Rank rank,
                                                RankedEnemy enemy) {
        WaveDefinition round = new WaveDefinition(count + " " + id, rank);
        WaveDefinition none = new WaveDefinition(NO_ENEMY, LAB_RANK);
        List<PathDefinition> paths = IntStream.range(0, level.paths().size())
                .mapToObj(i -> level.paths().get(i).withWaves(List.of(onlyPath < 0 || onlyPath == i ? round : none)))
                .toList();
        List<RankedEnemy> ladders = new ArrayList<>(level.customRankedEnemies());
        ladders.add(enemy);
        return LevelDefinition.of(level.name(), level.width(), level.height(), paths)
                .withCustomEnemies(level.customEnemies())
                .withCustomRankedEnemies(ladders)
                .withStartingLives(UNLIMITED_LIVES);
    }

    private static EnemyCatalog catalogOf(LevelDefinition level) {
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        level.customEnemies().forEach(catalog::register);
        level.customRankedEnemies().forEach(catalog::register);
        return catalog;
    }

    /**
     * Every enemy a round can send: the catalog's kinds but those only ever spawned by another enemy (eggs,
     * reinforcements, broods), unless a round of this level sends one itself.
     */
    private static List<String> probedKinds(LevelDefinition level) {
        EnemyCatalog catalog = catalogOf(level);
        Set<String> spawned = new LinkedHashSet<>();
        for (String id : catalog.ids()) {
            for (Rank rank : Rank.values()) {
                for (Ability ability : catalog.get(id, rank).abilities()) {
                    switch (ability.action()) {
                        case SpawnEnemiesAction spawn -> spawned.add(spawn.definitionId());
                        case ApplyEffectAction ignored -> {
                        }
                    }
                }
            }
        }
        Set<String> sent = new LinkedHashSet<>(kindsIn(level));
        return catalog.ids().stream().filter(id -> sent.contains(id) || !spawned.contains(id)).toList();
    }

    /** Every kind and rank the level's rounds send, as {@link #kindKey}s, in the order they first appear. */
    static List<String> ranksIn(LevelDefinition level) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(FIRST_SEED));
        engine.loadLevel(level);
        LoadedLevel loaded = engine.getGameWorld().level();
        Set<String> keys = new LinkedHashSet<>();
        for (int round = 0; round < loaded.waveCount(); round++) {
            for (Wave wave : loaded.wavesAt(round)) {
                wave.enemySet().forEach(definition -> keys.add(kindKey(definition.id(), wave.rankFor(definition))));
            }
        }
        return List.copyOf(keys);
    }

    /** The kinds the level's rounds send, in the order they first appear. */
    static List<String> kindsIn(LevelDefinition level) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(FIRST_SEED));
        engine.loadLevel(level);
        LoadedLevel loaded = engine.getGameWorld().level();
        Set<String> kinds = new LinkedHashSet<>();
        for (int round = 0; round < loaded.waveCount(); round++) {
            for (Wave wave : loaded.wavesAt(round)) {
                wave.enemySet().stream().map(EnemyDefinition::id).forEach(kinds::add);
            }
        }
        return List.copyOf(kinds);
    }
}
