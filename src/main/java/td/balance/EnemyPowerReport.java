package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.damage.DamageMix;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.LeaveZoneAction;
import td.enemy.Rank;
import td.enemy.RewindAction;
import td.enemy.ShoveAlliesAction;
import td.enemy.SpawnEnemiesAction;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.WaveScript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;

/**
 * Weighs every enemy at every rank against the Walker of that rank, with no tower involved: how much of each kind of
 * answer ({@link PowerProbe}) it takes to let none of a group through a straight lane, as a multiple of what the same
 * group of Walkers takes. From that it reads each enemy's power blind (against a mix of every answer) and countered
 * (against its best answer), what it is worth beside Walkers, how much less it is held back, and the bounty that power
 * asks for. A tool run on demand, not a test: several minutes.
 * <p>
 * Usage: {@code EnemyPowerReport [--kinds walker,golem] [--ranks grunt,elite]}. The table is also written to
 * {@code target/balance/enemy-power.tsv}.
 */
public final class EnemyPowerReport {

    private static final String BASELINE = TowerLab.PLAIN;
    private static final Path STORE = Path.of("target", "balance", "enemy-power.tsv");
    /** How far above the Walker an enemy may stand before it needs a weakness. */
    private static final double BLIND_LIMIT = 1.75;
    /** The share of its blind power an enemy over the limit may keep against its best answer. */
    private static final double WEAKNESS_SHARE = 0.6;
    private static final double MIX_HEADROOM = 1.15;
    private static final long PAY_SEED = 1L;
    private static final DamageMix ALL_PHYSICAL = new DamageMix(1_000_000, 0);
    private static final DamageMix ALL_MAGIC = new DamageMix(0, 1_000_000);
    private static final double PURE_START = 32;
    private static final LevelDefinition NO_CONTENT = PowerBench.lane(
            LevelDefinition.of("None", 1, 1, List.of(PathDefinition.of(
                    List.of(new Point(0, 0), new Point(1, 0)), List.of(new WaveDefinition("e"))))), "e");

    private EnemyPowerReport() {
    }

    public static void main(String[] args) {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        Set<String> onlyKinds = option(args, "--kinds");
        Set<String> onlyRanks = option(args, "--ranks");
        List<Rank> ranks = Arrays.stream(Rank.values())
                .filter(rank -> onlyRanks.isEmpty() || onlyRanks.contains(rank.name().toLowerCase(Locale.ROOT)))
                .toList();
        Map<Rank, Baseline> baselines = new EnumMap<>(Rank.class);
        ranks.parallelStream().map(Baseline::of).toList().forEach(baseline -> baselines.put(baseline.rank(), baseline));

        List<Subject> subjects = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<LevelDefinition> contents = new ArrayList<>(List.of(NO_CONTENT));
        contents.addAll(new BuiltInLevelCatalog().levels());
        for (LevelDefinition content : contents) {
            EnemyCatalog catalog = catalogOf(content);
            for (String kind : TowerLab.probedKinds(content)) {
                if (!seen.add(kind) || !onlyKinds.isEmpty() && !onlyKinds.contains(kind)) {
                    continue;
                }
                boolean unique = catalog.ranked(kind).isUnique();
                for (Rank rank : ranks) {
                    if (!unique || rank == Rank.BOSS) {
                        subjects.add(new Subject(content, kind, rank, unique));
                    }
                }
            }
        }
        List<EnemyPower> powers = subjects.parallelStream()
                .map(subject -> EnemyPower.of(subject, baselines.get(subject.rank())))
                .toList();

        StringBuilder tsv = new StringBuilder(EnemyPower.header()).append('\n');
        for (Rank rank : ranks) {
            Baseline baseline = baselines.get(rank);
            System.out.printf(Locale.ROOT, "%n== %s  (group of %d; the Walker takes shot %.0f, bolt %.0f, blast %.0f, burn %.0f"
                            + " damage a second, and %.2f of the mix)%n", rank, groupAt(rank),
                    baseline.pure().get(PowerProbe.SHOT), baseline.pure().get(PowerProbe.BOLT),
                    baseline.pure().get(PowerProbe.BLAST), baseline.pure().get(PowerProbe.BURN), baseline.mix());
            System.out.println(EnemyPower.header().replace('\t', ' '));
            for (EnemyPower power : powers) {
                if (power.subject().rank() == rank) {
                    System.out.println(power.line(" "));
                    tsv.append(power.line("\t")).append('\n');
                }
            }
        }
        try {
            Files.createDirectories(STORE.getParent());
            Files.writeString(STORE, tsv.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> option(String[] args, String name) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals(name)) {
                return Set.of(args[i + 1].split(","));
            }
        }
        return Set.of();
    }

    private static EnemyCatalog catalogOf(LevelDefinition content) {
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        content.customEnemies().forEach(catalog::register);
        content.customRankedEnemies().forEach(catalog::register);
        return catalog;
    }

    /** How many of an enemy walk together at {@code rank}: the higher ranks come few at a time. */
    static int groupAt(Rank rank) {
        return switch (rank) {
            case GRUNT, SOLDIER, VETERAN -> 6;
            case ELITE -> 3;
            case BOSS -> 1;
        };
    }

    private static String group(String kind, Rank rank) {
        int count = groupAt(rank);
        return WaveScript.headed(rank, count == 1 ? kind : "column " + count + " " + kind);
    }

    /** Half the group of Walkers, the enemy, and the rest of the Walkers: the enemy in company. */
    private static String escorted(String kind, Rank rank) {
        return WaveScript.headed(rank, "3 " + BASELINE + " " + kind + " 2 " + BASELINE);
    }

    /** An enemy at a rank, and the level whose content it comes from. */
    record Subject(LevelDefinition content, String kind, Rank rank, boolean unique) {

        EnemyDefinition definition() {
            return catalogOf(this.content).get(this.kind, this.rank);
        }
    }

    /**
     * What the Walker takes at a rank: the measure every other enemy of that rank is divided by.
     *
     * @param pure    each probe alone, in damage a second
     * @param mix     the scale of {@link #mixAt} it takes
     * @param company what six Walkers a slot apart take of the mix
     * @param held    how many times as long it walks the lane held back as free
     */
    record Baseline(Rank rank, Map<PowerProbe, Double> pure, double mix, double company, double held) {

        static Baseline of(Rank rank) {
            LevelDefinition lane = PowerBench.lane(NO_CONTENT, group(BASELINE, rank));
            Map<PowerProbe, Double> pure = new EnumMap<>(PowerProbe.class);
            for (PowerProbe probe : PowerProbe.values()) {
                pure.put(probe, PowerBench.threshold(lane, scale -> ProbeRig.Loadout.only(probe, scale), PURE_START,
                        DamageMix.none()));
            }
            Baseline unmixed = new Baseline(rank, pure, 1, 1, 1);
            double mix = PowerBench.threshold(lane, unmixed.mixAt(), 1, DamageMix.none());
            double company = PowerBench.threshold(PowerBench.lane(NO_CONTENT, WaveScript.headed(rank, "6 " + BASELINE)),
                    unmixed.mixAt(), 1, DamageMix.none());
            return new Baseline(rank, pure, mix, company, heldFactor(NO_CONTENT, BASELINE, rank));
        }

        /**
         * A quarter of what the Walker takes of each probe, with a chill on everything, times the scale: at a scale
         * near one it just holds the Walkers.
         */
        DoubleFunction<ProbeRig.Loadout> mixAt() {
            ProbeRig.Loadout unit = ProbeRig.Loadout.none().holding(ProbeRig.Hold.CHILL);
            for (PowerProbe probe : PowerProbe.values()) {
                unit = unit.with(probe, this.pure.get(probe) / PowerProbe.values().length);
            }
            ProbeRig.Loadout mix = unit;
            return mix::scaledBy;
        }
    }

    /** How many times as long one {@code kind} walks the lane under a chill and a freeze now and then as free. */
    private static double heldFactor(LevelDefinition content, String kind, Rank rank) {
        LevelDefinition lane = PowerBench.lane(content, WaveScript.headed(rank, kind));
        PowerBench.Outcome free = PowerBench.run(lane, ProbeRig.Loadout.none(), DamageMix.none(), PAY_SEED);
        PowerBench.Outcome held = PowerBench.run(lane,
                ProbeRig.Loadout.none().holding(ProbeRig.Hold.CHILL_AND_FREEZE), DamageMix.none(), PAY_SEED);
        return free.leaked() && held.leaked() ? (double) held.ticks() / free.ticks() : Double.NaN;
    }

    /**
     * One enemy at one rank, as multiples of the Walker of that rank.
     *
     * @param axes      what it takes of each probe alone
     * @param seen      what it takes of the shot while something keeps it revealed
     * @param blind     what it takes of the mix
     * @param countered the least of its axes and {@code seen}: what its best answer takes
     * @param company   what five Walkers and it take of the mix, against six Walkers
     * @param unheld    how much less a chill and a freeze hold it back: above 1 it shrugs them off
     * @param oneType   how many times as much a defence of one damage type takes once the level has dealt only that
     * @param spawnPay  what its spawns paid per enemy when the mix killed everything
     */
    record EnemyPower(Subject subject, Map<PowerProbe, Double> axes, double seen, double blind, double countered, double company,
                      double unheld, double oneType, double spawnPay, int walkerBounty) {

        static EnemyPower of(Subject subject, Baseline baseline) {
            Rank rank = subject.rank();
            String kind = subject.kind();
            LevelDefinition lane = PowerBench.lane(subject.content(), group(kind, rank));
            Map<PowerProbe, Double> axes = new EnumMap<>(PowerProbe.class);
            Map<PowerProbe, Double> raw = new EnumMap<>(PowerProbe.class);
            for (PowerProbe probe : PowerProbe.values()) {
                raw.put(probe, PowerBench.threshold(lane, scale -> ProbeRig.Loadout.only(probe, scale), PURE_START,
                        DamageMix.none()));
                axes.put(probe, raw.get(probe) / baseline.pure().get(probe));
            }
            double seen = PowerBench.threshold(lane,
                    scale -> ProbeRig.Loadout.only(PowerProbe.SHOT, scale).revealing(), PURE_START, DamageMix.none())
                    / baseline.pure().get(PowerProbe.SHOT);
            double mix = PowerBench.threshold(lane, baseline.mixAt(), 1, DamageMix.none());
            double company = PowerBench.threshold(PowerBench.lane(subject.content(), escorted(kind, rank)),
                    baseline.mixAt(), 1, DamageMix.none());
            double oneType = 1;
            EnemyDefinition definition = subject.definition();
            if (!definition.traitsFor(ALL_PHYSICAL).equals(definition.traitsFor(DamageMix.none()))
                    || !definition.traitsFor(ALL_MAGIC).equals(definition.traitsFor(DamageMix.none()))) {
                double physical = PowerBench.threshold(lane, scale -> ProbeRig.Loadout.only(PowerProbe.SHOT, scale),
                        PURE_START, ALL_PHYSICAL) / raw.get(PowerProbe.SHOT);
                double magic = PowerBench.threshold(lane, scale -> ProbeRig.Loadout.only(PowerProbe.BOLT, scale),
                        PURE_START, ALL_MAGIC) / raw.get(PowerProbe.BOLT);
                oneType = Math.max(physical, magic);
            }
            int count = groupAt(rank);
            double paid = Double.isInfinite(mix) ? Double.NaN : PowerBench.run(lane,
                    baseline.mixAt().apply(mix * MIX_HEADROOM), DamageMix.none(), PAY_SEED).credits() / (double) count;
            int walkerBounty = EnemyCatalog.builtIn().get(BASELINE, rank).price();
            return new EnemyPower(subject, axes, seen, mix / baseline.mix(),
                    Math.min(seen, axes.values().stream().mapToDouble(Double::doubleValue).min().orElseThrow()),
                    company / baseline.company(), baseline.held() / heldFactor(subject.content(), kind, rank), oneType,
                    paid - definition.price(), walkerBounty);
        }

        /** The bounty its blind power asks for, its spawns' pay taken off. */
        double fairBounty() {
            double spawns = Double.isNaN(this.spawnPay) ? 0 : Math.max(0, this.spawnPay);
            return Math.max(0, this.walkerBounty * this.blind - spawns);
        }

        /**
         * Its blind power over what its health and pace alone would make it: what its defences, abilities and spawns
         * multiply it by.
         */
        double hardness() {
            EnemyDefinition walker = EnemyCatalog.builtIn().get(BASELINE, this.subject.rank());
            EnemyDefinition own = this.subject.definition();
            double bulk = (own.baseHealth() / (double) walker.baseHealth()) * (walker.baseSpeed() / own.baseSpeed());
            return this.blind / bulk;
        }

        String verdict() {
            if (this.subject.unique() || this.subject.rank() == Rank.BOSS) {
                return "estimate";
            }
            if (this.blind <= BLIND_LIMIT) {
                return "ok";
            }
            return this.countered <= WEAKNESS_SHARE * this.blind && this.countered <= BLIND_LIMIT
                    ? "ok: has an answer" : "BREACH";
        }

        private String notes() {
            List<String> notes = new ArrayList<>();
            EnemyDefinition definition = this.subject.definition();
            if (definition.disruption().isPresent()) {
                notes.add("weakens towers");
            }
            Set<String> does = new LinkedHashSet<>(notes);
            for (Ability ability : definition.abilities()) {
                switch (ability.action()) {
                    case LeaveZoneAction ignored -> does.add("leaves ground");
                    case SpawnEnemiesAction ignored -> does.add("spawns");
                    case ApplyEffectAction ignored -> {
                    }
                    case ShoveAlliesAction ignored -> {
                    }
                    case RewindAction ignored -> {
                    }
                }
            }
            return String.join(", ", does);
        }

        static String header() {
            return String.join("\t", "rank", "kind", "health", "speed", "shot", "bolt", "blast", "burn", "seen", "blind",
                    "hardness",
                    "countered", "company", "unheld", "oneType", "bounty", "fair", "verdict", "notes");
        }

        String line(String separator) {
            EnemyDefinition definition = this.subject.definition();
            return String.join(separator,
                    this.subject.rank().name().toLowerCase(Locale.ROOT),
                    this.subject.kind(),
                    String.valueOf(definition.baseHealth()),
                    number(definition.baseSpeed()),
                    number(this.axes.get(PowerProbe.SHOT)),
                    number(this.axes.get(PowerProbe.BOLT)),
                    number(this.axes.get(PowerProbe.BLAST)),
                    number(this.axes.get(PowerProbe.BURN)),
                    number(this.seen),
                    number(this.blind),
                    number(this.hardness()),
                    number(this.countered),
                    number(this.company),
                    number(this.unheld),
                    number(this.oneType),
                    String.valueOf(definition.price()),
                    number(this.fairBounty()),
                    this.verdict(),
                    this.notes());
        }

        private static String number(double value) {
            if (Double.isNaN(value)) {
                return "-";
            }
            return Double.isInfinite(value) ? "immune" : String.format(Locale.ROOT, "%.2f", value);
        }
    }
}
