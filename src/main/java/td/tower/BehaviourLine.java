package td.tower;

import td.util.TickRate;

import java.util.Locale;

/**
 * One thing a tower does beyond range, damage and rate, short enough for one row:
 * {@code "Slows"} and {@code "50%, 2 s"}.
 *
 * @param value empty when the label says everything
 */
public record BehaviourLine(BehaviourMarker marker, String label, String value) {

    /** {@code ticks} as seconds, to one decimal where it has one: {@code "1.5 s"}, {@code "2 s"}. */
    static String seconds(int ticks) {
        float seconds = ticks / TickRate.TICKS_PER_SECOND;
        boolean whole = Math.abs(seconds - Math.round(seconds)) < 0.05f;
        return (whole ? Integer.toString(Math.round(seconds)) : String.format(Locale.ROOT, "%.1f", seconds)) + " s";
    }

    /** {@code fraction} as a whole percentage: {@code "50%"}. */
    static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }
}
