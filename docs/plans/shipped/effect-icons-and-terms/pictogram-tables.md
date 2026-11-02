### `src/main/java/td/ui/EffectPictograms.java`

```java
package td.ui;

import java.util.List;
import java.util.Map;

/** The effect kinds, keyed by {@code EffectKind.key()}. Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */
final class EffectPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Chill
            Map.entry("effect.chill", "S M12 2.5V21.5 M3.77 7.25L20.23 16.75 M3.77 16.75L20.23 7.25 ; T M10.16 3.66L12 5.5L13.84 3.66 M10.16 20.34L12 18.5L13.84 20.34 M18.3 17.76L17.63 15.25L20.14 14.58 M5.7 6.24L6.37 8.75L3.86 9.42 M18.3 6.24L17.63 8.75L20.14 9.42 M3.86 14.58L6.37 15.25L5.7 17.76"),
            // Burn
            Map.entry("effect.burn", "E M12 2C12 2 19.5 8 19.5 14.2A7.5 7.5 0 0 1 4.5 14.2C4.5 10.8 6.6 8.6 8.2 7.2C8.6 9.6 9.8 11 11 11.4C10.4 8.2 10.9 4.8 12 2Z M12 12.6C13.6 14.2 14.6 15.6 14.6 17.1A2.6 2.6 0 0 1 9.4 17.1C9.4 15.6 10.4 14.2 12 12.6Z"),
            // Freeze
            Map.entry("effect.freeze", "D M12 2.8L20.2 7.4L12 12L3.8 7.4Z ; S M12 2.8L20.2 7.4V16.6L12 21.2L3.8 16.6V7.4Z M3.8 7.4L12 12L20.2 7.4 M12 12V21.2 ; T M6.4 12.6V15.4 M8.6 13.8V16.6"),
            // Shield
            Map.entry("effect.shield", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; K M12 5.2L17.6 7.3V11C17.6 14.6 15.4 17.6 12 19Z ; D M12 5.2L17.6 7.3V11C17.6 14.6 15.4 17.6 12 19Z"),
            // Invisibility
            Map.entry("effect.invisible", "S M3 10.5C7 15.5 17 15.5 21 10.5 ; S M6 13.6L4.4 16.2 M12 15.2V18.3 M18 13.6L19.6 16.2"),
            // Heal
            Map.entry("effect.heal", "F M9.5 3H14.5V9.5H21V14.5H14.5V21H9.5V14.5H3V9.5H9.5Z"),
            // Vulnerable
            Map.entry("effect.vulnerable", "F M12 20.8C5 15.8 2.5 11.6 2.5 8.4A4.9 4.9 0 0 1 12 6.6A4.9 4.9 0 0 1 21.5 8.4C21.5 11.6 19 15.8 12 20.8Z ; X M12.2 6.4L10.4 10.4L13.6 13L11.4 17.4"),
            // Reveal
            Map.entry("effect.revealed", "S M2.5 12C6 6.2 18 6.2 21.5 12C18 17.8 6 17.8 2.5 12Z ; F O12 12 3.2"),
            // Poison
            Map.entry("effect.poison", "S M9.5 2.8H14.5 M10.5 2.8V9L5.2 17.6C4.2 19.4 5.4 21.2 7.4 21.2H16.6C18.6 21.2 19.8 19.4 18.8 17.6L13.5 9V2.8 ; F M7.6 15H16.4L18.8 18.4C19.4 19.8 18.4 21.2 16.6 21.2H7.4C5.6 21.2 4.6 19.8 5.2 18.4Z ; F O10.5 11.8 1.1 O13.4 9.4 0.8"),
            // Scorch
            Map.entry("effect.scorched", "S M6.5 21C4.5 17.5 8.5 15.5 6.5 12C5.5 10.2 6 8 7.5 6.5 M12 21C10 17.5 14 15 12 11C11 9 11.5 6 13 3.5 M17.5 21C15.5 17.5 19.5 15.5 17.5 12C16.5 10.2 17 8 18.5 6.5"),
            // Sicken
            Map.entry("effect.sickened", "F O8 16.5 4.5 ; F O15.5 9.5 3.2 ; F O18.3 17.6 2.1 ; F O10.4 5.2 1.5 ; K O6.6 15 1.2 ; K O14.6 8.6 0.8"),
            // Sunder
            Map.entry("effect.sundered", "F M10.6 2.9L3.8 5.6V11C3.8 15.6 6.6 19.3 10.6 21L9.2 15.4L11.2 11.4L9.6 7.3Z ; F M13.4 3.6L20.2 6.3V11.7C20.2 16.3 17.4 20 13.4 21.7L12.4 15.9L14.4 11.9L12.8 7.8Z"),
            // Expose
            Map.entry("effect.exposed", "S O12 12 9 ; F M12 5.2L13.6 10.4L18.8 12L13.6 13.6L12 18.8L10.4 13.6L5.2 12L10.4 10.4Z"),
            // Mark
            Map.entry("effect.marked", "E M12 21.8C12 21.8 4.6 14.4 4.6 9.2A7.4 7.4 0 0 1 19.4 9.2C19.4 14.4 12 21.8 12 21.8Z O12 9.2 2.7"),
            // Priority
            Map.entry("effect.priority", "S O12 12 6.8 ; S M12 2.2V6.6 M12 17.4V21.8 M2.2 12H6.6 M17.4 12H21.8 ; F O12 12 2"),
            // Resonance
            Map.entry("effect.resonating", "S M7.5 3V10.5A4.5 4.5 0 0 0 16.5 10.5V3 M12 15V21.5 ; T M19.6 7.2A6 6 0 0 1 19.6 13.8 M4.4 7.2A6 6 0 0 0 4.4 13.8"),
            // Fracture
            Map.entry("effect.fractured", "F@-40 O4.6 9.9 2.4 O4.6 14.1 2.4 M4.6 10.4H11.4L10.6 11.4L11.6 12.2L10.8 13.6H4.6Z ; F@-40 O19.4 9.9 2.4 O19.4 14.1 2.4 M19.4 10.4H13L12.2 11.4L13.2 12.2L12.4 13.6H19.4Z"),
            // Daze
            Map.entry("effect.dazed", "T M3 13A9 4.2 0 1 0 21 13A9 4.2 0 1 0 3 13Z ; F M6 8.3L6.9 10.6L9.2 11.5L6.9 12.4L6 14.7L5.1 12.4L2.8 11.5L5.1 10.6Z ; F M13 3.6L14.1 6.4L16.9 7.5L14.1 8.6L13 11.4L11.9 8.6L9.1 7.5L11.9 6.4Z ; F M18.6 14L19.4 15.9L21.3 16.7L19.4 17.5L18.6 19.4L17.8 17.5L15.9 16.7L17.8 15.9Z"),
            // Saturation
            Map.entry("effect.saturated", "F O12 12 3.2 ; S M5.6 9.2A7 7 0 0 1 9.2 5.6 M18.4 14.8A7 7 0 0 1 14.8 18.4 ; T M2.6 8.6A10 10 0 0 1 8.6 2.6 M21.4 15.4A10 10 0 0 1 15.4 21.4 M5.6 14.8A7 7 0 0 0 9.2 18.4 M18.4 9.2A7 7 0 0 0 14.8 5.6"),
            // Charge
            Map.entry("effect.charged", "F M14 2L5 13.6H11.2L9.6 22L19 9.8H12.8Z"),
            // Doom
            Map.entry("effect.doom", "F M12 2.6C7.2 2.6 4 6.2 4 10.4C4 13.2 5.4 15.2 7.4 16.2V19.6C7.4 20.4 8 21.2 9 21.2H15C16 21.2 16.6 20.4 16.6 19.6V16.2C18.6 15.2 20 13.2 20 10.4C20 6.2 16.8 2.6 12 2.6Z ; K O8.9 11 2.1 ; K O15.1 11 2.1 ; K M12 13.6L13.3 16H10.7Z ; X M10.6 18.4V21.2 M13.4 18.4V21.2"),
            // Blight
            Map.entry("effect.blight", "F M4.5 19.5C4.5 10 10 4 20 3.5C20 13.5 14 19.5 4.5 19.5Z ; X M4.5 19.5L15.2 8.8 ; K O15.2 13.4 1.5 ; K O10.4 16.2 1.1 ; K O16.6 7.2 0.9"),
            // Contagion
            Map.entry("effect.contagion", "F O12 12 4.6 ; S M12 7V3.6 M12 17V20.4 M7.67 9.5L4.72 7.8 M16.33 14.5L19.28 16.2 M7.67 14.5L4.72 16.2 M16.33 9.5L19.28 7.8 ; F O12 3.2 1.7 O12 20.8 1.7 O4.38 7.6 1.7 O19.62 16.4 1.7 O4.38 16.4 1.7 O19.62 7.6 1.7"),
            // Rime
            Map.entry("effect.rime", "F M3 3.5H21V6.4H3Z ; F M4.4 6.4L6.6 14.4L8.8 6.4Z M9.8 6.4L12 20.6L14.2 6.4Z M15.2 6.4L17.4 16.2L19.6 6.4Z"),
            // Ash
            Map.entry("effect.ash", "F M2.8 20.6C4.6 15.6 8 13.6 12 13.6C16 13.6 19.4 15.6 21.2 20.6Z ; F O8.2 9 1.4 ; F O12.8 5.4 1.1 ; F O16.6 10.2 1.5 ; F O11.4 10.8 0.8"),
            // Inversion
            Map.entry("effect.inversion", "S M18.6 9.4A7.2 7.2 0 0 0 5.6 8.2 M5.4 14.6A7.2 7.2 0 0 0 18.4 15.8 ; F M3.4 5.4L5.2 11L9.6 7.4Z M20.6 18.6L18.8 13L14.4 16.6Z"),
            // Sympathy
            Map.entry("effect.sympathy", "S O8.6 12 5.6 ; S O15.4 12 5.6"),
            // Reckoning
            Map.entry("effect.reckoning", "S M12 4V20.5 M7.5 20.5H16.5 M4 7H20 ; T M4 7L2 13 M4 7L6 13 M20 7L18 13 M20 7L22 13 ; F M1.4 13H6.6A2.6 2.4 0 0 1 1.4 13Z M17.4 13H22.6A2.6 2.4 0 0 1 17.4 13Z ; F O12 4 1.6"),
            // Silence
            Map.entry("effect.silenced", "S M4.5 4.5H19.5A1.5 1.5 0 0 1 21 6V14.5A1.5 1.5 0 0 1 19.5 16H11.5L7 20V16H4.5A1.5 1.5 0 0 1 3 14.5V6A1.5 1.5 0 0 1 4.5 4.5Z ; F M7 9H17V11.6H7Z"),
            // Anchor
            Map.entry("effect.anchored", "S O12 4.4 2 ; S M12 6.4V20.6 M8.4 9.6H15.6 M4.8 14C5.6 18.2 8.8 20.6 12 20.6C15.2 20.6 18.4 18.2 19.2 14 ; F M2.6 15.6L4.6 12L7 15.2Z M21.4 15.6L19.4 12L17 15.2Z"),
            // Unravel
            Map.entry("effect.unraveled", "S O12 10 7 ; T M5.6 7C8.8 9.4 14.6 9.6 18.6 7.2 M5.2 12.4C9 14.2 14.8 14 18.8 11.8 M8.8 3.8C10.6 7.6 11 12.6 9.6 16.6 ; S M7.6 15.4C5.8 18.4 8.6 20.8 12 20.2C15.2 19.6 17.4 21.4 21.2 20.6"),
            // Brittle
            Map.entry("effect.brittle", "F M12 2L17.4 9.8L13.2 22L6.6 10.6Z ; X M12.2 5.2L10.6 9.8L13.4 12.8L11.6 17.2"),
            // Toll
            Map.entry("effect.toll", "F M12 3.2C8.4 3.2 6.2 6.2 6.2 9.6V14L4 16.8V17.6H20V16.8L17.8 14V9.6C17.8 6.2 15.6 3.2 12 3.2Z ; F O12 19.6 2.2 ; S M12 3.2V1.8"),
            // Corrode
            Map.entry("effect.corroded", "E M3.5 4.5H20.5V13H3.5Z M7 7.4A1.6 1.6 0 1 0 7.01 7.4Z M13.2 9.2A2.2 1.8 0 1 0 13.21 9.2Z M17.2 6.8A1.2 1.2 0 1 0 17.21 6.8Z ; F M9.4 13V15.8A1.6 1.6 0 0 0 12.6 15.8V13Z ; F M15.2 18.2C15.2 18.2 17 20 17 21A1.8 1.8 0 0 1 13.4 21C13.4 20 15.2 18.2 15.2 18.2Z"),
            // Drag
            Map.entry("effect.dragged", "S M8.6 9.6V7.6A3.4 3.4 0 0 1 15.4 7.6V9.6 ; F M12 9C16.4 9 19.4 12.4 19.4 16.2C19.4 18.4 18.2 20.6 16.2 20.6H7.8C5.8 20.6 4.6 18.4 4.6 16.2C4.6 12.4 7.6 9 12 9Z"),
            // Undertow
            Map.entry("effect.undertow", "F M2.4 20.6C2.4 12 7.4 5.6 13.6 5.6C18.2 5.6 21 8.8 21 12C21 14.6 19.2 16.6 16.8 16.6C14.8 16.6 13.6 15.2 13.6 13.6C13.6 12.2 14.6 11 16.2 11.2C14.4 9.6 9.8 10.8 8.6 20.6Z ; T M2.4 22.2H21.6"),
            // Condemn
            Map.entry("effect.condemned", "F@-40 M7 3.4H17V9.4H7Z M10.9 9.4H13.1V18H10.9Z ; F M4 19.4H14V22H4Z"),
            // Still
            Map.entry("effect.stilled", "S O12 12 9 ; F M8.4 7.4H10.9V16.6H8.4Z M13.1 7.4H15.6V16.6H13.1Z"),
            // Unveil
            Map.entry("effect.unveiled", "S M3 14.6C6.6 9.6 17.4 9.6 21 14.6C17.4 19.6 6.6 19.6 3 14.6Z ; F O12 14.6 2.6 ; S M12 2.6V6 M5.4 4.6L7.2 7.6 M18.6 4.6L16.8 7.6"),
            // Dead Zone
            Map.entry("effect.dead_zone", "S O12 12 9.2 ; S M3.6 12.6H7.4L9 8.6L11 16L12.6 12.6H20.4"),
            // Fallout
            Map.entry("effect.fallout", "F O12 12 2 ; F M7.25 3.77A9.5 9.5 0 0 1 16.75 3.77L13.75 8.97A3.5 3.5 0 0 0 10.25 8.97Z ; F@120 M7.25 3.77A9.5 9.5 0 0 1 16.75 3.77L13.75 8.97A3.5 3.5 0 0 0 10.25 8.97Z ; F@240 M7.25 3.77A9.5 9.5 0 0 1 16.75 3.77L13.75 8.97A3.5 3.5 0 0 0 10.25 8.97Z"),
            // Frail
            Map.entry("effect.frail", "S M5.6 3H18.4C18.4 8.6 15.6 12.2 12 12.2C8.4 12.2 5.6 8.6 5.6 3Z M12 12.2V19.6 M7.6 20.6H16.4 ; T M12.4 3.4L10.8 6L13 7.8L11.6 10.4"),
            // Crack
            Map.entry("effect.cracked", "F M5 4H19A1.5 1.5 0 0 1 20.5 5.5V18.5A1.5 1.5 0 0 1 19 20H5A1.5 1.5 0 0 1 3.5 18.5V5.5A1.5 1.5 0 0 1 5 4Z ; K O6.6 7.1 1 O17.4 7.1 1 O6.6 16.9 1 O17.4 16.9 1 ; X M12.4 4L10.4 9L13.8 12.2L11 16.6L12.2 20"),
            // Tar
            Map.entry("effect.tarred", "F M12 5.4C14 5.4 14.6 7.6 16.4 7.4C18.6 7.2 20.4 8.8 19.4 11C18.8 12.4 20.6 13.4 20 15.4C19.4 17.4 17.2 17 16 18.2C14.6 19.8 13 20.4 11.6 19.4C10.2 18.4 8.4 19.8 6.8 18.4C5.2 17 6.6 15.2 5.4 14C4 12.6 4.4 10.4 6.4 9.8C8 9.4 7.8 7.4 9.4 6.4C10.2 5.8 11 5.4 12 5.4Z ; F O4 5 1.6 O20.6 4.6 1.2 O21.2 19.8 1.4 ; K O13.6 11.4 1.4"),
            // Bleed
            Map.entry("effect.bleeding", "F M9.6 2.6C9.6 2.6 15.8 9.8 15.8 14.2A6.2 6.2 0 0 1 3.4 14.2C3.4 9.8 9.6 2.6 9.6 2.6Z ; F M18.2 11.4C18.2 11.4 21.4 15 21.4 17.2A3.2 3.2 0 0 1 15 17.2C15 15 18.2 11.4 18.2 11.4Z"),
            // Soulfire
            Map.entry("effect.soulfire", "S M12 2.6C12 2.6 19 8.4 19 14.4A7 7 0 0 1 5 14.4C5 11 7 8.9 8.5 7.6C9 9.8 10 11 11.2 11.4C10.6 8.4 11 5 12 2.6Z ; F O12 15.6 2.6"),
            // Sprint
            Map.entry("effect.sprinting", "F M8.4 5.6L14.8 12L8.4 18.4H4.6L11 12L4.6 5.6Z M15 5.6L21.4 12L15 18.4H11.2L17.6 12L11.2 5.6Z"));

    private EffectPictograms() {
    }
}
```

### `src/main/java/td/ui/TowerTermPictograms.java`

```java
package td.ui;

import java.util.List;
import java.util.Map;

/** Tower buffs, debuffs and named mechanics, keyed by {@code TowerTerm.key()}. Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */
final class TowerTermPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Steady Aim
            Map.entry("tower.steady_aim", "S O12 12 8.4 ; T M12 3.6V8.4 M12 15.6V20.4 M3.6 12H8.4 M15.6 12H20.4 ; F O12 12 1.7"),
            // Crit Streak
            Map.entry("tower.crit_streak", "F M9.5 2.6L11.2 8.3L16.9 10L11.2 11.7L9.5 17.4L7.8 11.7L2.1 10L7.8 8.3Z ; F M17.2 12.4L18.3 15.6L21.5 16.7L18.3 17.8L17.2 21L16.1 17.8L12.9 16.7L16.1 15.6Z"),
            // Focus
            Map.entry("tower.focus", "S M3.8 9V3.8H9 M15 3.8H20.2V9 M20.2 15V20.2H15 M9 20.2H3.8V15 ; F O12 12 2.8"),
            // Frenzy
            Map.entry("tower.frenzy", "F M4 20.5V10.5C4 7.6 5.2 5.4 6 4C6.8 5.4 8 7.6 8 10.5V20.5Z M10 20.5V10.5C10 7.6 11.2 5.4 12 4C12.8 5.4 14 7.6 14 10.5V20.5Z M16 20.5V10.5C16 7.6 17.2 5.4 18 4C18.8 5.4 20 7.6 20 10.5V20.5Z ; X M4 12.8H8 M10 12.8H14 M16 12.8H20"),
            // Haste
            Map.entry("tower.haste", "S O12 13.6 7.6 ; S M12 13.6V9.4 M12 13.6L15 15.4 M18.4 6.4L20 4.8 ; F M9.8 2.4H14.2V4.8H9.8Z"),
            // Charged Shot
            Map.entry("tower.charged_shot", "F M4.6 21V10.5C4.6 7.2 6.1 4.8 7.6 3.2C9.1 4.8 10.6 7.2 10.6 10.5V21Z ; X M4.6 13.2H10.6 ; F M18 2.6L12.8 11.8H16.4L15 21.4L20.8 10.4H17.2Z"),
            // Patience
            Map.entry("tower.patience", "S M5.6 2.8H18.4 M5.6 21.2H18.4 M7.4 2.8C7.4 8 12 10 12 12C12 14 7.4 16 7.4 21.2 M16.6 2.8C16.6 8 12 10 12 12C12 14 16.6 16 16.6 21.2 ; F M9.2 21.2C9.4 18.6 11 17.4 12 16.8C13 17.4 14.6 18.6 14.8 21.2Z M9.6 6.6H14.4C13.8 8.4 12.8 9.4 12 10C11.2 9.4 10.2 8.4 9.6 6.6Z"),
            // Deadeye Round
            Map.entry("tower.deadeye_round", "T O12 12 9.6 ; F M9.6 20.6V10C9.6 6.6 11 4.2 12 2.6C13 4.2 14.4 6.6 14.4 10V20.6Z ; X M9.6 13.4H14.4"),
            // Silver Round
            Map.entry("tower.silver_round", "F M5.8 21V10C5.8 6.6 7.3 4.2 8.3 2.6C9.3 4.2 10.8 6.6 10.8 10V21Z ; X M5.8 13.4H10.8 ; F M17 3.4L18.2 7.3L22.1 8.5L18.2 9.7L17 13.6L15.8 9.7L11.9 8.5L15.8 7.3Z ; F M18.2 15.6L18.8 17.4L20.6 18L18.8 18.6L18.2 20.4L17.6 18.6L15.8 18L17.6 17.4Z"),
            // Bracketing
            Map.entry("tower.bracketing", "S M7.4 3.6H4V20.4H7.4 M16.6 3.6H20V20.4H16.6 ; F O12 12 3.2"),
            // Napalm Shell
            Map.entry("tower.napalm_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; K M12 10.4C12 10.4 14.4 12.6 14.4 14.8A2.4 2.4 0 0 1 9.6 14.8C9.6 13.4 10.6 12.2 11.2 11.8C11.4 12.8 12 13.2 12 13.2C12 12.2 11.8 11.2 12 10.4Z"),
            // Tar Shell
            Map.entry("tower.tar_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; K M12 10.4C12 10.4 14.4 13.2 14.4 15A2.4 2.4 0 0 1 9.6 15C9.6 13.2 12 10.4 12 10.4Z"),
            // Cryo Shell
            Map.entry("tower.cryo_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; X M12 10.2V17.4 M9.2 12L14.8 15.6 M9.2 15.6L14.8 12"),
            // Nuke Shell
            Map.entry("tower.nuke_shell", "F M6 9.6C6 6 8.6 3.4 12 3.4C15.4 3.4 18 6 18 9.6C18 11.4 16.6 12.6 15 12.6H14V17.2C14 18.4 15.6 19.6 18 20.8H6C8.4 19.6 10 18.4 10 17.2V12.6H9C7.4 12.6 6 11.4 6 9.6Z ; X M8.6 10.4C10.4 11.2 13.6 11.2 15.4 10.4"),
            // Cryo Missile
            Map.entry("tower.cryo_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; T M17.6 13.6V21.4 M14.2 15.6L21 19.4 M14.2 19.4L21 15.6"),
            // Arcane Missile
            Map.entry("tower.arcane_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F M17.6 13.2L18.6 16.4L21.8 17.4L18.6 18.4L17.6 21.6L16.6 18.4L13.4 17.4L16.6 16.4Z"),
            // EMP Missile
            Map.entry("tower.emp_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F M19 12.6L14.8 18.2H17.6L16.4 22.2L21 16H18.2Z"),
            // Tracer Missile
            Map.entry("tower.tracer_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F O13.6 19.6 1.3 O17.4 19.6 1.3 O21.2 19.6 1.3"),
            // Event Horizon
            Map.entry("tower.event_horizon", "S@-20 M2.4 12A9.6 3.4 0 1 0 21.6 12A9.6 3.4 0 1 0 2.4 12Z ; K O12 12 5.6 ; S O12 12 4.2 ; S@-20 M2.4 12A9.6 3.4 0 0 0 21.6 12"),
            // Rally
            Map.entry("tower.rally", "S M5.6 21.6V2.8 ; F M5.6 3.4H19L15.8 8L19 12.6H5.6Z"),
            // Aura
            Map.entry("tower.aura", "F O12 12 3.4 ; S O12 12 6.8 ; T M12 1.6V3.6 M12 20.4V22.4 M1.6 12H3.6 M20.4 12H22.4 M4.6 4.6L6 6 M18 18L19.4 19.4 M4.6 19.4L6 18 M18 6L19.4 4.6"),
            // Ley Line
            Map.entry("tower.ley_line", "F M4.6 12.8L6.4 17.4L4.6 22L2.8 17.4Z M19.4 2L21.2 6.6L19.4 11.2L17.6 6.6Z ; S M6.6 15.4L17.4 8.6 ; F M12 8.6L12.9 11.1L15.4 12L12.9 12.9L12 15.4L11.1 12.9L8.6 12L11.1 11.1Z"),
            // Jammed
            Map.entry("tower.jammed", "F M12 9.4L16.2 21.4H13.6L12 16L10.4 21.4H7.8Z ; S M3.4 6.4L6.4 3.4L9.2 7.8L12 3.4L14.8 7.8L17.6 3.4L20.6 6.4"),
            // Interference
            Map.entry("tower.interference", "S M2 9C5 4.6 8 4.6 11 9S17 13.4 22 9 ; S M2 15C5 19.4 8 19.4 11 15S17 10.6 22 15"),
            // Nest
            Map.entry("tower.nest", "F M2.6 13.2H21.4C21.4 18 17.2 21.6 12 21.6C6.8 21.6 2.6 18 2.6 13.2Z ; F M6.4 13.2V8C6.4 6.6 7.2 5.4 8 4.6C8.8 5.4 9.6 6.6 9.6 8V13.2Z M10.4 13.2V6C10.4 4.6 11.2 3.4 12 2.6C12.8 3.4 13.6 4.6 13.6 6V13.2Z M14.4 13.2V8C14.4 6.6 15.2 5.4 16 4.6C16.8 5.4 17.6 6.6 17.6 8V13.2Z ; X M3 13.4H21"),
            // Stoke
            Map.entry("tower.stoke", "F M9.6 5.2C9.6 5.2 15.4 10 15.4 15.2A5.8 5.8 0 0 1 3.8 15.2C3.8 12.4 5.4 10.4 6.8 9.4C7.2 11.4 8 12.4 9 12.8C8.6 10.2 8.8 7.4 9.6 5.2Z ; S M16.2 9.4L18.8 6.8L21.4 9.4 M16.2 14.6L18.8 12L21.4 14.6"),
            // Kinship
            Map.entry("tower.kinship", "T M12 5.6L5.6 17.6H18.4Z ; F O12 5.4 3 ; F M2.6 15.2H8.6V21.2H2.6Z ; F M18.4 14.4L21.6 20.6H15.2Z"),
            // Ping
            Map.entry("tower.ping", "F O5.6 18.4 2.4 ; S M5.6 11.6A6.8 6.8 0 0 1 12.4 18.4 M5.6 6.6A11.8 11.8 0 0 1 17.4 18.4 ; T M5.6 1.8A16.6 16.6 0 0 1 22.2 18.4"),
            // Shatter
            Map.entry("tower.shatter", "F O12 12 1.8 ; F M12 9L10.4 2.2L14.2 3.2Z M14.8 11L21.6 8.2L20.8 12.4Z M14.2 14.4L18 20.8L13.8 20.4Z M9.8 14.6L6.2 21.2L4.4 17.6Z M9.2 10.8L2.6 8.4L4.2 4.8Z"),
            // Arc
            Map.entry("tower.arc", "F O4.6 18.6 2.6 O19.4 5.4 2.6 ; S M6.6 16.8L10.2 15.4L9 11.6L14 10.8L12.8 7.2L17.4 6.8"),
            // Hex
            Map.entry("tower.hex", "S O12 12 9 ; S M12 19.6L5 7.4H19Z ; F O12 11.2 1.8"),
            // Shrapnel
            Map.entry("tower.shrapnel", "F O12 12 2.4 ; F M4.6 4.6L8.6 5.8L5.8 8.6Z M19.4 4.6L18.2 8.6L15.4 5.8Z M19.4 19.4L15.4 18.2L18.2 15.4Z M4.6 19.4L5.8 15.4L8.6 18.2Z ; T M12 7.6V3.4 M12 16.4V20.6 M7.6 12H3.4 M16.4 12H20.6"),
            // Bomblet
            Map.entry("tower.bomblet", "F O7.4 16.4 3.8 O16.8 14.6 3.4 O11.2 6.8 2.8 ; T M9.8 13.4L10.6 11.6 M18.6 11.8L19.6 10.2 M12.6 4.4L13.6 2.8"));

    private TowerTermPictograms() {
    }
}
```

### `src/main/java/td/ui/EnemyTermPictograms.java`

```java
package td.ui;

import java.util.List;
import java.util.Map;

/** Enemy traits and abilities, keyed by {@code TraitTerm.key()} and {@code AbilityTerm.key()}. Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */
final class EnemyTermPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Regeneration
            Map.entry("trait.regeneration", "S M19.6 12A7.6 7.6 0 1 1 15.2 5.2 ; F M13.6 2.2L19.8 4.6L15 8.8Z ; F M10.7 8.4H13.3V10.7H15.6V13.3H13.3V15.6H10.7V13.3H8.4V10.7H10.7Z"),
            // Adrenaline
            Map.entry("trait.adrenaline", "F M8.6 21C3.6 17.4 1.8 14.4 1.8 12A3.5 3.5 0 0 1 8.6 10.8A3.5 3.5 0 0 1 15.4 12C15.4 14.4 13.6 17.4 8.6 21Z ; S M13.6 9.6L20.6 2.6 M15.2 2.4H20.8V8"),
            // Elusive
            Map.entry("trait.elusive", "F O9.6 12.6 2.4 ; S M2.6 19.4C9.6 19.4 13.6 6 20.4 6 ; F M21.8 6L17.2 2.8V9.2Z"),
            // Adaptation
            Map.entry("trait.adaptation", "S M12 2.6L19.6 5.4V11C19.6 15.8 16.4 19.4 12 21.2C7.6 19.4 4.4 15.8 4.4 11V5.4Z ; T M15.4 10.6A3.6 3.6 0 0 0 8.8 9.8 M8.6 13.4A3.6 3.6 0 0 0 15.2 14.2 ; F M8 7.6L8.8 10.6L11.4 9.2Z M16 16.4L15.2 13.4L12.6 14.8Z"),
            // Jam
            Map.entry("trait.jam", "F O12 12 2.4 ; S M7.6 8A5.8 5.8 0 0 0 7.6 16 M16.4 8A5.8 5.8 0 0 1 16.4 16 ; T M4 4.8A10 10 0 0 0 4 19.2 M20 4.8A10 10 0 0 1 20 19.2"),
            // Brace
            Map.entry("ability.brace", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; K M10.8 6.4H13.2V13.2H10.8Z ; K O12 16.4 1.4"),
            // Capacitor
            Map.entry("ability.capacitor", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; K M13.4 5.4L9 12.6H12L10.6 18.6L15.2 10.8H12.2Z"),
            // Bulwark
            Map.entry("ability.bulwark", "F M5.6 2.8H18.4V14.6C18.4 18.2 15.6 20.6 12 21.6C8.4 20.6 5.6 18.2 5.6 14.6Z ; K M11 5.6H13V18.4H11Z M8 10.8H16V12.8H8Z"),
            // Shield Wall
            Map.entry("ability.shield_wall", "F M6.4 3.6L10.6 5.2V10.2C10.6 13.6 8.6 15.6 6.4 16.6C4.2 15.6 2.2 13.6 2.2 10.2V5.2Z M17.6 3.6L21.8 5.2V10.2C21.8 13.6 19.8 15.6 17.6 16.6C15.4 15.6 13.4 13.6 13.4 10.2V5.2Z ; K M12 6.2L17.6 8.4V14.4C17.6 18.4 15 20.8 12 22C9 20.8 6.4 18.4 6.4 14.4V8.4Z ; F M12 7.6L16.4 9.4V14.4C16.4 17.6 14.4 19.6 12 20.6C9.6 19.6 7.6 17.6 7.6 14.4V9.4Z"),
            // Guard
            Map.entry("ability.guard", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; X M15.2 11.4A3.4 3.4 0 1 1 13.4 8.4 ; K M12.6 6.4L16 7.6L13.6 10.2Z"),
            // Arcane Ward
            Map.entry("ability.arcane_ward", "F M12 2.5L20 5.5V11C20 16 16.5 19.8 12 21.5C7.5 19.8 4 16 4 11V5.5Z ; K M12 6.2L13.3 10.4L17.4 11.6L13.3 12.8L12 17L10.7 12.8L6.6 11.6L10.7 10.4Z"),
            // Aegis
            Map.entry("ability.aegis", "F M12 6L17 7.9V11.4C17 14.6 14.8 17 12 18C9.2 17 7 14.6 7 11.4V7.9Z ; T M12 1.4V3.6 M12 20.4V22.6 M2 12H4.2 M19.8 12H22 M4.6 4.6L6.2 6.2 M17.8 17.8L19.4 19.4 M4.6 19.4L6.2 17.8 M17.8 6.2L19.4 4.6"),
            // Harden
            Map.entry("ability.harden", "F M10.6 3.5L17.6 6.2V11.2C17.6 15.6 14.6 19 10.6 20.6C6.6 19 3.6 15.6 3.6 11.2V6.2Z ; K M18.4 1.6L19.9 6.1L24.4 7.6L19.9 9.1L18.4 13.6L16.9 9.1L12.4 7.6L16.9 6.1Z ; F M18.4 3.2L19.4 6.6L22.8 7.6L19.4 8.6L18.4 12L17.4 8.6L14 7.6L17.4 6.6Z"),
            // Panic
            Map.entry("ability.panic", "F M9.6 3.5L16.6 6.2V11.2C16.6 15.6 13.6 19 9.6 20.6C5.6 19 2.6 15.6 2.6 11.2V6.2Z ; F M19.6 3.6C19.6 3.6 21.6 6 21.6 7.4A2 2 0 0 1 17.6 7.4C17.6 6 19.6 3.6 19.6 3.6Z M19.6 11.6C19.6 11.6 21.4 13.8 21.4 15A1.8 1.8 0 0 1 17.8 15C17.8 13.8 19.6 11.6 19.6 11.6Z"),
            // Ward
            Map.entry("ability.ward", "S O12 12 9.6 ; F M12 6.4L16.6 8.2V11.4C16.6 14.4 14.6 16.6 12 17.6C9.4 16.6 7.4 14.4 7.4 11.4V8.2Z"),
            // Mend
            Map.entry("ability.mend", "T O12 12 9.4 ; F M10.4 6.4H13.6V10.4H17.6V13.6H13.6V17.6H10.4V13.6H6.4V10.4H10.4Z"),
            // Recover
            Map.entry("ability.recover", "F M6.4 8.4H9.6V12.4H13.6V15.6H9.6V19.6H6.4V15.6H2.4V12.4H6.4Z ; F M19.4 2.6A5 5 0 1 0 21.8 10.6A3.9 3.9 0 1 1 19.4 2.6Z"),
            // Last Rites
            Map.entry("ability.last_rites", "F M5.8 20.6V10C5.8 6.4 8.6 3.4 12 3.4C15.4 3.4 18.2 6.4 18.2 10V20.6Z ; K M10.9 7.6H13.1V10.6H16V12.8H13.1V17.2H10.9V12.8H8V10.6H10.9Z ; T M2.6 21.4H21.4"),
            // Shed Skin
            Map.entry("ability.shed_skin", "F M10.4 7.4H13.6V11.4H17.6V14.6H13.6V18.6H10.4V14.6H6.4V11.4H10.4Z ; F M4.6 2.6L5.5 5.1L8 6L5.5 6.9L4.6 9.4L3.7 6.9L1.2 6L3.7 5.1Z M19.4 2.6L20.1 4.6L22.1 5.3L20.1 6L19.4 8L18.7 6L16.7 5.3L18.7 4.6Z M19.6 16.6L20.3 18.6L22.3 19.3L20.3 20L19.6 22L18.9 20L16.9 19.3L18.9 18.6Z"),
            // Hunker
            Map.entry("ability.hunker", "F M2.8 17.6C2.8 11 7 6.6 12 6.6C17 6.6 21.2 11 21.2 17.6Z ; K M11 9.4H13V11.6H15.2V13.6H13V15.8H11V13.6H8.8V11.6H11Z ; T M1.8 20.2H22.2"),
            // Bolt
            Map.entry("ability.bolt", "F M9.4 2.6L3.4 12.6H7.6L6 21.4L13.2 10.4H8.8Z ; F M13.6 5.6L20 12L13.6 18.4H10.2L16.6 12L10.2 5.6Z"),
            // Sidestep
            Map.entry("ability.sidestep", "S M2.8 17.6C8 17.6 8.6 8.4 14 8.4H18.6 ; F M21.6 8.4L17.2 4.8V12Z ; T M3.6 21.2H10.4 M6 13.4H2.4"),
            // Second Wind
            Map.entry("ability.second_wind", "S M2.8 8.4H13.8A3 3 0 1 0 10.8 5.4 M2.8 13.2H18.2A3 3 0 1 1 15.2 16.2 M2.8 18H9.8"),
            // Fade
            Map.entry("ability.fade", "S M3 9.6C7 14.6 17 14.6 21 9.6 ; T M6 12.6L4.6 15 M12 14.2V17 M18 12.6L19.4 15 ; F O6 19.6 0.9 O10 20.8 0.8 O14.6 20.4 0.9 O18.6 19.4 0.7"),
            // Shroud
            Map.entry("ability.shroud", "F M12 2.8C7.4 2.8 4.8 7.2 4.8 12V21.2H19.2V12C19.2 7.2 16.6 2.8 12 2.8Z ; K M12 8.2C9.8 8.2 8.6 10.4 8.6 12.6C8.6 14.8 10 16.2 12 16.2C14 16.2 15.4 14.8 15.4 12.6C15.4 10.4 14.2 8.2 12 8.2Z"),
            // Phase
            Map.entry("ability.phase", "F M12 3C7.6 3 5 6.4 5 10.6V20.6L7.4 18.8L9.8 20.8L12 19Z ; S M12 3C16.4 3 19 6.4 19 10.6V20.6L16.6 18.8L14.2 20.8L12 19 ; K O9 9.8 1.3"),
            // Vanish
            Map.entry("ability.vanish", "F O8.6 14.4 4.2 O14.2 11.6 5 O17 16.2 3.6 O11.6 17.4 3.4 ; F M5 3.2L5.7 5.1L7.6 5.8L5.7 6.5L5 8.4L4.3 6.5L2.4 5.8L4.3 5.1Z M19.6 2.2L20.2 3.8L21.8 4.4L20.2 5L19.6 6.6L19 5L17.4 4.4L19 3.8Z"),
            // Reinforce
            Map.entry("ability.reinforce", "F O8 7.2 2.8 ; F M3.2 19.6V15C3.2 12.6 5.4 11 8 11C10.6 11 12.8 12.6 12.8 15V19.6Z ; F M16.9 3.6H19.1V6.9H22.4V9.1H19.1V12.4H16.9V9.1H13.6V6.9H16.9Z"),
            // Swarm
            Map.entry("ability.swarm", "F O6 6.6 1.9 O12.2 4.6 1.7 O18.2 7.4 1.9 O8.4 12.8 1.7 O15 13 2 O4.8 18.4 1.7 O11.2 19 1.9 O18.6 18.4 1.7"),
            // Brood
            Map.entry("ability.brood", "F O12 15.6 4.8 ; F O4.8 6.6 2.2 O12 3.6 2.2 O19.2 6.6 2.2 ; T M8.6 12L6.6 8.6 M12 10.4V6.4 M15.4 12L17.4 8.6"),
            // Split
            Map.entry("ability.split", "F M10.4 4.8A7.2 7.2 0 0 0 10.4 19.2Z ; F M13.6 4.8A7.2 7.2 0 0 1 13.6 19.2Z ; T M3.6 12H1.2 M20.4 12H22.8"),
            // Lay Egg
            Map.entry("ability.lay_egg", "F M12 2.8C8 2.8 5.2 9.2 5.2 14C5.2 18.2 8.2 21.2 12 21.2C15.8 21.2 18.8 18.2 18.8 14C18.8 9.2 16 2.8 12 2.8Z ; K O9.6 11.6 1.2 O14.4 15.2 1.5 O10.6 17.6 0.9"),
            // Hatch
            Map.entry("ability.hatch", "F M5.2 14L7.6 12L9.8 14.4L12 11.8L14.2 14.4L16.4 12L18.8 14C18.8 18.2 15.8 21.4 12 21.4C8.2 21.4 5.2 18.2 5.2 14Z ; F M6.6 8.8L8.8 7L11.2 9.4L13.4 7L15.6 8.8C14.8 5 13 2.2 11.2 2.2C9.2 2.2 7.4 4.6 6.6 8.8Z"));

    private EnemyTermPictograms() {
    }
}
```

### `src/main/java/td/ui/PerkPictograms.java`

```java
package td.ui;

import java.util.List;
import java.util.Map;

/** Perk lines, keyed by the id of the node whose art the line shows ({@code UpgradeLine.artNode()}). Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */
final class PerkPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Range
            Map.entry("base.range", "S O9.4 12 5.8 ; F O9.4 12 1.6 ; S M15.4 12H21.4 M18.4 8.8L21.6 12L18.4 15.2"),
            // Attune
            Map.entry("base.attune", "F M5.6 8.6L9 3.8H15L18.4 8.6L12 20.6Z ; X M5.6 8.6H18.4 M9 3.8L10.4 8.6L12 20.6 M15 3.8L13.6 8.6L12 20.6"),
            // Focused Optics
            Map.entry("sniper.head.focused_optics.1", "F M2.6 9.6H14.4V14.4H2.6Z M14.4 7.6H19.4L21.4 9.6V14.4L19.4 16.4H14.4Z ; F M5.4 14.4H8.6V18H5.4Z ; X M17.6 9.4V14.6"),
            // Railgun
            Map.entry("sniper.head.focused_optics.4a", "S M2 12H19.4 ; S O7 12 3 O14.4 12 3 ; F M22 12L18.4 8.8V15.2Z"),
            // Executioner
            Map.entry("sniper.head.focused_optics.4b", "S M3.6 20.6L14 8.2 ; F M10.8 2.8C15.6 3 19.8 6.6 20.8 11.4L13.8 12.6L10.6 9.6Z"),
            // Marksman's Eye
            Map.entry("sniper.head.marksmans_eye.1", "S M2.4 12C6 6.2 18 6.2 21.6 12C18 17.8 6 17.8 2.4 12Z ; S O12 12 3.4 ; T M12 6.4V17.6 M6.4 12H17.6"),
            // Unbroken Aim
            Map.entry("sniper.head.marksmans_eye.4a", "S M3.6 12C3.6 9.4 5.4 7.8 7.6 7.8C11.4 7.8 12.6 16.2 16.4 16.2C18.6 16.2 20.4 14.6 20.4 12C20.4 9.4 18.6 7.8 16.4 7.8C12.6 7.8 11.4 16.2 7.6 16.2C5.4 16.2 3.6 14.6 3.6 12Z"),
            // Sunder Rounds
            Map.entry("sniper.head.marksmans_eye.4b", "=effect.sundered"),
            // Tradecraft
            Map.entry("sniper.extra.tradecraft.1", "F M2.4 8.8C5.4 7.2 9.2 7.6 12 9C14.8 7.6 18.6 7.2 21.6 8.8C21.8 13 19.6 16 16.6 16C14.6 16 13.2 14.6 12 13.2C10.8 14.6 9.4 16 7.4 16C4.4 16 2.2 13 2.4 8.8Z ; K M5.2 11.2C6.2 10.2 8.4 10.2 9.6 11.6C8.6 12.8 6.4 13 5.2 11.2Z M18.8 11.2C17.8 10.2 15.6 10.2 14.4 11.6C15.4 12.8 17.6 13 18.8 11.2Z"),
            // Momentum
            Map.entry("sniper.special.momentum", "F O15.4 12 5.4 ; T M1.8 8.6H8 M3.4 12H8.6 M1.8 15.4H8 ; K M13.4 9.4A3 3 0 0 1 17.8 9.6"),
            // Ricochet
            Map.entry("sniper.special.ricochet", "T M2.6 20.6H21.4 ; S M3.4 4.6L10.4 18.4L16.6 8.2 ; F M19.2 4.2L14.6 6.6L18.6 10.2Z ; F O10.4 18.4 1.6"),
            // Headhunter
            Map.entry("sniper.special.headhunter", "F M4.4 17.6L3.4 7.6L8.4 11.6L12 4.8L15.6 11.6L20.6 7.6L19.6 17.6Z ; F M4.4 19.2H19.6V21.4H4.4Z ; K O12 13.2 1.8"),
            // Hollow Point
            Map.entry("sniper.special.hollow_point", "F M8.6 21.2V10.2C8.6 6.6 10.2 4 12 2.6C13.8 4 15.4 6.6 15.4 10.2V21.2Z ; K O12 6.6 1.7 ; X M8.6 13.6H15.4"),
            // Fourth Shot
            Map.entry("sniper.special.fifth_shot", "F M2.8 6.4H5V17.6H2.8Z M7 6.4H9.2V17.6H7Z M11.2 6.4H13.4V17.6H11.2Z ; F M18.4 4.6L19.8 9.8L21.8 12L19.8 14.2L18.4 19.4L17 14.2L15 12L17 9.8Z"),
            // Shatter Shot
            Map.entry("sniper.special.shatter_shot", "=tower.shatter"),
            // Arc
            Map.entry("splash.head.arc.1", "=tower.arc"),
            // Chain Lightning
            Map.entry("splash.head.arc.4a", "S M3 3.4L8.8 10L5.6 12.8L11.4 20.6 ; S M8.8 10L15 9L13 13L20.8 15.2 ; S M15 9L20.6 3.4 ; F O11.4 20.6 1.6 O20.8 15.2 1.6 O20.6 3.4 1.6"),
            // Lightning Rod
            Map.entry("splash.head.arc.4b", "S M14.6 21.4V8.4 M11.2 21.4H18 ; F O14.6 5.6 2.6 ; F M7.6 2.4L3 10H6.2L4.6 15.4L10 7.8H6.8Z"),
            // Hex
            Map.entry("splash.head.hex.1", "=tower.hex"),
            // Rime Coven
            Map.entry("splash.head.hex.4a", "=effect.rime"),
            // Ash Coven
            Map.entry("splash.head.hex.4b", "=effect.ash"),
            // Hex of Inversion
            Map.entry("splash.special.inversion", "=effect.inversion"),
            // Hex of Sympathy
            Map.entry("splash.special.sympathy", "=effect.sympathy"),
            // Hex of Reckoning
            Map.entry("splash.special.reckoning", "=effect.reckoning"),
            // Blast Engineering
            Map.entry("splash.extra.blast_engineering.1", "F O10.4 14.2 6.8 ; S M15 9.6C16 7.6 17.2 6 19 5.2 ; F M20.4 1.8L21.1 3.7L23 4.4L21.1 5.1L20.4 7L19.7 5.1L17.8 4.4L19.7 3.7Z ; K O8 11.8 1.6"),
            // Thunderclap
            Map.entry("splash.special.thunderclap", "F M13.4 3L8.4 12.4H11.6L10.4 21L15.8 10.8H12.6Z ; S M6.4 8A6 6 0 0 0 6.4 16 M17.6 8A6 6 0 0 1 17.6 16 ; T M3 5.4A9.6 9.6 0 0 0 3 18.6 M21 5.4A9.6 9.6 0 0 1 21 18.6"),
            // Static Charge
            Map.entry("splash.special.static_charge", "=effect.charged"),
            // Thunderstrike
            Map.entry("splash.special.thunderstrike", "F M6.4 13C4.4 13 3 11.6 3 9.8C3 8 4.4 6.6 6.2 6.6C6.8 4.4 8.8 3 11.2 3C14 3 16.2 5 16.6 7.6C18.8 7.6 20.6 9.2 20.6 11.2C20.6 13 19.2 13 17.4 13Z ; F M12.8 13.8L9.4 18.6H12L10.8 22.4L15.6 16.6H12.8L14.6 13.8Z"),
            // Siege Rounds
            Map.entry("mortar.head.siege_rounds.1", "F O12 13 7.6 ; K O9.2 10.2 2 ; F M10 3.4H14V5.8H10Z"),
            // Tactical Nuke
            Map.entry("mortar.head.siege_rounds.4a", "=tower.nuke_shell"),
            // Bunker Buster
            Map.entry("mortar.head.siege_rounds.4b", "F M8.6 2.4H15.4V11.6L12 17L8.6 11.6Z ; X M8.6 6.4H15.4 ; S M2.4 19H21.6 ; T M7 19L5.2 22.4 M17 19L18.8 22.4 M12 19V22.6"),
            // Fragmentation Rounds
            Map.entry("mortar.head.fragmentation_rounds.1", "=tower.shrapnel"),
            // Carpet Bombing
            Map.entry("mortar.head.fragmentation_rounds.4a", "=tower.bomblet"),
            // Shrapnel Storm
            Map.entry("mortar.head.fragmentation_rounds.4b", "F O9 10.4 2.2 ; F M3.4 3.4L6.8 4.4L4.4 6.8Z M14.6 3.4L13.6 6.8L11.2 4.4Z M3.4 17.4L4.4 14L6.8 16.4Z ; T M9 6.4V3 M9 14.4V17.6 M5 10.4H2 M13 10.4H16 ; F M17.6 11.6C17.6 11.6 21.6 16.2 21.6 18.6A4 4 0 0 1 13.6 18.6C13.6 16.2 17.6 11.6 17.6 11.6Z"),
            // Ballistics
            Map.entry("mortar.extra.ballistics.1", "S M3.2 19.6C5.4 7 14.6 5 19.4 14.8 ; F O20.4 17.6 2.4 ; T M1.8 21.6H22.2 ; F M3.2 19.6L1.6 16.6L5 17Z"),
            // Napalm
            Map.entry("mortar.special.napalm", "=tower.napalm_shell"),
            // Tar
            Map.entry("mortar.special.tar", "=tower.tar_shell"),
            // Cryo Shells
            Map.entry("mortar.special.cryo_shells", "=tower.cryo_shell"),
            // Gravity
            Map.entry("pulse.head.gravity.1", "S M12 12C12 10.6 13.4 10 14.4 10.8C15.8 12 15 14.6 12.8 15C10 15.6 8.4 12.8 9.2 10.4C10.2 7.4 14.4 6.4 17 8.4C20 10.8 19.6 15.8 16.4 18.2C12.4 21.2 6 19.6 4 15C2 10.2 5 4.6 10.2 3.6"),
            // Gravity Well
            Map.entry("pulse.head.gravity.4a", "S M2.4 7.6A9.6 3.2 0 1 0 21.6 7.6A9.6 3.2 0 1 0 2.4 7.6Z ; S M6.2 13A5.8 2 0 1 0 17.8 13A5.8 2 0 1 0 6.2 13Z ; F O12 18.8 2.2"),
            // Stasis
            Map.entry("pulse.head.gravity.4b", "=effect.stilled"),
            // Ward
            Map.entry("pulse.head.ward.1", "=effect.revealed"),
            // True Sight
            Map.entry("pulse.head.ward.4a", "=effect.unveiled"),
            // Purge
            Map.entry("pulse.head.ward.4b", "F M11 3.4L4.2 6V11.2C4.2 13.8 5.2 16 6.8 17.6L9.6 12.8L8.6 8.4Z ; F M13 3.4L19.8 6V11.2C19.8 13.8 18.8 16 17.2 17.6L14.4 12.8L15.4 8.4Z ; F M7.8 19.2L10.6 14.4H13.4L16.2 19.2C15 20.2 13.6 21 12 21.6C10.4 21 9 20.2 7.8 19.2Z"),
            // Bite
            Map.entry("pulse.extra.bite.1", "F M2.6 3.6H21.4V7.4L18.8 12.6L16.4 7.4L14 12.6L12 7.4L10 12.6L7.6 7.4L5.2 12.6L2.6 7.4Z ; F M2.6 20.4H21.4V17.6L19.2 13.6L17 17.6L12 13.4L7 17.6L4.8 13.6L2.6 17.6Z"),
            // Arc Discharge
            Map.entry("pulse.special.arc_discharge", "=effect.condemned"),
            // Soul Drain
            Map.entry("pulse.special.soul_drain", "F M12 2.4C12 2.4 17 7 17 11.2C17 14.2 14.8 16.2 12 16.2C9.2 16.2 7 14.2 7 11.2C7 9.2 8.4 7.6 9.4 6.8C9.6 8.6 10.4 9.6 11.2 9.8C10.8 7.4 11 4.6 12 2.4Z ; S M6.2 19L12 22.2L17.8 19 ; K O12 11.8 1.6"),
            // Ley Line
            Map.entry("pulse.special.ley_line", "=tower.ley_line"),
            // Twin Warhead
            Map.entry("seeker.head.twin_warhead.1", "F M4.4 20.4V9.6C4.4 6.6 5.8 4.4 7.6 2.8C9.4 4.4 10.8 6.6 10.8 9.6V20.4Z M13.2 20.4V9.6C13.2 6.6 14.6 4.4 16.4 2.8C18.2 4.4 19.6 6.6 19.6 9.6V20.4Z ; F M4.4 20.4L2.6 22.2V17.6L4.4 15.8Z M10.8 20.4L12.6 22.2V17.6L10.8 15.8Z M19.6 20.4L21.4 22.2V17.6L19.6 15.8Z ; X M4.4 9.4H10.8 M13.2 9.4H19.6"),
            // Shatterburst
            Map.entry("seeker.head.twin_warhead.4a", "=tower.shatter"),
            // Rearm
            Map.entry("seeker.head.twin_warhead.4b", "F M5.4 20.6V10.2C5.4 7.4 6.8 5.2 8.6 3.6C10.4 5.2 11.8 7.4 11.8 10.2V20.6Z ; X M5.4 10.6H11.8 ; S M15.4 19.6A6.2 6.2 0 0 0 17.6 7.6 ; F M14.6 5.2L19.8 4.6L18.4 9.8Z"),
            // Deep Freeze
            Map.entry("seeker.head.deep_freeze.1", "=effect.freeze"),
            // Absolute Zero
            Map.entry("seeker.head.deep_freeze.4a", "S M9.6 14.4V5.2A2.4 2.4 0 0 1 14.4 5.2V14.4A4.4 4.4 0 1 1 9.6 14.4Z ; F O12 17.6 2.4 ; T M17.4 6.4H20.4 M17.4 10H20.4"),
            // Frostbite
            Map.entry("seeker.head.deep_freeze.4b", "S M10.4 4V20 M3.47 8L17.33 16 M3.47 16L17.33 8 ; F M18.6 1.8L19.8 5.4L23.4 6.6L19.8 7.8L18.6 11.4L17.4 7.8L13.8 6.6L17.4 5.4Z"),
            // Mixed Payloads
            Map.entry("seeker.extra.mixed_payloads.1", "F M5.6 3.6H18.4A2.6 2.6 0 0 1 18.4 8.8H5.6A2.6 2.6 0 0 1 5.6 3.6Z M5.6 9.4H18.4A2.6 2.6 0 0 1 18.4 14.6H5.6A2.6 2.6 0 0 1 5.6 9.4Z M5.6 15.2H18.4A2.6 2.6 0 0 1 18.4 20.4H5.6A2.6 2.6 0 0 1 5.6 15.2Z ; K O6.6 6.2 1 O6.6 12 1 O6.6 17.8 1"),
            // Arcane Warhead
            Map.entry("seeker.special.arcane_warhead", "=tower.arcane_missile"),
            // Nullifier
            Map.entry("seeker.special.nullifier", "S M13.2 3.6L20 6.2V11.2C20 15.6 17.2 19 13.2 20.6C9.2 19 6.4 15.6 6.4 11.2V6.2Z ; F@-45 M1.4 10.6H9.8L12.6 12L9.8 13.4H1.4Z"),
            // Hunter's Mark
            Map.entry("seeker.special.hunters_mark", "=tower.focus"),
            // Rapid Array
            Map.entry("sonar.head.rapid_array.1", "S O12 12 8.8 ; F M12 12V3.2A8.8 8.8 0 0 1 19.6 7.6Z ; F O12 12 1.6"),
            // Twin Beam
            Map.entry("sonar.head.rapid_array.4a", "S O12 12 8.8 ; F M12 12V3.2A8.8 8.8 0 0 1 19.6 7.6Z M12 12V20.8A8.8 8.8 0 0 1 4.4 16.4Z"),
            // Phased Array
            Map.entry("sonar.head.rapid_array.4b", "F O4.6 13 1.5 O8.6 13 1.5 O12.6 13 1.5 O4.6 17 1.5 O8.6 17 1.5 O12.6 17 1.5 O4.6 21 1.5 O8.6 21 1.5 O12.6 21 1.5 ; S M12.6 10.6L20.4 2.8 ; T M15.6 10.6L21.6 4.6 M10 8.2L16 2.2"),
            // Long Reach
            Map.entry("sonar.head.long_reach.1", "S M3.2 8.4C4.8 15.2 10 19.6 17.2 20.4 ; S M3.2 8.4L17.2 20.4 ; T M10.2 14.4L16.6 8 ; F O17.4 7.2 2 ; F M9 20.6H14V22.4H9Z"),
            // Horizon
            Map.entry("sonar.head.long_reach.4a", "F M5.6 16.6A6.4 6.4 0 0 1 18.4 16.6Z ; S M2 16.6H22 ; T M12 4.6V7.4 M4.4 8.4L6.4 10.4 M19.6 8.4L17.6 10.4 M4.6 20H19.4"),
            // Fault Line
            Map.entry("sonar.head.long_reach.4b", "S M2 12.6L6.2 10.2L9.4 14.6L13 9L16.4 13.8L19.4 10.6L22 12.4 ; T M2 18H8.6 M15.4 18H22 M2 6H7 M17 6H22"),
            // Frequency
            Map.entry("sonar.extra.frequency.1", "S M2 12C3.6 5.6 6.4 5.6 8 12S12.4 18.4 14 12S18.4 5.6 20 12C20.6 14.4 21.2 15.6 22 15.6"),
            // Mark on Sweep
            Map.entry("sonar.special.mark_on_sweep", "=effect.marked"),
            // Wide Band
            Map.entry("sonar.special.wide_band", "F M12 20.4L2.8 9.4A13 13 0 0 1 21.2 9.4Z ; X M7.2 14.6A7 7 0 0 1 16.8 14.6 M4.6 11.6A10.6 10.6 0 0 1 19.4 11.6"),
            // Command Ping
            Map.entry("sonar.special.command_ping", "=effect.priority"),
            // White Flame
            Map.entry("cinder.head.white_flame.1", "F M12 2C12 2 19.5 8 19.5 14.2A7.5 7.5 0 0 1 4.5 14.2C4.5 10.8 6.6 8.6 8.2 7.2C8.6 9.6 9.8 11 11 11.4C10.4 8.2 10.9 4.8 12 2Z ; K M12 9.6C12 9.6 16.2 13 16.2 16A4.2 4.2 0 0 1 7.8 16C7.8 13 12 9.6 12 9.6Z ; D M12 11.4C12 11.4 14.8 13.8 14.8 16A2.8 2.8 0 0 1 9.2 16C9.2 13.8 12 11.4 12 11.4Z"),
            // Flashpoint
            Map.entry("cinder.head.white_flame.4a", "F M9.6 5.6C9.6 5.6 15.4 10.2 15.4 15.2A5.8 5.8 0 0 1 3.8 15.2C3.8 12.4 5.4 10.4 6.8 9.4C7.2 11.4 8 12.4 9 12.8C8.6 10.2 8.8 7.6 9.6 5.6Z ; F M18.6 1.8L19.8 5.4L23.4 6.6L19.8 7.8L18.6 11.4L17.4 7.8L13.8 6.6L17.4 5.4Z"),
            // Combustion
            Map.entry("cinder.head.white_flame.4b", "F M12 1.6L14 7.2L19.4 4.6L16.8 10L22.4 12L16.8 14L19.4 19.4L14 16.8L12 22.4L10 16.8L4.6 19.4L7.2 14L1.6 12L7.2 10L4.6 4.6L10 7.2Z ; K M12 8.6C12 8.6 15 11.2 15 13.4A3 3 0 0 1 9 13.4C9 11.2 12 8.6 12 8.6Z"),
            // Wide Nozzle
            Map.entry("cinder.head.wide_nozzle.1", "F M2.4 9.6H7.4V14.4H2.4Z ; F M7.4 10.2L21.6 3.4V20.6L7.4 13.8Z ; X M10.6 12H21.6 M10.2 10.6L21.6 7.2 M10.2 13.4L21.6 16.8"),
            // Inferno Ring
            Map.entry("cinder.head.wide_nozzle.4a", "S O12 12 6.6 ; F M12 0.8C12 0.8 14.4 3 14.4 4.6A2.4 2.4 0 0 1 9.6 4.6C9.6 3 12 0.8 12 0.8Z M12 16.8C12 16.8 14.4 19 14.4 20.6A2.4 2.4 0 0 1 9.6 20.6C9.6 19 12 16.8 12 16.8Z M4 8.8C4 8.8 6.4 11 6.4 12.6A2.4 2.4 0 0 1 1.6 12.6C1.6 11 4 8.8 4 8.8Z M20 8.8C20 8.8 22.4 11 22.4 12.6A2.4 2.4 0 0 1 17.6 12.6C17.6 11 20 8.8 20 8.8Z"),
            // Dragon's Breath
            Map.entry("cinder.head.wide_nozzle.4b", "F M1.8 12C5.6 7.8 12 6 22.4 3.6C18.6 8 18.6 16 22.4 20.4C12 18 5.6 16.2 1.8 12Z ; K M6 12C8.6 10.2 12.6 9.2 17.4 8.6C16 10.8 16 13.2 17.4 15.4C12.6 14.8 8.6 13.8 6 12Z ; D M8.8 12C10.6 11.2 13 10.8 15.2 10.8C14.6 11.6 14.6 12.4 15.2 13.2C13 13.2 10.6 12.8 8.8 12Z"),
            // Fuel
            Map.entry("cinder.extra.fuel.1", "F M5.4 5.6H14.6L18.6 9.6V20.4A1.2 1.2 0 0 1 17.4 21.6H6.6A1.2 1.2 0 0 1 5.4 20.4Z ; K M7.8 7.4H11.8V9.4H7.8Z ; K M12 11.6C12 11.6 15 14.8 15 16.6A3 3 0 0 1 9 16.6C9 14.8 12 11.6 12 11.6Z ; S M16.4 5.6L18.4 3.2"),
            // Searing Flame
            Map.entry("cinder.special.searing_flame", "S M3.2 20.8L11.6 12.4 ; F M11 9.6L15.6 5L20.2 9.6L15.6 14.2Z ; K M13.6 9.6L15.6 7.6L17.6 9.6L15.6 11.6Z ; F M4.6 4.6C4.6 4.6 7.4 7 7.4 8.8A2.8 2.8 0 0 1 1.8 8.8C1.8 7 4.6 4.6 4.6 4.6Z"),
            // Thermal Shock
            Map.entry("cinder.special.thermal_shock", "F M8.6 3C8.6 3 13.6 7.6 13.6 12.4A5 5 0 0 1 3.6 12.4C3.6 9.6 5.2 7.8 6.4 6.8C6.8 8.8 7.6 9.8 8.4 10.2C8 7.8 8.2 5 8.6 3Z ; T M17 10V21.6 M12 12.9L22 18.7 M12 18.7L22 12.9"),
            // Pyromancer's Mark
            Map.entry("cinder.special.pyromancers_mark", "S O12 12 9.4 ; F M12 5.4C12 5.4 16.4 9.4 16.4 13A4.4 4.4 0 0 1 7.6 13C7.6 11 8.8 9.6 9.8 8.8C10 10.2 10.6 11 11.4 11.2C11 9.2 11.2 7 12 5.4Z"),
            // Amplifying Core
            Map.entry("aura.head.amplifying_core.1", "F M12 7.4L16.6 12L12 16.6L7.4 12Z ; S M12 1.6L9.6 4 M12 1.6L14.4 4 M12 22.4L9.6 20 M12 22.4L14.4 20 M1.6 12L4 9.6 M1.6 12L4 14.4 M22.4 12L20 9.6 M22.4 12L20 14.4"),
            // Broadcast
            Map.entry("aura.head.broadcast.1", "F M12 10.4L15.6 21.4H13.4L12 16.8L10.6 21.4H8.4Z ; F O12 8.4 2 ; S M7.6 4.6A6.2 6.2 0 0 0 7.6 12.2 M16.4 4.6A6.2 6.2 0 0 1 16.4 12.2 ; T M4.2 2A10.2 10.2 0 0 0 4.2 14.8 M19.8 2A10.2 10.2 0 0 1 19.8 14.8"),
            // Tutelage
            Map.entry("aura.extra.tutelage.1", "F M2.6 5.6C5.8 4.2 9.2 4.4 11.2 6.2V20C9.2 18.4 5.8 18.2 2.6 19.6Z M21.4 5.6C18.2 4.2 14.8 4.4 12.8 6.2V20C14.8 18.4 18.2 18.2 21.4 19.6Z"),
            // Withering Field
            Map.entry("aura.special.withering_field", "S M12 21.4V13.6C12 10 14.8 8.6 17.4 9.8 ; F M17.4 9.8C19.6 9.4 21.6 10.8 21.6 13.4C19.4 13.8 17.6 12.4 17.4 9.8Z M17.4 9.8C16.4 11.6 16.6 14 18.6 15.4C19.8 13.6 19.4 11.2 17.4 9.8Z ; T M6 21.4H18 ; F M7.6 15.6C5 15.6 3.4 13.6 3.6 11.2C6.2 11 7.8 13 7.6 15.6Z"),
            // Chosen
            Map.entry("aura.special.chosen", "F M12 2.6L14.6 9L21.4 9.4L16.2 13.8L17.9 20.4L12 16.8L6.1 20.4L7.8 13.8L2.6 9.4L9.4 9Z"),
            // Rally
            Map.entry("aura.special.rally", "=tower.rally"));

    private PerkPictograms() {
    }
}
```

