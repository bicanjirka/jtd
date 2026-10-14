package td.tower.upgrade;

import td.util.TickRate;

import java.math.BigDecimal;
import java.util.Locale;

/** How every perk writes its numbers, so they read the same across towers. */
public final class PerkText {

    private PerkText() {
    }

    /** {@code fraction} as a whole percentage: {@code "25%"}. */
    public static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    /** {@code ticks} as seconds, to two decimals where it has them: {@code "2s"}, {@code "0.5s"}. */
    public static String seconds(int ticks) {
        return number(ticks / TickRate.TICKS_PER_SECOND) + "s";
    }

    /** A distance in cells: {@code "1.5"}, {@code "2"}. */
    public static String cells(float cells) {
        return number(cells);
    }

    /** A distance with its unit: {@code "1 cell"}, {@code "1.5 cells"}. */
    public static String distance(float cells) {
        return number(cells) + (cells == 1f ? " cell" : " cells");
    }

    /** A multiple: {@code "x4"}, {@code "x1.5"}. */
    public static String times(float factor) {
        return "x" + number(factor);
    }

    /** {@code "1st"}, {@code "2nd"}, {@code "3rd"}, {@code "4th"}, {@code "11th"}. */
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

    /** {@code value} to two decimals, without trailing zeros: {@code "1.5"}, {@code "0.25"}, {@code "4"}. */
    private static String number(float value) {
        return new BigDecimal(String.format(Locale.ROOT, "%.2f", value)).stripTrailingZeros().toPlainString();
    }
}
