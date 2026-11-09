package td.ui;

import java.util.List;
import java.util.Map;

/**
 * Enemy affixes, keyed by {@code Affix.key()}. Each spec is {@link Pictogram#parse} input. Bold and few-layered: a pip
 * over an enemy is the smallest place a pictogram is drawn.
 */
final class AffixPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Hulking: a body beside a small one
            Map.entry("affix.hulking", "F O14.6 11.4 7.6 ; F O4.6 18.8 2.4"),
            // Exalted: a crown
            Map.entry("affix.exalted", "F M2.6 19L3.8 6.6L8.6 11.6L12 4L15.4 11.6L20.2 6.6L21.4 19Z"),
            // Armored: a breastplate
            Map.entry("affix.armored", "F M4 4.4L8.2 3C9.6 5.2 14.4 5.2 15.8 3L20 4.4L19 10.4C19 15.8 16.2 19.6 12 21.2C7.8 19.6 5 15.8 5 10.4Z ; X M12 7.4V17.8"),
            // Elusive: a shot through where it stood
            Map.entry("affix.elusive", "=trait.skittish"),
            // Regenerating: health coming back
            Map.entry("affix.regenerating", "=trait.regeneration"),
            // Shielded: a shield's outline
            Map.entry("affix.shielded", "S M12 3.2L19 5.9V11C19 15.6 16 19 12 20.6C8 19 5 15.6 5 11V5.9Z"),
            // Withered: a block crumbling away
            Map.entry("affix.withered", "=trait.crumble"),
            // Commanding: a banner
            Map.entry("affix.commanding", "F M4.6 2.4H7.2V21.6H4.6Z M7.2 3.4H20.6L17 8.2L20.6 13H7.2Z"),
            // Vengeful: allies hurried on
            Map.entry("affix.vengeful", "F M1.6 4H6.4L14.4 12L6.4 20H1.6L9.6 12Z M9.6 4H14.4L22.4 12L14.4 20H9.6L17.6 12Z"),
            // Taunting: a target
            Map.entry("affix.taunting", "S O12 12 8.4 ; F O12 12 3.6"),
            // Coldblooded: an ice cube struck through
            Map.entry("affix.coldblooded", "D M12 2.8L20.2 7.4L12 12L3.8 7.4Z ; S M12 2.8L20.2 7.4V16.6L12 21.2L3.8 16.6V7.4Z M3.8 7.4L12 12L20.2 7.4 M12 12V21.2 ; X M3.4 20.6L20.6 3.4 ; S M3.4 20.6L20.6 3.4"),
            // Steadfast: a mountain
            Map.entry("affix.steadfast", "F M1.8 20.4L8.6 7.8L12.4 14.2L15.6 9.2L22.2 20.4Z"),
            // Plated: a shield in plates
            Map.entry("affix.plated", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; X M4.8 9.2H19.2 M5.6 14.6H18.4"),
            // Fireproof: a flame struck through
            Map.entry("affix.fireproof", "E M12 2C12 2 19.5 8 19.5 14.2A7.5 7.5 0 0 1 4.5 14.2C4.5 10.8 6.6 8.6 8.2 7.2C8.6 9.6 9.8 11 11 11.4C10.4 8.2 10.9 4.8 12 2Z M12 12.6C13.6 14.2 14.6 15.6 14.6 17.1A2.6 2.6 0 0 1 9.4 17.1C9.4 15.6 10.4 14.2 12 12.6Z ; X M3.4 20.6L20.6 3.4 ; S M3.4 20.6L20.6 3.4"),
            // Unstoppable: a ram's charge
            Map.entry("affix.unstoppable", "F M2.4 8.4H12.6V4L21.6 12L12.6 20V15.6H2.4Z"));

    private AffixPictograms() {
    }
}
