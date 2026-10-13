package td.balance;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * A measured quantity with how sure we are of it: the sample's value and its 95% interval. Two estimates whose
 * intervals overlap are not reported as different.
 *
 * @param value the sample mean, or the observed share for a proportion
 * @param low   the 95% interval's lower end
 * @param high  its upper end
 * @param n     how many values
 */
public record Estimate(double value, double low, double high, int n) {

    /** The z for a two-sided 95% interval. */
    static final double Z95 = 1.96;

    public static Estimate of(double[] values) {
        int n = values.length;
        if (n == 0) {
            return new Estimate(Double.NaN, Double.NaN, Double.NaN, 0);
        }
        double mean = Arrays.stream(values).average().orElse(0);
        if (n == 1) {
            return new Estimate(mean, mean, mean, 1);
        }
        double variance = Arrays.stream(values).map(v -> (v - mean) * (v - mean)).sum() / (n - 1);
        double margin = Z95 * Math.sqrt(variance / n);
        return new Estimate(mean, mean - margin, mean + margin, n);
    }

    public static <T> Estimate of(Collection<T> items, ToDoubleFunction<T> value) {
        return of(items.stream().mapToDouble(value).toArray());
    }

    /**
     * The share of {@code hits} in {@code n} trials, with the Wilson 95% interval: unlike the normal one it stays
     * inside 0 to 1 and is honest at 0% and 100%, which is where a win rate over few seeds often lands.
     */
    public static Estimate proportion(int hits, int n) {
        if (n == 0) {
            return new Estimate(Double.NaN, Double.NaN, Double.NaN, 0);
        }
        double p = (double) hits / n;
        double z2 = Z95 * Z95;
        double centre = (p + z2 / (2.0 * n)) / (1 + z2 / n);
        double half = Z95 * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / (1 + z2 / n);
        return new Estimate(p, Math.max(0, centre - half), Math.min(1, centre + half), n);
    }

    public static <T> Estimate proportion(Collection<T> items, Predicate<T> hit) {
        return proportion((int) items.stream().filter(hit).count(), items.size());
    }

    /** Whether the two intervals do not overlap: a difference the sample can stand behind. */
    public boolean differsFrom(Estimate other) {
        return this.high < other.low || other.high < this.low;
    }

    /** {@code 12.3 ±1.4}, at {@code decimals} places. */
    public String format(int decimals) {
        return String.format("%." + decimals + "f ±%." + decimals + "f", this.value, (this.high - this.low) / 2);
    }

    /** As a percentage with its interval: {@code 74% (61-84)}. */
    public String formatPercent() {
        return String.format("%3.0f%% (%.0f-%.0f)", this.value * 100, this.low * 100, this.high * 100);
    }
}
