package td.ui;

import java.util.Locale;

/** How info-sheet rows write numbers, so every sheet writes them alike. */
final class SheetNumbers {

    private SheetNumbers() {
    }

    /** A whole number when {@code value} is within a rounding step of one, else one decimal. */
    static String decimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    /** "+25%", "-50%", or "0%". */
    static String signedPercent(float fraction) {
        int rounded = Math.round(fraction * 100);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    /** "Boss" for {@code BOSS}. */
    static String titleCase(Enum<?> constant) {
        String name = constant.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
