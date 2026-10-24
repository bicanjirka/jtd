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
            // Awaken
            Map.entry("base.awaken", "S M2.2 12C6 5.6 18 5.6 21.8 12C18 18.4 6 18.4 2.2 12Z ; F O12 12 3.4 ; T M12 2.4V4.6 M5.6 4.2L6.8 6 M18.4 4.2L17.2 6"),
            // Transcendent
            Map.entry("base.transcendent", "F M3.4 18.6L4.6 7.4L8.8 12.4L12 4.6L15.2 12.4L19.4 7.4L20.6 18.6Z ; T M3.4 21H20.6"),
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
