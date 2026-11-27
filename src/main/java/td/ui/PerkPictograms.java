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
            // Heavy Rounds
            Map.entry("sniper.heavy_rounds", "F M5.4 21.6V11.8C5.4 6.8 8.2 3.2 12 1.6C15.8 3.2 18.6 6.8 18.6 11.8V21.6Z ; X M5.4 15H18.6 M5.4 18.2H18.6"),
            // Executioner
            Map.entry("sniper.executioner", "S M3.6 20.6L14 8.2 ; F M10.8 2.8C15.6 3 19.8 6.6 20.8 11.4L13.8 12.6L10.6 9.6Z"),
            // Keen Eye
            Map.entry("sniper.keen_eye", "S M2.4 12C6 6.2 18 6.2 21.6 12C18 17.8 6 17.8 2.4 12Z ; S O12 12 3.4 ; T M12 6.4V17.6 M6.4 12H17.6"),
            // Unbroken Aim
            Map.entry("sniper.unbroken_aim", "S M3.6 12C3.6 9.4 5.4 7.8 7.6 7.8C11.4 7.8 12.6 16.2 16.4 16.2C18.6 16.2 20.4 14.6 20.4 12C20.4 9.4 18.6 7.8 16.4 7.8C12.6 7.8 11.4 16.2 7.6 16.2C5.4 16.2 3.6 14.6 3.6 12Z"),
            // Exposing Crits
            Map.entry("sniper.exposing_crits", "=effect.exposed"),
            // Hard Rounds
            Map.entry("sniper.hard_rounds", "F M8.4 21.6V10.4C8.4 6.6 10 3.6 12 2.2C14 3.6 15.6 6.6 15.6 10.4V21.6Z ; X M8.4 13.6H15.6 ; F M6.6 16.4H17.4V18.6H6.6Z"),
            // Killing Spree
            Map.entry("sniper.killing_spree", "F O15.4 12 5.4 ; T M1.8 8.6H8 M3.4 12H8.6 M1.8 15.4H8 ; K M13.4 9.4A3 3 0 0 1 17.8 9.6"),
            // Ricochet
            Map.entry("sniper.ricochet", "T M2.6 20.6H21.4 ; S M3.4 4.6L10.4 18.4L16.6 8.2 ; F M19.2 4.2L14.6 6.6L18.6 10.2Z ; F O10.4 18.4 1.6"),
            // Headhunter
            Map.entry("sniper.headhunter", "F M4.4 17.6L3.4 7.6L8.4 11.6L12 4.8L15.6 11.6L20.6 7.6L19.6 17.6Z ; F M4.4 19.2H19.6V21.4H4.4Z ; K O12 13.2 1.8"),
            // Hollow Point
            Map.entry("sniper.hollow_point", "=tower.hollow_point"),
            // Fourth Shot
            Map.entry("sniper.fourth_shot", "F M2.8 6.4H5V17.6H2.8Z M7 6.4H9.2V17.6H7Z M11.2 6.4H13.4V17.6H11.2Z ; F M18.4 4.6L19.8 9.8L21.8 12L19.8 14.2L18.4 19.4L17 14.2L15 12L17 9.8Z"),
            // Deadeye
            Map.entry("sniper.deadeye", "F M1.6 9.4H8.6C11 9.4 12.8 10.6 13.8 12C12.8 13.4 11 14.6 8.6 14.6H1.6Z ; X M5 9.4V14.6 ; F M18.4 2.8L19.9 10.3L22.8 12L19.9 13.7L18.4 21.2L16.9 13.7L14 12L16.9 10.3Z"),
            // Rifling
            Map.entry("sniper.rifling", "S O12 12 8.8 ; F O12 12 2.4 ; T M12 3.2C15.8 5.4 16.4 8.4 14 10.2 M20.8 12C18.6 15.8 15.6 16.4 13.8 14 M12 20.8C8.2 18.6 7.6 15.6 10 13.8 M3.2 12C5.4 8.2 8.4 7.6 10.2 10"),
            // Steady Aim
            Map.entry("sniper.steady_aim", "=tower.steady_aim"),
            // Deep Breath
            Map.entry("sniper.deep_breath", "=tower.steady_aim"),
            // Hemorrhage
            Map.entry("sniper.hemorrhage", "=effect.bleeding"),
            // Silver Round
            Map.entry("sniper.silver_round", "=tower.silver_round"),
            // Long Barrel II
            Map.entry("base.range.2", "=base.range"),
            // Flint Stones
            Map.entry("tier.1", "S M5 15L12 8L19 15"),
            Map.entry("tier.2", "S M5 11L12 4L19 11 ; S M5 19L12 12L19 19"),
            Map.entry("tier.3", "S M5 8L12 3L19 8 ; S M5 14L12 9L19 14 ; S M5 20L12 15L19 20"),
            Map.entry("splash.sharp_stones", "F M12 2.4L19.6 12L12 21.6L4.4 12Z ; X M12 6.4V17.6 M8.2 12H15.8"),
            Map.entry("splash.flint_stones", "F M3.4 17.6L7.6 6.4L13.6 2.8L20.6 9.4L18.4 18.8L10.4 21.4Z ; X M7.6 6.4L11.6 12.2L20.6 9.4 M11.6 12.2L10.4 21.4"),
            // Stormcaller
            Map.entry("splash.stormcaller", "F M7 20.4H17V22.4H7Z ; F M9.8 10.4H14.2V20.4H9.8Z ; X M9.8 13.2H14.2 M9.8 15.8H14.2 M9.8 18.4H14.2 ; F O12 6.8 3.4 ; T M15.4 5.4L17.8 3.6L18.2 6L21.2 4.4 M8.6 5.4L6.2 3.6L5.8 6L2.8 4.4"),
            // Hexer
            Map.entry("splash.hexer", "F O12 9 6.6 ; X M12 13.2L8.4 6.8H15.6Z ; S M5.6 12.4C6.6 16.4 9 17.8 12 17.8C15 17.8 17.4 16.4 18.4 12.4 M12 17.8V20.6 ; F M8 20.4H16V22.4H8Z"),
            // Arc
            Map.entry("splash.arc", "=tower.arc"),
            // Hex of Doom
            Map.entry("splash.hex_of_doom", "=effect.doom"),
            // Hex of Blight
            Map.entry("splash.hex_of_blight", "=effect.blight"),
            // Reckoning
            Map.entry("splash.reckoning", "F M 12 2.6 C 7.2 2.6 4 6.2 4 10.4 C 4 13.2 5.4 15.2 7.4 16.2 V 19.6 C 7.4 20.4 8 21.2 9 21.2 H 15 C 16 21.2 16.6 20.4 16.6 19.6 V 16.2 C 18.6 15.2 20 13.2 20 10.4 C 20 6.2 16.8 2.6 12 2.6 Z ; K O8.9 11.0 2.1 ; K O15.1 11.0 2.1 ; T O12 12 11.4"),
            // Blight Cloud
            Map.entry("splash.blight_cloud", "F O8 15 4 O13 11.6 5.2 O18 15.6 3.6 M4 15H22V19H4Z ; K O9 16.4 1 O13.6 15 1.1 O17 17.4 0.9 ; F M5.4 8.4C5.4 5.6 7.4 3.8 10 3.6C10 6.4 8.2 8.4 5.4 8.4Z"),
            // Contagion
            Map.entry("splash.contagion", "F O12 12 4.6 ; S M12 7V3.6 M12 17V20.4 M7.67 9.5L4.72 7.8 M16.33 14.5L19.28 16.2 M7.67 14.5L4.72 16.2 M16.33 9.5L19.28 7.8 ; F O12 3.2 1.7 O12 20.8 1.7 O4.38 7.6 1.7 O19.62 16.4 1.7 O4.38 16.4 1.7 O19.62 7.6 1.7"),
            // Chain Lightning
            Map.entry("splash.chain_lightning", "S M3 3.4L8.8 10L5.6 12.8L11.4 20.6 ; S M8.8 10L15 9L13 13L20.8 15.2 ; S M15 9L20.6 3.4 ; F O11.4 20.6 1.6 O20.8 15.2 1.6 O20.6 3.4 1.6"),
            // Wild Arc
            Map.entry("splash.wild_arc", "F O4.6 16.4 2.8 O19.4 16.4 2.8 ; S M4.6 13.6L7.4 6.4L10.6 9.4L13.4 3.4L19.4 13.6 ; S M17 20C14.4 22.4 9.8 22.4 7.6 20.4 ; F M4.6 22.6L5.4 17.4L10 20.6Z"),
            // Soul Harvest
            Map.entry("splash.soul_harvest", "F M11 2.4C7.4 7 5.6 10.2 5.6 13.4A5.4 5.4 0 0 0 16.4 13.4C16.4 10.2 14.6 7 11 2.4Z ; K O8.8 13 1.1 O13.2 13 1.1 ; S M3 21.4C10 21.4 18 18.4 21.2 8.4 ; F M21.8 5.2L22.4 10.6L17.8 8.2Z"),
            // Hex of Backlash
            Map.entry("splash.hex_of_backlash", "=effect.backlash"),
            // Choking Hexes
            Map.entry("splash.choking_hexes", "F M3 17C3 9.4 7.6 4.4 16 4C16 8 15 11 13.2 13.2C12 13.2 11 14 11 15.2V17C8 17.6 5.4 17.6 3 17Z ; X M3 17L11.6 8.4 ; S M12.6 14H21.6V19.6H17.6L14.8 22V19.6H12.6Z ; F M14.6 16.2H19.6V17.4H14.6Z"),
            // Twin Hex
            Map.entry("splash.twin_hex", "S O8 8.4 5.2 ; S M8 12.8L3.9 5.7H12.1Z ; F O8 7.9 1.1 ; S O16 15.6 5.2 ; S M16 20L11.9 12.9H20.1Z ; F O16 15.1 1.1"),
            // Shockwave
            Map.entry("splash.shockwave", "F M13.4 3L8.4 12.4H11.6L10.4 21L15.8 10.8H12.6Z ; S M6.4 8A6 6 0 0 0 6.4 16 M17.6 8A6 6 0 0 1 17.6 16 ; T M3 5.4A9.6 9.6 0 0 0 3 18.6 M21 5.4A9.6 9.6 0 0 1 21 18.6"),
            // Thunderstrike
            Map.entry("splash.thunderstrike", "F M6.4 13C4.4 13 3 11.6 3 9.8C3 8 4.4 6.6 6.2 6.6C6.8 4.4 8.8 3 11.2 3C14 3 16.2 5 16.6 7.6C18.8 7.6 20.6 9.2 20.6 11.2C20.6 13 19.2 13 17.4 13Z ; F M12.8 13.8L9.4 18.6H12L10.8 22.4L15.6 16.6H12.8L14.6 13.8Z"),
            // Hard Stones
            Map.entry("splash.hard_stones", "F M4 17.4L6.4 8.2L12.6 4.4L19.4 8L20.4 16.6L13.2 20.6Z ; X M6.4 8.2L12.4 11.6L19.4 8 M12.4 11.6L13.2 20.6 ; T M3 21.6H21"),
            // Conductor
            Map.entry("splash.conductor", "F O5 6.6 2.4 O19 6.6 2.4 ; S M7.4 6.6L10.4 11L13.6 4.8L16.6 6.6 ; F M12 13.4L13.2 16.6L16.4 17.8L13.2 19L12 22.2L10.8 19L7.6 17.8L10.8 16.6Z"),
            // High Voltage
            Map.entry("splash.high_voltage", "F M9.2 2.6L3.4 12.6H7.6L6.4 21.4L12.4 10.6H8.2Z ; F M17.6 6L12.4 14.8H16L15 21.6L20.8 12.4H17Z"),
            // Feedback
            Map.entry("splash.feedback", "S M17.4 7.6A7.4 7.4 0 1 0 19.4 13.4 ; F M21.6 3.8L21 9.6L15.6 7.4Z ; F M13.4 7.6L9.6 12.6H12.2L11 17.2L15.2 11.6H12.6Z"),
            Map.entry("splash.wide_blast", "F O10.4 14.2 6.8 ; S M15 9.6C16 7.6 17.2 6 19 5.2 ; F M20.4 1.8L21.1 3.7L23 4.4L21.1 5.1L20.4 7L19.7 5.1L17.8 4.4L19.7 3.7Z ; K O8 11.8 1.6"),
            // Packed Charge
            Map.entry("mortar.hard_casings", "F M12 2.8C14.8 5 17.4 7.4 17.4 11V21H6.6V11C6.6 7.4 9.2 5 12 2.8Z ; X M9.2 18L12 15.2L14.8 18 M9.2 13.6L12 10.8L14.8 13.6"),
            Map.entry("mortar.packed_charge", "F M6 21.6H18C19.6 21.6 20.4 20.4 20 18.8L17.6 9.6H6.4L4 18.8C3.6 20.4 4.4 21.6 6 21.6Z ; F M8.4 6.6H15.6V9H8.4Z ; F M9.4 6.6L8 2.8L12 4.6L16 2.8L14.6 6.6Z ; X M12 12.6V19 M8.8 15.8H15.2"),
            // Heavy Charge
            // Wide Crater
            Map.entry("mortar.wide_crater", "F M2.6 17.4A9.4 3.6 0 0 1 21.4 17.4A9.4 3.6 0 0 1 2.6 17.4Z ; K M7 17.2A5 1.6 0 0 1 17 17.2A5 1.6 0 0 1 7 17.2Z ; S M9 11.4L4.4 6.8 M15 11.4L19.6 6.8 ; F M2.6 5L7.6 5.6L3.2 10Z M21.4 5L16.4 5.6L20.8 10Z"),
            // Big Bore
            Map.entry("mortar.big_bore", "F M8 20.4H16L18 7H6Z ; F M4.4 7A7.6 2.8 0 0 1 19.6 7A7.6 2.8 0 0 1 4.4 7Z ; K M7.4 7A4.6 1.4 0 0 1 16.6 7A4.6 1.4 0 0 1 7.4 7Z ; F M5.6 20.4H18.4V22.6H5.6Z"),
            Map.entry("mortar.heavy_rack", "F M3.6 19.4V11C3.6 8.8 4.6 6.8 5.4 5.4C6.2 6.8 7.2 8.8 7.2 11V19.4Z M10.2 19.4V11C10.2 8.8 11.2 6.8 12 5.4C12.8 6.8 13.8 8.8 13.8 11V19.4Z M16.8 19.4V11C16.8 8.8 17.8 6.8 18.6 5.4C19.4 6.8 20.4 8.8 20.4 11V19.4Z ; F M2.4 19.8H21.6V22H2.4Z"),
            // Predictive Fire
            Map.entry("mortar.predictive_fire", "S M3.2 19.6C5.4 7 14.6 5 19.4 14.8 ; S O19.4 16.4 3 ; T M19.4 11.4V13 M19.4 19.8V21.4 M14.4 16.4H16 M22.8 16.4H24"),
            // Concussion
            Map.entry("mortar.concussion", "=effect.dazed"),
            // Shrapnel
            Map.entry("mortar.shrapnel", "F O9 10.4 2.2 ; F M3.4 3.4L6.8 4.4L4.4 6.8Z M14.6 3.4L13.6 6.8L11.2 4.4Z M3.4 17.4L4.4 14L6.8 16.4Z ; T M9 6.4V3 M9 14.4V17.6 M5 10.4H2 M13 10.4H16 ; F M17.6 11.6C17.6 11.6 21.6 16.2 21.6 18.6A4 4 0 0 1 13.6 18.6C13.6 16.2 17.6 11.6 17.6 11.6Z"),
            // Napalm, Tar, Cryo
            Map.entry("mortar.napalm", "=tower.napalm_shell"),
            Map.entry("mortar.coated_shells", "F O12 9.4 6.4 ; K O9.8 7.2 1.6 ; F M7.6 16C7.6 16 10 18.8 10 20.2A2.4 2.4 0 0 1 5.2 20.2C5.2 18.8 7.6 16 7.6 16Z M16.4 16.8C16.4 16.8 18.6 19.4 18.6 20.6A2.2 2.2 0 0 1 14.2 20.6C14.2 19.4 16.4 16.8 16.4 16.8Z M12 17.2C12 17.2 13.4 18.8 13.4 19.8A1.4 1.4 0 0 1 10.6 19.8C10.6 18.8 12 17.2 12 17.2Z"),
            Map.entry("mortar.tar_shell", "=tower.tar_shell"),
            Map.entry("mortar.cryo", "=tower.cryo_shell"),
            // Flare
            Map.entry("mortar.flare", "=effect.revealed"),
            // Barrage
            Map.entry("mortar.barrage", "=tower.barrage"),
            // Carpet Bombing
            Map.entry("mortar.carpet_bombing", "=tower.carpet_bombing"),
            // Quick Load
            Map.entry("mortar.quick_load", "F M9 8.4H15.4C18.6 8.4 21.4 10 22.4 12C21.4 14 18.6 15.6 15.4 15.6H9Z ; F M6.8 8.4H8.2V15.6H6.8Z ; T M1.4 9.6H5.2 M2.4 12H5.4 M1.4 14.4H5.2"),
            // Overcharge
            Map.entry("mortar.overcharge", "F M8.6 2.4H15.4V11.6L12 17L8.6 11.6Z ; X M8.6 6.4H15.4 ; S M2.4 19H21.6 ; T M7 19L5.2 22.4 M17 19L18.8 22.4 M12 19V22.6"),
            // Fusion
            Map.entry("mortar.fusion", "=tower.nuke_shell"),
            // Rich Mix
            Map.entry("mortar.rich_mix", "F O7.4 13 3.6 ; S O16.6 13 3.6 ; T M7.4 6.4V8 M16.6 6.4V8"),
            // Heavy Powder
            Map.entry("mortar.heavy_powder", "F M6.4 21.6H17.6L16.4 9.6H7.6Z ; F M9.4 9.6V6.6H14.6V9.6Z ; X M8.8 14.4H15.2 M8.4 17.6H15.6 ; F O12 4 1.6"),
            // Deep Crack
            Map.entry("mortar.lasting_mix", "F M3.6 20.4H10V12C10 8.6 8.6 6.2 6.8 4.8C5 6.2 3.6 8.6 3.6 12Z ; F M13.6 8.4H20.8L17.2 14.4Z M13.6 20.4H20.8L17.2 14.4Z ; T M12.8 7.6H21.6 M12.8 21.2H21.6"),
            Map.entry("mortar.deep_crack", "F M5 4H19A1.5 1.5 0 0 1 20.5 5.5V18.5A1.5 1.5 0 0 1 19 20H5A1.5 1.5 0 0 1 3.5 18.5V5.5A1.5 1.5 0 0 1 5 4Z ; K O6.6 7.1 1 O17.4 7.1 1 O6.6 16.9 1 O17.4 16.9 1 ; X M9.6 4L8 8.8L10.6 12L8.4 16.4L9.4 20 M15 4L13.4 8.4L16 11.8L13.6 16L14.8 20"),
            // Hot Flame
            Map.entry("cinder.hot_flame", "=tower.flame"),
            // Long Stream
            Map.entry("cinder.long_stream", "=tower.stream"),
            // Wide Nozzle
            Map.entry("cinder.wide_nozzle", "F M2.4 9.6H7.4V14.4H2.4Z ; F M7.4 10.2L21.6 3.4V20.6L7.4 13.8Z ; X M10.6 12H21.6 M10.2 10.6L21.6 7.2 M10.2 13.4L21.6 16.8"),
            // Core
            Map.entry("cinder.core", "=tower.core"),
            // Fan
            // Soulfire
            Map.entry("cinder.soulfire", "=effect.soulfire"),
            // Dragon's Breath
            Map.entry("cinder.dragons_breath", "=tower.dragons_breath"),
            // Blue Flame
            Map.entry("cinder.blue_flame", "=tower.flame"),
            // Flashover
            Map.entry("cinder.flashover", "=tower.flashover"),
            // Meltdown
            Map.entry("cinder.meltdown", "=tower.meltdown"),
            // Thermal Shock
            Map.entry("cinder.thermal_shock", "=tower.thermal_shock"),
            // Hot Flame II
            Map.entry("cinder.hot_flame_2", "=tower.flame"),
            // Long Stream II
            Map.entry("cinder.long_stream_2", "=cinder.long_stream"),
            // Hum
            Map.entry("pulse.hum", "F@-40 M7 3.4H17V9.4H7Z M10.9 9.4H13.1V18H10.9Z ; F M4 19.4H14V22H4Z"),
            // Sundial
            Map.entry("pulse.sundial", "=tower.sundial"),
            // Ward
            Map.entry("pulse.ward", "=effect.silenced"),
            // Quick Toll
            Map.entry("pulse.quick_toll", "S O12 12 9 ; S M12 6.4V12L16 14.4"),
            // Deep Pull
            Map.entry("pulse.deep_pull", "S M12 12C12 10.6 13.4 10 14.4 10.8C15.8 12 15 14.6 12.8 15C10 15.6 8.4 12.8 9.2 10.4C10.2 7.4 14.4 6.4 17 8.4C20 10.8 19.6 15.8 16.4 18.2C12.4 21.2 6 19.6 4 15C2 10.2 5 4.6 10.2 3.6"),
            // Burden
            Map.entry("pulse.burden", "=pulse.deep_pull"),
            // Gravity
            Map.entry("pulse.gravity", "S O12 12 9 ; F M8.4 7.4H10.9V16.6H8.4Z M13.1 7.4H15.6V16.6H13.1Z"),
            // Umbra
            Map.entry("pulse.umbra", "S O12 12 8.6 ; F O14.6 10.4 6.4"),
            // Hum II
            Map.entry("pulse.hum_2", "=pulse.hum"),
            // Deep Shadow
            Map.entry("pulse.deep_shadow", "=tower.shadow"),
            // Knell
            Map.entry("pulse.knell", "F M12 3.2C8.4 3.2 6.2 6.2 6.2 9.6V14L4 16.8V17.6H20V16.8L17.8 14V9.6C17.8 6.2 15.6 3.2 12 3.2Z ; F O12 19.6 2.2 ; S M12 3.2V1.8 ; S M2 6.4C2.8 4.6 3.8 3.4 5.2 2.6 M22 6.4C21.2 4.6 20.2 3.4 18.8 2.6"),
            // Deep Toll
            Map.entry("pulse.deep_toll", "=effect.toll"),
            // Deep Pull II
            Map.entry("pulse.deep_pull_2", "=pulse.deep_pull"),
            // Swift Shadow
            Map.entry("pulse.swift_shadow", "=tower.sundial"),
            // Steady Light
            Map.entry("aura.steady_light", "F M12 10.4L15.6 21.4H13.4L12 16.8L10.6 21.4H8.4Z ; F O12 8.4 2 ; S M7.6 4.6A6.2 6.2 0 0 0 7.6 12.2 M16.4 4.6A6.2 6.2 0 0 1 16.4 12.2 ; T M4.2 2A10.2 10.2 0 0 0 4.2 14.8 M19.8 2A10.2 10.2 0 0 1 19.8 14.8"),
            // Afterglow
            Map.entry("aura.afterglow", "F M12 20.4L2.8 9.4A13 13 0 0 1 21.2 9.4Z ; X M7.2 14.6A7 7 0 0 1 16.8 14.6 M4.6 11.6A10.6 10.6 0 0 1 19.4 11.6"),
            // Searchlight
            Map.entry("aura.searchlight", "=effect.priority"),
            // Last Light
            Map.entry("aura.last_light", "S M3.2 8.4C4.8 15.2 10 19.6 17.2 20.4 ; S M3.2 8.4L17.2 20.4 ; T M10.2 14.4L16.6 8 ; F O17.4 7.2 2 ; F M9 20.6H14V22.4H9Z"),
            // Ringleader
            Map.entry("aura.ringleader", "=effect.priority"),
            // Kinship
            Map.entry("aura.kinship", "F M12 7.4L16.6 12L12 16.6L7.4 12Z ; S M12 1.6L9.6 4 M12 1.6L14.4 4 M12 22.4L9.6 20 M12 22.4L14.4 20 M1.6 12L4 9.6 M1.6 12L4 14.4 M22.4 12L20 9.6 M22.4 12L20 14.4"),
            // Mentor
            Map.entry("aura.mentor", "=tower.mentor"),
            // Breakthrough
            Map.entry("aura.breakthrough", "=tower.breakthrough"),
            // Daylight
            Map.entry("aura.daylight", "=tower.daylight"),
            // Glare
            Map.entry("aura.glare", "=effect.exposed"),
            // Sanctuary
            Map.entry("aura.sanctuary", "=aura.steady_light"),
            // Prism
            Map.entry("aura.prism", "=tower.prism"),
            // Deep Freeze
            Map.entry("seeker.deep_freeze", "=effect.freeze"),
            // Wide Comb
            Map.entry("seeker.wide_comb", "F M4.4 20.4V9.6C4.4 6.6 5.8 4.4 7.6 2.8C9.4 4.4 10.8 6.6 10.8 9.6V20.4Z M13.2 20.4V9.6C13.2 6.6 14.6 4.4 16.4 2.8C18.2 4.4 19.6 6.6 19.6 9.6V20.4Z ; F M4.4 20.4L2.6 22.2V17.6L4.4 15.8Z M10.8 20.4L12.6 22.2V17.6L10.8 15.8Z M19.6 20.4L21.4 22.2V17.6L19.6 15.8Z ; X M4.4 9.4H10.8 M13.2 9.4H19.6"),
            // Volley
            Map.entry("seeker.volley", "=tower.volley"),
            // Hatching
            Map.entry("seeker.hatching", "=tower.nest"),
            // Feeding
            Map.entry("seeker.feeding", "F M5.4 20.6V10.2C5.4 7.4 6.8 5.2 8.6 3.6C10.4 5.2 11.8 7.4 11.8 10.2V20.6Z ; X M5.4 10.6H11.8 ; S M15.4 19.6A6.2 6.2 0 0 0 17.6 7.6 ; F M14.6 5.2L19.8 4.6L18.4 9.8Z"),
            // Wide Comb II
            Map.entry("seeker.wide_comb_2", "F M2 20.4V9.6C2 6.6 3 4.4 4.3 2.8C5.6 4.4 6.6 6.6 6.6 9.6V20.4Z M9.7 20.4V9.6C9.7 6.6 10.7 4.4 12 2.8C13.3 4.4 14.3 6.6 14.3 9.6V20.4Z M17.4 20.4V9.6C17.4 6.6 18.4 4.4 19.7 2.8C21 4.4 22 6.6 22 9.6V20.4Z ; X M2 9.4H6.6 M9.7 9.4H14.3 M17.4 9.4H22"),
            // Ruby
            Map.entry("seeker.ruby", "=tower.ruby"),
            // Shade
            Map.entry("seeker.shade", "=tower.shade"),
            // Sharp Sting
            Map.entry("seeker.sharp_sting", "F M12 2.6L15 14.4H9Z ; F O12 18 3.2"),
            // Ice Wake
            Map.entry("seeker.ice_wake", "=effect.chill"),
            // Shatter
            Map.entry("seeker.shatter", "=tower.shatter"),
            // Rich Brood
            Map.entry("seeker.rich_brood", "F@-40 M3 10.4H15L19.8 12L15 13.6H3Z M3.6 10.4L2 7.6H5.6L7.4 10.4Z M3.6 13.6L2 16.4H5.6L7.4 13.6Z ; F M15.2 16.6L16 18.6L18 19.4L16 20.2L15.2 22.2L14.4 20.2L12.4 19.4L14.4 18.6Z ; F M21 11.8L18 15.8H20L19.2 19.2L22.4 14.8H20.4Z"),
            // Deep Freeze II
            Map.entry("seeker.deep_freeze_2", "S M3 6L12 2L21 6V18L12 22L3 18Z ; F M12 8L16.5 10.2V15L12 17.2L7.5 15V10.2Z"),
            // Quick Brood: an egg with its speed streaks
            Map.entry("seeker.quick_brood", "F M15 3.6C19 3.6 22 9 22 14C22 18.6 19 21.4 15 21.4C11 21.4 8 18.6 8 14C8 9 11 3.6 15 3.6Z ; T M2.4 10H6 M1.6 13H5.4 M2.4 16H6"),
            // Sharp Sting II
            Map.entry("seeker.sharp_sting_2", "F M8 2.6L10.8 13.4H5.2Z M16 2.6L18.8 13.4H13.2Z ; F O8 18 2.6 ; F O16 18 2.6"));

    private PerkPictograms() {
    }
}
