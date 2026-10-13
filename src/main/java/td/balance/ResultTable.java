package td.balance;

import td.damage.DamageUnits;
import td.tower.TowerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Every number a report measured, per level, player and seed, flat enough to keep in a file and compare with a
 * later run. The lab's numbers are kept under the player {@code lab}, one value each: they repeat exactly, so any
 * change in them is the change.
 */
public final class ResultTable {

    static final String LAB = "lab";
    /** Headline numbers, shown in a comparison whether they moved or not. */
    private static final List<String> HEADLINES = List.of("won", "margin", "livesLost", "leaks");
    /** A lab number that moved by less than this share did not move. */
    private static final double LAB_TOLERANCE = 0.03;
    private static final int MOST_CHANGES = 25;
    /** Beyond the headlines, only these players' numbers are listed; the others exist to be compared with. */
    private static final List<String> REPORTED_PLAYERS = List.of(BalanceReport.COMPETENT, BalanceReport.NAIVE);

    private final Map<Key, double[]> values;

    private ResultTable(Map<Key, double[]> values) {
        this.values = values;
    }

    /** Which number: on which level, for which player, and what it is. */
    record Key(String level, String player, String metric) {
    }

    static ResultTable of(List<LevelRun> runs) {
        Map<Key, double[]> values = new LinkedHashMap<>();
        for (LevelRun run : runs) {
            String level = run.survey().level().name();
            for (String player : run.plays().keySet()) {
                List<PlayRecord> plays = run.playsOf(player);
                put(values, level, player, "won", plays, play -> play.result().won() ? 1 : 0);
                put(values, level, player, "livesLost", plays, play -> play.result().livesLost());
                put(values, level, player, "leaks", plays, play -> play.result().leaks());
                put(values, level, player, "roundsCleared", plays, play -> play.result().roundsCleared());
                int rounds = run.survey().level().paths().getFirst().waves().size();
                for (int r = 1; r <= rounds; r++) {
                    int number = r;
                    put(values, level, player, "closest.r" + r, plays, play -> roundOf(play, number)
                            .map(round -> round.pressure().closest()).orElse(Double.NaN));
                    put(values, level, player, "leaked.r" + r, plays, play -> roundOf(play, number)
                            .map(round -> (double) round.pressure().leaked()).orElse(Double.NaN));
                }
                for (TowerFactory.Type type : TowerFactory.Type.values()) {
                    put(values, level, player, "share." + type, plays, play -> damageShare(play, type));
                    put(values, level, player, "towers." + type, plays,
                            play -> play.towers().stream().filter(tower -> tower.type() == type).count());
                }
            }
            run.margins().forEach((player, margins) -> values.put(new Key(level, player, "margin"),
                    Arrays.stream(margins).asDoubleStream().toArray()));
            TowerLab lab = run.lab();
            for (TowerFactory.Type type : TowerFactory.Type.values()) {
                TowerLab.TypeProfile profile = lab.profile(type);
                if (type != TowerFactory.Type.AURA) {
                    values.put(new Key(level, LAB, "best." + type),
                            new double[]{lab.cellValue(type, lab.bestCell(type))});
                    values.put(new Key(level, LAB, "beacon." + type), new double[]{profile.beaconGain()});
                }
                profile.kindFactors().forEach((kind, factor) ->
                        values.put(new Key(level, LAB, "kind." + type + "." + kind), new double[]{factor}));
                profile.nodeFactors().forEach((node, factor) ->
                        values.put(new Key(level, LAB, "node." + type + "." + node), new double[]{factor}));
            }
        }
        return new ResultTable(values);
    }

    private static void put(Map<Key, double[]> values, String level, String player, String metric,
                            List<PlayRecord> plays, java.util.function.ToDoubleFunction<PlayRecord> value) {
        values.put(new Key(level, player, metric), plays.stream().mapToDouble(value).toArray());
    }

    private static java.util.Optional<PlayRecord.Round> roundOf(PlayRecord play, int number) {
        return play.rounds().stream().filter(round -> round.number() == number).findFirst();
    }

    private static double damageShare(PlayRecord play, TowerFactory.Type type) {
        long total = 0;
        long mine = 0;
        for (PlayRecord.Round round : play.rounds()) {
            for (Map.Entry<TowerFactory.Type, Long> entry : round.pressure().damage().entrySet()) {
                total += entry.getValue();
                if (entry.getKey() == type) {
                    mine += entry.getValue();
                }
            }
        }
        return total == 0 ? 0 : (double) mine / total;
    }

    Path save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("# level\tplayer\tmetric\tone value per seed");
        this.values.forEach((key, row) -> lines.add(key.level() + "\t" + key.player() + "\t" + key.metric() + "\t"
                + Arrays.stream(row).mapToObj(Double::toString).collect(Collectors.joining("\t"))));
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file.toAbsolutePath();
    }

    static ResultTable load(Path file) throws IOException {
        Map<Key, double[]> values = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] cells = line.split("\t");
            values.put(new Key(cells[0], cells[1], cells[2]),
                    Arrays.stream(cells, 3, cells.length).mapToDouble(Double::parseDouble).toArray());
        }
        return new ResultTable(values);
    }

    /** One number before and after, and whether the difference is more than the seeds' noise. */
    private record Change(Key key, Estimate before, Estimate after, Estimate difference) {

        boolean real() {
            return this.difference.low() > 0 || this.difference.high() < 0;
        }

        /** Real, and big enough to matter for what it measures: a near-certain tiny shift is not news. */
        boolean worthReporting() {
            return this.real() && Math.abs(this.difference.value()) >= smallestNews(this.key.metric());
        }

        double strength() {
            double half = (this.difference.high() - this.difference.low()) / 2;
            return half <= 0 ? Math.abs(this.difference.value()) * 1e6 : Math.abs(this.difference.value()) / half;
        }
    }

    /**
     * What moved from this run (before) to {@code after}: the headline numbers for every player whether they moved
     * or not, then every other number that moved by more than the seeds' noise, compared seed by seed, and every lab
     * number that moved at all.
     */
    String compareWith(ResultTable after, String name) {
        StringBuilder out = new StringBuilder("=== Compared with '" + name + "' (after - before, seed by seed) ===\n");
        List<String> levels = this.values.keySet().stream().map(Key::level).distinct()
                .filter(level -> after.values.keySet().stream().anyMatch(key -> key.level().equals(level))).toList();
        for (String level : levels) {
            out.append('\n').append(level).append('\n');
            List<Change> played = new ArrayList<>();
            List<String> labLines = new ArrayList<>();
            for (Map.Entry<Key, double[]> entry : this.values.entrySet()) {
                Key key = entry.getKey();
                double[] later = after.values.get(key);
                if (!key.level().equals(level) || later == null) {
                    continue;
                }
                if (key.player().equals(LAB)) {
                    double scale = key.metric().startsWith("best.") ? DamageUnits.PER_POINT : 1;
                    double was = entry.getValue()[0] / scale;
                    double now = later[0] / scale;
                    if (Math.abs(now - was) > LAB_TOLERANCE * Math.max(Math.abs(was), 1e-9)) {
                        labLines.add(String.format("  lab %-50s %8.2f -> %8.2f%n", key.metric(), was, now));
                    }
                } else {
                    played.add(change(key, entry.getValue(), later));
                }
            }
            out.append("  headline                          before            after             difference\n");
            for (Change change : played) {
                if (HEADLINES.contains(change.key().metric())) {
                    out.append(line(change));
                }
            }
            List<Change> others = played.stream().filter(change -> !HEADLINES.contains(change.key().metric()))
                    .filter(change -> REPORTED_PLAYERS.contains(change.key().player()))
                    .filter(Change::worthReporting).sorted(Comparator.comparingDouble(Change::strength).reversed())
                    .limit(MOST_CHANGES).toList();
            out.append(others.isEmpty() ? "  nothing else moved beyond noise\n" : "  moved beyond noise\n");
            others.forEach(change -> out.append(line(change)));
            if (!labLines.isEmpty()) {
                out.append("  lab numbers that moved\n");
                labLines.stream().limit(MOST_CHANGES).forEach(out::append);
                if (labLines.size() > MOST_CHANGES) {
                    out.append(String.format("  ... and %d more%n", labLines.size() - MOST_CHANGES));
                }
            }
        }
        return out.toString();
    }

    /** The smallest difference in {@code metric} worth a line. */
    private static double smallestNews(String metric) {
        if (metric.startsWith("closest.")) {
            return 0.1;
        }
        if (metric.startsWith("share.")) {
            return 0.03;
        }
        if (metric.startsWith("towers.") || metric.startsWith("leaked.")) {
            return 0.5;
        }
        return 0;
    }

    private static Change change(Key key, double[] before, double[] after) {
        int n = Math.min(before.length, after.length);
        double[] differences = IntStream.range(0, n).filter(i -> !Double.isNaN(before[i]) && !Double.isNaN(after[i]))
                .mapToDouble(i -> after[i] - before[i]).toArray();
        return new Change(key, Estimate.of(finite(before)), Estimate.of(finite(after)), Estimate.of(differences));
    }

    private static double[] finite(double[] values) {
        return Arrays.stream(values).filter(v -> !Double.isNaN(v)).toArray();
    }

    private static String line(Change change) {
        String label = change.key().player() + " " + change.key().metric();
        return String.format("  %s %-32s %-17s %-17s %+.2f (%+.2f..%+.2f)%n", change.real() ? "*" : " ",
                label.length() > 32 ? label.substring(0, 32) : label, change.before().format(2),
                change.after().format(2), change.difference().value(), change.difference().low(),
                change.difference().high());
    }
}
