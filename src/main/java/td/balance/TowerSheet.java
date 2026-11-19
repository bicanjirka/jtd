package td.balance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What the bench stored, read back: every build of every tower with what it holds, and the one number prices are made
 * of, a build's power. Power is what the build adds to the bench's partners, in Walkers, over a mix of scenarios. It
 * is read beside partners because a tower alone holds nothing of a wave it lets one enemy through, however much of it
 * it kills; beside partners every hit counts.
 */
final class TowerSheet {

    /** How much of a mixed wave each scenario stands for: plain enemies one half, defences the other. */
    private static final Map<String, Double> MIX = Map.ofEntries(Map.entry("single", 1.0), Map.entry("stream", 4.0),
            Map.entry("group", 4.0), Map.entry("boss", 2.0), Map.entry("armored", 2.5), Map.entry("plated", 2.5),
            Map.entry("resist", 1.5), Map.entry("hidden", 1.0), Map.entry("fast", 1.5), Map.entry("elusive", 1.0),
            Map.entry("tank", 2.0));
    private static final double UNLISTED = 1.0;
    /** Measured, but nothing is priced off them and no verdict counts them: their trees are not final. */
    private static final Set<String> NOT_JUDGED = Set.of("AURA");
    private static final String SEPARATOR = ":";
    private static final int FIRST_SCENARIO = 9;

    private final List<Held> builds;
    private final List<String> scenarios;
    private final Map<String, Double> partners;
    private final double rate;

    private TowerSheet(List<Held> builds, List<String> scenarios, Map<String, Double> partners) {
        this.builds = List.copyOf(builds);
        this.scenarios = List.copyOf(scenarios);
        this.partners = Map.copyOf(partners);
        List<Double> rates = new ArrayList<>();
        this.judged().forEach(tower -> rates.add(this.power(this.bare(tower)) / this.bare(tower).cost()));
        this.rate = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** The sheets the last bench run stored. */
    static TowerSheet stored() {
        return of(ReportPage.load(TowerBench.SHEET), ReportPage.load(TowerBench.SCENARIO_SHEET));
    }

    /** A sheet of {@code towers} and {@code scenarios}, each as stored: its header first, then its rows. */
    static TowerSheet of(List<List<String>> towers, List<List<String>> scenarios) {
        List<String> header = towers.getFirst();
        List<Held> builds = towers.stream().skip(1).map(row -> Held.of(header, row)).toList();
        List<String> alone = header.subList(FIRST_SCENARIO, header.size() - 1).stream()
                .filter(name -> !name.endsWith(TowerBench.BESIDE_MARK)).toList();
        int held = scenarios.getFirst().indexOf("partners");
        Map<String, Double> partners = new LinkedHashMap<>();
        scenarios.stream().skip(1).filter(row -> row.getFirst().endsWith(TowerBench.BESIDE_MARK))
                .forEach(row -> partners.put(row.getFirst(), parsed(row.get(held))));
        return new TowerSheet(builds, alone, partners);
    }

    private static double parsed(String cell) {
        if ("inf".equals(cell)) {
            return Double.POSITIVE_INFINITY;
        }
        return cell.isEmpty() || "-".equals(cell) ? 0 : Double.parseDouble(cell);
    }

    /**
     * One measured build of a tower.
     *
     * @param tier    the tier of the node it weighs, or nothing
     * @param walkers what it holds of each scenario, alone and, under the scenario's name with the beside mark, as
     *                one of a team with the partners
     */
    record Held(String tower, String name, String label, String role, String node, String nodeName, String tier,
                String versus, int cost, Map<String, Double> walkers, List<String> owned) {

        static Held of(List<String> header, List<String> row) {
            Map<String, Double> walkers = new LinkedHashMap<>();
            for (int i = FIRST_SCENARIO; i < header.size() - 1; i++) {
                walkers.put(header.get(i), parsed(row.get(i)));
            }
            String owned = row.get(header.size() - 1);
            return new Held(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), row.get(5), row.get(6), row.get(7),
                    Integer.parseInt(row.get(8)), walkers, owned.isEmpty() ? List.of() : List.of(owned.split(" ")));
        }

        /** What it holds of {@code scenario} with no other tower on the lane. */
        double alone(String scenario) {
            return this.walkers.getOrDefault(scenario, 0.0);
        }

        /** What it and the partners hold of {@code scenario} together. */
        double beside(String scenario) {
            return this.walkers.getOrDefault(scenario + TowerBench.BESIDE_MARK, 0.0);
        }

        boolean isBare() {
            return this.role.equals(TowerBench.BARE);
        }

        boolean isFull() {
            return this.role.equals(TowerBench.FULL);
        }

        /** How far it is bought: the tier a full build ends with, and one below the first tier for the bare tower. */
        int stage() {
            return this.isFull() ? Integer.parseInt(this.family().get(1)) : -1;
        }

        /** The fork side a full build took, or nothing. */
        String side() {
            return this.isFull() ? this.family().get(0) : "";
        }

        /** The cluster member a full build took in place of its cluster's first, or nothing. */
        String member() {
            return this.isFull() ? this.family().get(2) : "";
        }

        /** A full build's label taken apart: side, tier, member. */
        private List<String> family() {
            String[] parts = this.label.split(SEPARATOR);
            int at = isNumber(parts[1]) ? 1 : 2;
            return List.of(at == 1 ? "" : parts[1], parts[at], parts.length > at + 1 ? parts[at + 1] : "");
        }

        private static boolean isNumber(String text) {
            return !text.isEmpty() && text.chars().allMatch(Character::isDigit);
        }
    }

    /** The towers measured, in the order stored. */
    List<String> towers() {
        return this.builds.stream().map(Held::tower).distinct().toList();
    }

    /** The towers verdicts and prices count. */
    List<String> judged() {
        return this.towers().stream().filter(TowerSheet::isJudged).toList();
    }

    static boolean isJudged(String tower) {
        return !NOT_JUDGED.contains(tower);
    }

    /** The scenarios fought, by the name each has alone. */
    List<String> scenarios() {
        return this.scenarios;
    }

    List<Held> buildsOf(String tower) {
        return this.builds.stream().filter(build -> build.tower().equals(tower)).toList();
    }

    Optional<Held> build(String tower, String label) {
        return this.builds.stream().filter(build -> build.tower().equals(tower) && build.label().equals(label)).findFirst();
    }

    Held bare(String tower) {
        return this.buildsOf(tower).stream().filter(Held::isBare).findFirst().orElseThrow();
    }

    /** Every way {@code tower} is bought whole up to {@code stage}; the bare tower for the stage below the first tier. */
    List<Held> stage(String tower, int stage) {
        if (stage < 0) {
            return List.of(this.bare(tower));
        }
        return this.buildsOf(tower).stream().filter(build -> build.isFull() && build.stage() == stage).toList();
    }

    /** The highest tier any tower is bought to. */
    int topStage() {
        return this.builds.stream().filter(Held::isFull).mapToInt(Held::stage).max().orElse(-1);
    }

    /** The highest tier {@code tower} is bought to. */
    int topStage(String tower) {
        return this.buildsOf(tower).stream().filter(Held::isFull).mapToInt(Held::stage).max().orElse(-1);
    }

    /** The Walkers of {@code scenario} that {@code build} adds to what the partners hold without it. */
    double contribution(Held build, String scenario) {
        double without = this.partners.getOrDefault(scenario + TowerBench.BESIDE_MARK, 0.0);
        return Math.max(0, build.beside(scenario) - without);
    }

    static double weightOf(String scenario) {
        return MIX.getOrDefault(scenario, UNLISTED);
    }

    /** The Walkers of a mixed wave that {@code build} adds to a defence. */
    double power(Held build) {
        double total = 0;
        double weights = 0;
        for (String scenario : this.scenarios) {
            total += weightOf(scenario) * this.contribution(build, scenario);
            weights += weightOf(scenario);
        }
        return weights == 0 ? 0 : total / weights;
    }

    /** The going rate: the power a credit buys, as the judged towers sell it bare. */
    double rate() {
        return this.rate;
    }

    /** What {@code build}'s power costs at the going rate, in credits. */
    double worth(Held build) {
        return this.rate == 0 ? 0 : this.power(build) / this.rate;
    }
}
