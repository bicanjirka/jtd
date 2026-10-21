package td.term;

import java.math.BigDecimal;
import java.util.Locale;

/** How texts write numbers: {@code "15%"}, {@code "1.5"}, {@code "2 s"}, {@code "x1.5"}, {@code "3rd"}. */
public final class TermNumbers {

    private TermNumbers() {
    }

    /** {@code 0.15} as {@code "15%"}. */
    public static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    /** {@code 0.15} as {@code "+15%"}, {@code -0.4} as {@code "-40%"}. */
    public static String signedPercent(float fraction) {
        int rounded = Math.round(fraction * 100);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    /** {@code value} to two decimals without trailing zeros: {@code "1.5"}, {@code "4"}. */
    public static String number(float value) {
        return new BigDecimal(String.format(Locale.ROOT, "%.2f", value)).stripTrailingZeros().toPlainString();
    }

    /** {@code -5} as {@code "-5"}, {@code 5} as {@code "+5"}. */
    public static String signed(float value) {
        return (value > 0f ? "+" : "") + number(value);
    }

    /** {@code seconds} with its unit: {@code "2 s"}, {@code "0.5 s"}. */
    public static String seconds(float seconds) {
        return number(seconds) + " s";
    }

    /** A multiple: {@code "x2"}, {@code "x1.5"}. */
    public static String times(float factor) {
        return "x" + number(factor);
    }

    /** {@code "1st"}, {@code "2nd"}, {@code "3rd"}, {@code "11th"}. */
    public static String ordinal(int n) {
        int lastTwo = n % 100;
        if (lastTwo >= 11 && lastTwo <= 13) {
            return n + "th";
        }
        return n + switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /** A distance in cells with its unit: {@code "1 cell"}, {@code "1.5 cells"}. */
    public static String cells(float cells) {
        return number(cells) + (cells == 1f ? " cell" : " cells");
    }
}
