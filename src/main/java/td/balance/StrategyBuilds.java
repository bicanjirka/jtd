package td.balance;

import td.tower.TowerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The builds a tower's retune names as its strategies, each a set of nodes bought whole: the bench measures them beside
 * the builds it reads off the tree, since a strategy cuts across tiers and clusters.
 */
final class StrategyBuilds {

    /** One named build: the nodes it buys, whatever their tier. */
    record Strategy(String name, Set<String> nodes) {
    }

    private static final Map<TowerFactory.Type, List<Strategy>> STRATEGIES = new EnumMap<>(Map.of(
            TowerFactory.Type.SPLASH, List.of(
                    new Strategy("doom", Set.of("base.range", "splash.hexer", "splash.hex_of_doom", "splash.reckoning", "splash.agony", "splash.contagion", "splash.twin_hex", "base.range.2", "splash.soul_harvest", "splash.wide_blast")),
                    new Strategy("blight", Set.of("base.range", "splash.hexer", "splash.hex_of_blight", "splash.blight_cloud", "splash.agony", "splash.choking_hexes", "splash.twin_hex", "base.range.2", "splash.soul_harvest", "splash.wide_blast")),
                    new Strategy("shockwave", Set.of("base.range", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "base.range.2",
                            "splash.shockwave", "splash.high_voltage",
                            "splash.wide_blast")),
                    new Strategy("thunderstrike", Set.of("base.range", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "base.range.2",
                            "splash.thunderstrike", "splash.high_voltage",
                            "splash.wide_blast"))),
            TowerFactory.Type.SNIPER, List.of(
                    new Strategy("crit", Set.of("sniper.steady_aim", "sniper.rifling",
                            "sniper.fourth_shot", "sniper.keen_eye", "sniper.deadeye", "sniper.hollow_point",
                            "sniper.ricochet", "sniper.sundering_rounds", "sniper.headhunter",
                            "sniper.heavy_rounds")),
                    new Strategy("speed", Set.of("sniper.steady_aim", "sniper.rifling",
                            "sniper.fourth_shot", "sniper.deadeye", "sniper.deep_breath",
                            "sniper.silver_round", "sniper.ricochet", "sniper.sundering_rounds", "sniper.executioner",
                            "sniper.killing_spree", "sniper.unbroken_aim"))),
            TowerFactory.Type.MORTAR, mortar()));

    private static final String CRACKING = "mortar.cracking_shells";
    private static final String CONCUSSION = "mortar.concussion";
    private static final String SHRAPNEL = "mortar.shrapnel";
    private static final String OVERCHARGE = "mortar.overcharge";

    /**
     * Nuke, Heavy shells (to tier II with both Big Bores, then the whole build without them), a Payload per kind and
     * Normal shells loaded fast; every one with Concussion also with Shrapnel, and the Payloads also without Concussion
     * or Overcharge.
     */
    private static List<Strategy> mortar() {
        Set<String> nuke = Set.of("mortar.big_bore", "mortar.heavy_charge", CRACKING, CONCUSSION, OVERCHARGE, "mortar.big_bore_2",
                "base.range", "mortar.barrage", "mortar.fusion", "mortar.deep_crack", "mortar.quick_reload");
        Set<String> bores = Set.of("mortar.big_bore", "mortar.heavy_charge", CRACKING, CONCUSSION, OVERCHARGE,
                "mortar.big_bore_2", "base.range");
        Set<String> heavy = Set.of("mortar.big_bore", "mortar.big_bore_2", "mortar.heavy_charge", CRACKING, CONCUSSION, OVERCHARGE, "base.range", "mortar.barrage",
                "mortar.rich_mix", "mortar.deep_crack", "mortar.quick_reload");
        Set<String> normal = Set.of("mortar.predictive_fire", "mortar.flare", "mortar.packed_charge", CRACKING, "base.range",
                "mortar.deep_crack", "mortar.quick_load", "mortar.quick_reload");
        List<Strategy> strategies = new ArrayList<>(List.of(new Strategy("nuke", nuke), bled("nuke", nuke),
                new Strategy("heavy-bores", bores), bled("heavy-bores", bores),
                new Strategy("heavy", heavy), bled("heavy", heavy), new Strategy("normal", normal)));
        for (String payload : List.of("napalm", "tar_shell", "cryo")) {
            Set<String> nodes = Set.of("mortar.heavy_charge", CRACKING, CONCUSSION, "mortar." + payload, OVERCHARGE, "base.range",
                    "mortar.barrage", "mortar.thick_mix", "mortar.rich_mix", "mortar.deep_crack",
                    "mortar.quick_reload", "mortar.lasting_mix");
            strategies.addAll(List.of(new Strategy(payload, nodes), bled(payload, nodes),
                    new Strategy(payload + "-no-concussion", without(nodes, CONCUSSION)),
                    new Strategy(payload + "-no-overcharge", without(nodes, OVERCHARGE))));
        }
        return List.copyOf(strategies);
    }

    /** {@code nodes} with Shrapnel's Bleed in place of Concussion's Daze. */
    private static Strategy bled(String name, Set<String> nodes) {
        Set<String> swapped = new LinkedHashSet<>(without(nodes, CONCUSSION));
        swapped.add(SHRAPNEL);
        return new Strategy(name + "-bleed", Set.copyOf(swapped));
    }

    private static Set<String> without(Set<String> nodes, String left) {
        return nodes.stream().filter(node -> !node.equals(left)).collect(Collectors.toUnmodifiableSet());
    }

    private StrategyBuilds() {
    }

    /** The strategies named for {@code type}, none if its retune has named none. */
    static List<Strategy> of(TowerFactory.Type type) {
        return STRATEGIES.getOrDefault(type, List.of());
    }
}
