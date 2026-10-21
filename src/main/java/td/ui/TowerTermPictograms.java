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
