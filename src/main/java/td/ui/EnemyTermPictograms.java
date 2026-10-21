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
