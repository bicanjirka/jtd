package td.term;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * How the game writes numbers, on sheets and in texts alike: at most one decimal, two below 1, trailing zeros
 * dropped, percents whole, from ten thousand abbreviated: {@code "15%"}, {@code "1.5"}, {@code "0.25"},
 * {@code "12.3k"}, {@code "2 s"}, {@code "x1.5"}, {@code "3rd"}.
 */
public final class TermNumbers {

    private static final float ABBREVIATE_FROM = 10_000f;
    private static final float THOUSAND = 1_000f;
    private static final float MILLION = 1_000_000f;

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

    /** {@code 1.25} as {@code "1.3"}, {@code 0.25} as {@code "0.25"}, {@code 12345} as {@code "12.3k"}. */
    public static String number(float value) {
        float size = Math.abs(value);
        if (size >= MILLION) {
            return digits(value / MILLION, 1) + "M";
        }
        if (size >= ABBREVIATE_FROM) {
            return digits(value / THOUSAND, 1) + "k";
        }
        return digits(value, size < 1f ? 2 : 1);
    }

    /** {@code value} in full, rounded to a whole number: a health bar's {@code "12345"}. */
    public static String whole(float value) {
        return Long.toString(Math.round((double) value));
    }

    private static String digits(float value, int decimals) {
        BigDecimal rounded = new BigDecimal(Float.toString(value)).setScale(decimals, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        String text = rounded.signum() == 0 ? "0" : rounded.toPlainString();
        return text.equals("-0") ? "0" : text;
    }

    /** {@code -5} as {@code "-5"}, {@code 5} as {@code "+5"}. */
    public static String signed(float value) {
        String text = number(value);
        return value > 0f && !text.equals("0") ? "+" + text : text;
    }

    /** {@code seconds} with its unit: {@code "2 s"}, {@code "0.5 s"}. */
    public static String seconds(float seconds) {
        return number(seconds) + " s";
    }

    /** A multiplier: {@code "x2"}, {@code "x1.5"}. */
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
        String number = number(cells);
        return number + (number.equals("1") ? " cell" : " cells");
    }

    /** {@code "$12"}. */
    public static String cash(int amount) {
        return "$" + number(amount);
    }

    /** Title case of an upper-case constant name: {@code "Boss"} for {@code BOSS}. */
    public static String titleCase(Enum<?> constant) {
        String name = constant.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
