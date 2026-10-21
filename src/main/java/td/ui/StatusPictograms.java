package td.ui;

import java.util.List;
import java.util.Map;

/** The status bar's icons, keyed by {@code StatusTerm.key()}. Each spec is {@link Pictogram#parse} input. */
final class StatusPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Wave: a flag on a pole
            Map.entry("status.wave", "S M6.4 2.8V21.4 ; F M6.4 4H19.6L15.4 8.8L19.6 13.6H6.4Z"),
            // Lives: a heart
            Map.entry("status.lives", "F M12 20.8C4.6 15.2 2.6 11.2 2.6 7.8C2.6 5 4.6 3.4 7 3.4C9.2 3.4 11 4.8 12 6.8C13 4.8 14.8 3.4 17 3.4C19.4 3.4 21.4 5 21.4 7.8C21.4 11.2 19.4 15.2 12 20.8Z"),
            // Score: a five-pointed star
            Map.entry("status.score", "F M12 2.6L14.9 8.9L21.6 9.6L16.6 14.1L18.1 20.8L12 17.4L5.9 20.8L7.4 14.1L2.4 9.6L9.1 8.9Z"),
            // Cash: a coin with a rim and a bar
            Map.entry("status.cash", "F O12 12 9.8 ; X O12 12 7.6 ; X M15 9.6C15 8 13.6 7.4 12 7.4C10.2 7.4 9 8.3 9 9.8C9 11.4 10.6 11.9 12 12.1C13.6 12.4 15.2 12.9 15.2 14.4C15.2 15.9 13.8 16.8 12 16.8C10.2 16.8 8.8 16 8.8 14.5 ; X M12 5.8V7.4 M12 16.8V18.4"));

    private StatusPictograms() {
    }
}
