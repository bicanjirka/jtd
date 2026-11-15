package td.ui;

import java.util.List;
import java.util.Map;

/** Tower buffs, debuffs and named mechanics, keyed by {@code TowerTerm.key()}. Each spec is {@link Pictogram#parse} input; "=key" reuses another pictogram. */
final class TowerTermPictograms {

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Steady Aim
            Map.entry("tower.steady_aim", "S O12 12 8.4 ; T M12 3.6V8.4 M12 15.6V20.4 M3.6 12H8.4 M15.6 12H20.4 ; F O12 12 1.7"),
            // Crit Streak
            // Focus
            Map.entry("tower.focus", "S M3.8 9V3.8H9 M15 3.8H20.2V9 M20.2 15V20.2H15 M9 20.2H3.8V15 ; F O12 12 2.8"),
            // Frenzy
            Map.entry("tower.frenzy", "F M4 20.5V10.5C4 7.6 5.2 5.4 6 4C6.8 5.4 8 7.6 8 10.5V20.5Z M10 20.5V10.5C10 7.6 11.2 5.4 12 4C12.8 5.4 14 7.6 14 10.5V20.5Z M16 20.5V10.5C16 7.6 17.2 5.4 18 4C18.8 5.4 20 7.6 20 10.5V20.5Z ; X M4 12.8H8 M10 12.8H14 M16 12.8H20"),
            // Haste
            Map.entry("tower.haste", "S O12 13.6 7.6 ; S M12 13.6V9.4 M12 13.6L15 15.4 M18.4 6.4L20 4.8 ; F M9.8 2.4H14.2V4.8H9.8Z"),
            // Charged Shot
            // Patience
            // Fourth Shot
            Map.entry("tower.fourth_shot", "T O12 12 9.6 ; F M9.6 20.6V10C9.6 6.6 11 4.2 12 2.6C13 4.2 14.4 6.6 14.4 10V20.6Z ; X M9.6 13.4H14.4"),
            // Silver Round
            // Hollow Point
            Map.entry("tower.hollow_point", "F M8.6 21.2V10.2C8.6 6.6 10.2 4 12 2.6C13.8 4 15.4 6.6 15.4 10.2V21.2Z ; K O12 6.6 1.7 ; X M8.6 13.6H15.4"),
            Map.entry("tower.silver_round", "F M5.8 21V10C5.8 6.6 7.3 4.2 8.3 2.6C9.3 4.2 10.8 6.6 10.8 10V21Z ; X M5.8 13.4H10.8 ; F M17 3.4L18.2 7.3L22.1 8.5L18.2 9.7L17 13.6L15.8 9.7L11.9 8.5L15.8 7.3Z ; F M18.2 15.6L18.8 17.4L20.6 18L18.8 18.6L18.2 20.4L17.6 18.6L15.8 18L17.6 17.4Z"),
            // Magazine
            Map.entry("tower.magazine", "F O4.4 19.4 1.6 ; S O9.8 14 2.8 ; S O16.8 7.2 4.6 ; F O16.8 7.2 1.6"),
            // Napalm Shell
            Map.entry("tower.napalm_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; K M12 10.4C12 10.4 14.4 12.6 14.4 14.8A2.4 2.4 0 0 1 9.6 14.8C9.6 13.4 10.6 12.2 11.2 11.8C11.4 12.8 12 13.2 12 13.2C12 12.2 11.8 11.2 12 10.4Z"),
            // Tar Shell
            Map.entry("tower.tar_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; K M12 10.4C12 10.4 14.4 13.2 14.4 15A2.4 2.4 0 0 1 9.6 15C9.6 13.2 12 10.4 12 10.4Z"),
            // Cryo Shell
            Map.entry("tower.cryo_shell", "F M8 20.4H16V11C16 7 14.2 4.2 12 2.6C9.8 4.2 8 7 8 11Z M8 20.4L5.4 22V17.4L8 15.4Z M16 20.4L18.6 22V17.4L16 15.4Z ; X M12 10.2V17.4 M9.2 12L14.8 15.6 M9.2 15.6L14.8 12"),
            // Heavy
            Map.entry("tower.heavy_shell", "F O12 13 7.6 ; K O9.2 10.2 2 ; F M10 3.4H14V5.8H10Z"),
            Map.entry("tower.payload", "=tower.napalm_shell"),
            Map.entry("tower.flare", "=effect.revealed"),
            // Nuke
            Map.entry("tower.nuke_shell", "F M6 9.6C6 6 8.6 3.4 12 3.4C15.4 3.4 18 6 18 9.6C18 11.4 16.6 12.6 15 12.6H14V17.2C14 18.4 15.6 19.6 18 20.8H6C8.4 19.6 10 18.4 10 17.2V12.6H9C7.4 12.6 6 11.4 6 9.6Z ; X M8.6 10.4C10.4 11.2 13.6 11.2 15.4 10.4"),
            // Cryo Missile
            Map.entry("tower.cryo_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; T M17.6 13.6V21.4 M14.2 15.6L21 19.4 M14.2 19.4L21 15.6"),
            // Arcane Missile
            Map.entry("tower.arcane_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F M17.6 13.2L18.6 16.4L21.8 17.4L18.6 18.4L17.6 21.6L16.6 18.4L13.4 17.4L16.6 16.4Z"),
            // EMP Missile
            Map.entry("tower.emp_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F M19 12.6L14.8 18.2H17.6L16.4 22.2L21 16H18.2Z"),
            // Tracer Missile
            Map.entry("tower.tracer_missile", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F O13.6 19.6 1.3 O17.4 19.6 1.3 O21.2 19.6 1.3"),
            // Rally
            Map.entry("tower.rally", "F M2.6 10.8H5.2V13.2H2.6Z M5.2 11.1H12.8L20.4 5.6V18.4L12.8 12.9H5.2Z ; S M7.4 12.9C7.4 17.4 12.8 17.4 12.8 12.9"),
            // Aura
            Map.entry("tower.aura", "F O12 12 3.4 ; S O12 12 6.8 ; T M12 1.6V3.6 M12 20.4V22.4 M1.6 12H3.6 M20.4 12H22.4 M4.6 4.6L6 6 M18 18L19.4 19.4 M4.6 19.4L6 18 M18 6L19.4 4.6"),
            // Jammed
            Map.entry("tower.jammed", "F M12 9.4L16.2 21.4H13.6L12 16L10.4 21.4H7.8Z ; S M3.4 6.4L6.4 3.4L9.2 7.8L12 3.4L14.8 7.8L17.6 3.4L20.6 6.4"),
            // Nest
            Map.entry("tower.nest", "F M2.6 13.2H21.4C21.4 18 17.2 21.6 12 21.6C6.8 21.6 2.6 18 2.6 13.2Z ; F M6.4 13.2V8C6.4 6.6 7.2 5.4 8 4.6C8.8 5.4 9.6 6.6 9.6 8V13.2Z M10.4 13.2V6C10.4 4.6 11.2 3.4 12 2.6C12.8 3.4 13.6 4.6 13.6 6V13.2Z M14.4 13.2V8C14.4 6.6 15.2 5.4 16 4.6C16.8 5.4 17.6 6.6 17.6 8V13.2Z ; X M3 13.4H21"),
            // Flame
            Map.entry("tower.flame", "F M12 2C12 2 19.5 8 19.5 14.2A7.5 7.5 0 0 1 4.5 14.2C4.5 10.8 6.6 8.6 8.2 7.2C8.6 9.6 9.8 11 11 11.4C10.4 8.2 10.9 4.8 12 2Z ; K M12 9.6C12 9.6 16.2 13 16.2 16A4.2 4.2 0 0 1 7.8 16C7.8 13 12 9.6 12 9.6Z ; D M12 11.4C12 11.4 14.8 13.8 14.8 16A2.8 2.8 0 0 1 9.2 16C9.2 13.8 12 11.4 12 11.4Z"),
            // Stream
            Map.entry("tower.stream", "F M5.4 5.6H14.6L18.6 9.6V20.4A1.2 1.2 0 0 1 17.4 21.6H6.6A1.2 1.2 0 0 1 5.4 20.4Z ; K M7.8 7.4H11.8V9.4H7.8Z ; K M12 11.6C12 11.6 15 14.8 15 16.6A3 3 0 0 1 9 16.6C9 14.8 12 11.6 12 11.6Z ; S M16.4 5.6L18.4 3.2"),
            // Core
            Map.entry("tower.core", "F M9.6 5.6C9.6 5.6 15.4 10.2 15.4 15.2A5.8 5.8 0 0 1 3.8 15.2C3.8 12.4 5.4 10.4 6.8 9.4C7.2 11.4 8 12.4 9 12.8C8.6 10.2 8.8 7.6 9.6 5.6Z ; F M18.6 1.8L19.8 5.4L23.4 6.6L19.8 7.8L18.6 11.4L17.4 7.8L13.8 6.6L17.4 5.4Z"),
            // Dragon's Breath
            Map.entry("tower.dragons_breath", "F M1.8 12C5.6 7.8 12 6 22.4 3.6C18.6 8 18.6 16 22.4 20.4C12 18 5.6 16.2 1.8 12Z ; K M6 12C8.6 10.2 12.6 9.2 17.4 8.6C16 10.8 16 13.2 17.4 15.4C12.6 14.8 8.6 13.8 6 12Z ; D M8.8 12C10.6 11.2 13 10.8 15.2 10.8C14.6 11.6 14.6 12.4 15.2 13.2C13 13.2 10.6 12.8 8.8 12Z"),
            // Flashover
            Map.entry("tower.flashover", "F M12 1.6L14 7.2L19.4 4.6L16.8 10L22.4 12L16.8 14L19.4 19.4L14 16.8L12 22.4L10 16.8L4.6 19.4L7.2 14L1.6 12L7.2 10L4.6 4.6L10 7.2Z ; K M12 8.6C12 8.6 15 11.2 15 13.4A3 3 0 0 1 9 13.4C9 11.2 12 8.6 12 8.6Z"),
            // Thermal Shock
            Map.entry("tower.thermal_shock", "F M8.6 3C8.6 3 13.6 7.6 13.6 12.4A5 5 0 0 1 3.6 12.4C3.6 9.6 5.2 7.8 6.4 6.8C6.8 8.8 7.6 9.8 8.4 10.2C8 7.8 8.2 5 8.6 3Z ; T M17 10V21.6 M12 12.9L22 18.7 M12 18.7L22 12.9"),
            // Meltdown
            Map.entry("tower.meltdown", "S O12 12 9.4 ; F M12 5.4C12 5.4 16.4 9.4 16.4 13A4.4 4.4 0 0 1 7.6 13C7.6 11 8.8 9.6 9.8 8.8C10 10.2 10.6 11 11.4 11.2C11 9.2 11.2 7 12 5.4Z"),
            // Sundial
            Map.entry("tower.sundial", "S O12 12 9 ; F M12 12L18.4 5.6A9 9 0 0 1 21 12Z ; F O12 12 1.6"),
            // Frailty
            Map.entry("tower.frailty", "S M5.6 3H18.4C18.4 8.6 15.6 12.2 12 12.2C8.4 12.2 5.6 8.6 5.6 3Z M12 12.2V19.6 M7.6 20.6H16.4 ; T M12.4 3.4L10.8 6L13 7.8L11.6 10.4"),
            // Shadow
            Map.entry("tower.shadow", "S O12 12 8.8 ; F M12 12V3.2A8.8 8.8 0 0 1 19.6 7.6Z ; F O12 12 1.6"),
            // Kinship
            Map.entry("tower.kinship", "T M12 5.6L5.6 17.6H18.4Z ; F O12 5.4 3 ; F M2.6 15.2H8.6V21.2H2.6Z ; F M18.4 14.4L21.6 20.6H15.2Z"),
            // Shatter
            Map.entry("tower.shatter", "F O12 12 1.8 ; F M12 9L10.4 2.2L14.2 3.2Z M14.8 11L21.6 8.2L20.8 12.4Z M14.2 14.4L18 20.8L13.8 20.4Z M9.8 14.6L6.2 21.2L4.4 17.6Z M9.2 10.8L2.6 8.4L4.2 4.8Z"),
            // Arc
            Map.entry("tower.arc", "F O4.6 18.6 2.6 O19.4 5.4 2.6 ; S M6.6 16.8L10.2 15.4L9 11.6L14 10.8L12.8 7.2L17.4 6.8"),
            // Hex
            Map.entry("tower.hex", "S O12 12 9 ; S M12 19.6L5 7.4H19Z ; F O12 11.2 1.8"),
            // Static
            Map.entry("tower.static", "=tower.arc"),
            // Soulbrand
            Map.entry("tower.soulbrand", "=tower.hex"),
            // Spark
            Map.entry("tower.spark", "=effect.charged"),
            // Soul
            Map.entry("tower.soul", "=effect.soulfire"),
            // Thunderclap
            Map.entry("tower.thunderclap", "F M13.4 3L8.4 12.4H11.6L10.4 21L15.8 10.8H12.6Z ; S M6.4 8A6 6 0 0 0 6.4 16 M17.6 8A6 6 0 0 1 17.6 16 ; T M3 5.4A9.6 9.6 0 0 0 3 18.6 M21 5.4A9.6 9.6 0 0 1 21 18.6"),
            // Thunderstrike
            Map.entry("tower.thunderstrike", "F M6.4 13C4.4 13 3 11.6 3 9.8C3 8 4.4 6.6 6.2 6.6C6.8 4.4 8.8 3 11.2 3C14 3 16.2 5 16.6 7.6C18.8 7.6 20.6 9.2 20.6 11.2C20.6 13 19.2 13 17.4 13Z ; F M12.8 13.8L9.4 18.6H12L10.8 22.4L15.6 16.6H12.8L14.6 13.8Z"),
            // Carpet Bombing
            Map.entry("tower.barrage", "F O6 9 2.4 O12 13 2.4 O18 17 2.4 ; T M6 3.4V5.4 M12 7.4V9.4 M18 11.4V13.4"),
            Map.entry("tower.carpet_bombing", "F O7.4 16.4 3.8 O16.8 14.6 3.4 O11.2 6.8 2.8 ; T M9.8 13.4L10.6 11.6 M18.6 11.8L19.6 10.2 M12.6 4.4L13.6 2.8"));

    private TowerTermPictograms() {
    }
}
