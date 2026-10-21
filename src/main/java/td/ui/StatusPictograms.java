package td.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The status bar's icons, keyed by {@code StatusTerm.key()}, and the round skulls. Each spec is {@link Pictogram#parse} input. */
final class StatusPictograms {

    static final String SKULL = "round.skull";
    static final String SKULL_VETERAN = "round.skull.veteran";
    static final String SKULL_ELITE = "round.skull.elite";
    static final String SKULL_BOSS = "round.skull.boss";
    static final String SKULL_UNIQUE = "round.skull.unique";
    /** Appended to a skull's key: the same skull with a crack down its crown. */
    static final String CRACKED = ".cracked";

    private static final String CRANIUM = "F M12 2.6C6.9 2.6 3.6 6.3 3.6 10.9C3.6 13.6 4.8 15.6 6.8 16.6V19.4C6.8 20.3 7.5 21 "
            + "8.4 21H15.6C16.5 21 17.2 20.3 17.2 19.4V16.6C19.2 15.6 20.4 13.6 20.4 10.9C20.4 6.3 17.1 2.6 12 2.6Z";
    private static final String ROUND_EYES = "K O8.5 11.4 2.4 ; K O15.5 11.4 2.4";
    private static final String GLARING_EYES = "K M5.8 9.4L10.9 11C10.9 13 9.9 14 8.4 14C6.8 14 5.8 12.8 5.8 11.2Z ; "
            + "K M18.2 9.4L13.1 11C13.1 13 14.1 14 15.6 14C17.2 14 18.2 12.8 18.2 11.2Z";
    private static final String FACE = "K M12 14.2L10.8 16.4H13.2Z ; X M10.2 18.4V21 M13.8 18.4V21";
    private static final String HELMET = "F M1.6 9.6C2.2 9 2.8 8.6 3.2 8.2C3.6 3.8 7.4 1.2 12 1.2C16.6 1.2 20.4 3.8 20.8 8.2"
            + "C21.2 8.6 21.8 9 22.4 9.6Z ; X M2.2 9.6H21.8";
    private static final String HORNS = "F M6.2 6.2C3.6 5.4 1.8 3.4 1.8 0.8C3.2 2.6 5.2 3.4 7.8 3.6Z ; "
            + "F M17.8 6.2C20.4 5.4 22.2 3.4 22.2 0.8C20.8 2.6 18.8 3.4 16.2 3.6Z";
    private static final String CROWN = "F M4.6 7.8L3.8 1.4L8 4.6L12 0.6L16 4.6L20.2 1.4L19.4 7.8Z ; X M4.2 8.1H19.8";
    private static final String SMALL_CROWN = "F M6.8 7.8L6.2 1.8L9.4 4.4L12 0.6L14.6 4.4L17.8 1.8L17.2 7.8Z ; "
            + "X M6.4 8.1H17.6 ; K O12 5.4 1.1";
    private static final String CRACK = "X M13.2 2.2L11.2 5.6L13.4 7.4L11.6 9.8";

    static final List<Map.Entry<String, String>> SPECS = specs();

    private StatusPictograms() {
    }

    private static List<Map.Entry<String, String>> specs() {
        List<Map.Entry<String, String>> specs = new ArrayList<>(List.of(
                // Wave: a flag on a pole
                Map.entry("status.wave", "S M6.4 2.8V21.4 ; F M6.4 4H19.6L15.4 8.8L19.6 13.6H6.4Z"),
                // Lives: a heart
                Map.entry("status.lives", "F M12 20.8C4.6 15.2 2.6 11.2 2.6 7.8C2.6 5 4.6 3.4 7 3.4C9.2 3.4 11 4.8 12 6.8C13 4.8 14.8 3.4 17 3.4C19.4 3.4 21.4 5 21.4 7.8C21.4 11.2 19.4 15.2 12 20.8Z"),
                // Score: a five-pointed star
                Map.entry("status.score", "F M12 2.6L14.9 8.9L21.6 9.6L16.6 14.1L18.1 20.8L12 17.4L5.9 20.8L7.4 14.1L2.4 9.6L9.1 8.9Z"),
                // Cash: a coin with a rim and a bar
                Map.entry("status.cash", "F O12 12 9.8 ; X O12 12 7.6 ; X M15 9.6C15 8 13.6 7.4 12 7.4C10.2 7.4 9 8.3 9 9.8C9 11.4 10.6 11.9 12 12.1C13.6 12.4 15.2 12.9 15.2 14.4C15.2 15.9 13.8 16.8 12 16.8C10.2 16.8 8.8 16 8.8 14.5 ; X M12 5.8V7.4 M12 16.8V18.4")));
        // A bare skull; a helmet; horns and a glare; a crown and a glare; horns, a jewelled crown and a glare
        skull(specs, SKULL, CRANIUM + " ; " + ROUND_EYES);
        skull(specs, SKULL_VETERAN, CRANIUM + " ; " + HELMET + " ; " + ROUND_EYES);
        skull(specs, SKULL_ELITE, HORNS + " ; " + CRANIUM + " ; " + GLARING_EYES);
        skull(specs, SKULL_BOSS, CRANIUM + " ; " + CROWN + " ; " + GLARING_EYES);
        skull(specs, SKULL_UNIQUE, HORNS + " ; " + CRANIUM + " ; " + SMALL_CROWN + " ; " + GLARING_EYES);
        return List.copyOf(specs);
    }

    private static void skull(List<Map.Entry<String, String>> specs, String key, String head) {
        String whole = head + " ; " + FACE;
        specs.add(Map.entry(key, whole));
        specs.add(Map.entry(key + CRACKED, whole + " ; " + CRACK));
    }
}
