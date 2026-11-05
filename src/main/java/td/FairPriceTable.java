package td;

import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeTier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What an upgrade should cost: the price at which a credit spent on it saves as many lives as a credit spent on another
 * tower, the choice a player weighs it against.
 */
final class FairPriceTable {

    private static final double PERCENT = 100.0;

    private final double livesPerCredit;

    FairPriceTable(double livesPerCredit) {
        this.livesPerCredit = livesPerCredit;
    }

    /** One more tower as measured: what it cost and the lives it saved. */
    record MeasuredCopy(int cost, double saved) {
    }

    /**
     * A node with everything that leads to it, as measured.
     *
     * @param tier  the tier of the node the line ends in
     * @param steps the ids of the nodes in the line, its own included
     * @param saved the lives the whole line saved
     */
    record MeasuredLine(TowerFactory.Type tower, UpgradeTier tier, Set<String> steps, double saved) {
    }

    /** The lives a credit saves on another tower, over every type; empty when another tower saves none. */
    static OptionalDouble livesPerCredit(List<MeasuredCopy> copies) {
        double saved = copies.stream().mapToDouble(MeasuredCopy::saved).sum();
        int cost = copies.stream().mapToInt(MeasuredCopy::cost).sum();
        return saved > 0 && cost > 0 ? OptionalDouble.of(saved / cost) : OptionalDouble.empty();
    }

    /** The fair price of a whole line that saves {@code saved} lives. */
    double linePrice(double saved) {
        return saved / this.livesPerCredit;
    }

    /**
     * For each tier, the mean fair price of a node at that tier, as a percent of its tower's list price. Unfloored: a
     * negative price is noise around nothing, and averaging cancels it.
     */
    Map<UpgradeTier, Double> tierPercents(List<MeasuredLine> lines) {
        return lines.stream().collect(Collectors.groupingBy(MeasuredLine::tier, () -> new EnumMap<>(UpgradeTier.class),
                Collectors.averagingDouble(line -> this.stepPrice(line, lines) / line.tower().priceFor(0) * PERCENT)));
    }

    /** What {@code line} saves beyond the most any part of it saves alone, priced: what its own node adds. */
    private double stepPrice(MeasuredLine line, List<MeasuredLine> lines) {
        double before = lines.stream()
                .filter(other -> other.tower() == line.tower() && other != line && line.steps().containsAll(other.steps()))
                .mapToDouble(MeasuredLine::saved).max().orElse(0);
        return this.linePrice(line.saved() - before);
    }
}
