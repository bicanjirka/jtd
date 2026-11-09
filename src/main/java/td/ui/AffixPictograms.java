package td.ui;

import java.util.List;
import java.util.Map;

/**
 * Enemy affixes, keyed by {@code Affix.key()}. Each spec is {@link Pictogram#parse} input. Bold and few-layered: a pip
 * over an enemy is the smallest place a pictogram is drawn.
 */
final class AffixPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Hulking: broad shoulders
            Map.entry("affix.hulking", "F M2.6 5.4H21.4L18.2 20.6H5.8Z"),
            // Exalted: a crown
            Map.entry("affix.exalted", "F M2.6 19L3.8 6.6L8.6 11.6L12 4L15.4 11.6L20.2 6.6L21.4 19Z"),
            // Armored: a plate
            Map.entry("affix.armored", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; K M7 10H17V12.6H7Z"),
            // Elusive: a dodging line
            Map.entry("affix.elusive", "S M2.8 19L8.6 9.4L13.6 15.4L19 6.4 ; F M22 3.4L15.8 5.2L20.4 9.4Z"),
            // Regenerating: a plus
            Map.entry("affix.regenerating", "F M8.8 2.8H15.2V8.8H21.2V15.2H15.2V21.2H8.8V15.2H2.8V8.8H8.8Z"),
            // Shielded: a shield's outline
            Map.entry("affix.shielded", "S M12 3.2L19 5.9V11C19 15.6 16 19 12 20.6C8 19 5 15.6 5 11V5.9Z"),
            // Withered: a cracked drop
            Map.entry("affix.withered", "F M12 2.6C16.4 7.4 18.6 10.6 18.6 14.4A6.6 6.6 0 0 1 5.4 14.4C5.4 10.6 7.6 7.4 12 2.6Z ; X M12.6 7.6L10.4 12.6L13.6 14.2L11.4 19.4"),
            // Commanding: a banner
            Map.entry("affix.commanding", "F M4.6 2.4H7.2V21.6H4.6Z M7.2 3.4H20.6L17 8.2L20.6 13H7.2Z"),
            // Vengeful: allies hurried on
            Map.entry("affix.vengeful", "F M1.6 4H6.4L14.4 12L6.4 20H1.6L9.6 12Z M9.6 4H14.4L22.4 12L14.4 20H9.6L17.6 12Z"),
            // Taunting: a target
            Map.entry("affix.taunting", "S O12 12 8.4 ; F O12 12 3.6"),
            // Coldblooded: a snowflake
            Map.entry("affix.coldblooded", "S M12 2.6V21.4 M3.9 7.3L20.1 16.7 M3.9 16.7L20.1 7.3"));

    private AffixPictograms() {
    }
}
