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
                    new Strategy("doom", Set.of("base.range", "splash.hexer", "splash.hex_of_doom", "splash.reckoning", "splash.contagion", "splash.twin_hex", "base.range.2", "splash.soul_harvest", "splash.wide_blast", "splash.hard_stones")),
                    new Strategy("blight", Set.of("base.range", "splash.hexer", "splash.hex_of_blight", "splash.blight_cloud", "splash.choking_hexes", "splash.twin_hex", "base.range.2", "splash.soul_harvest", "splash.wide_blast", "splash.hard_stones")),
                    new Strategy("shockwave", Set.of("base.range", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "base.range.2",
                            "splash.shockwave", "splash.high_voltage",
                            "splash.wide_blast", "splash.hard_stones")),
                    new Strategy("thunderstrike", Set.of("base.range", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "base.range.2",
                            "splash.thunderstrike", "splash.high_voltage",
                            "splash.wide_blast", "splash.hard_stones"))),
            TowerFactory.Type.SNIPER, List.of(
                    new Strategy("crit", Set.of("sniper.steady_aim", "sniper.rifling",
                            "sniper.fourth_shot", "sniper.keen_eye", "sniper.deadeye", "sniper.hollow_point",
                            "sniper.ricochet", "sniper.headhunter",
                            "sniper.heavy_rounds", "sniper.hard_rounds")),
                    new Strategy("speed", Set.of("sniper.steady_aim", "sniper.rifling",
                            "sniper.fourth_shot", "sniper.deadeye", "sniper.deep_breath",
                            "sniper.silver_round", "sniper.ricochet", "sniper.executioner",
                            "sniper.killing_spree", "sniper.unbroken_aim", "sniper.hard_rounds"))),
            TowerFactory.Type.SEEKER, aerie(),
            TowerFactory.Type.MORTAR, mortar()));

    /** Focus (one enemy at a time), Volley (many at once) and Wither (long holds, no healing), each with its stronger Apex. */
    private static List<Strategy> aerie() {
        Set<String> focus = Set.of("seeker.wide_comb", "seeker.hatching", "seeker.sharp_sting", "seeker.wide_comb_2",
                "seeker.feeding", "base.range.2", "seeker.ruby", "seeker.rich_brood", "seeker.shatter");
        Set<String> volley = Set.of("seeker.wide_comb", "seeker.volley", "seeker.sharp_sting", "seeker.wide_comb_2",
                "seeker.feeding", "base.range.2", "seeker.shade", "seeker.rich_brood", "seeker.shatter");
        Set<String> wither = Set.of("seeker.deep_freeze", "seeker.wide_comb", "seeker.hatching", "seeker.sharp_sting",
                "seeker.wide_comb_2", "seeker.feeding", "base.range.2", "seeker.deep_freeze_2", "seeker.shade",
                "seeker.rich_brood", "seeker.ice_wake");
        return List.of(new Strategy("focus", with(focus, "seeker.quick_brood")),
                new Strategy("volley", with(volley, "seeker.quick_brood")),
                new Strategy("wither", with(wither, "seeker.sharp_sting_2")));
    }

    private static Set<String> with(Set<String> nodes, String added) {
        Set<String> all = new LinkedHashSet<>(nodes);
        all.add(added);
        return Set.copyOf(all);
    }

    private static final String PREDICTIVE = "mortar.predictive_fire";
    private static final String WIDE_CRATER = "mortar.wide_crater";
    private static final String HEAVY_POWDER = "mortar.heavy_powder";
    private static final String CONCUSSION = "mortar.concussion";
    private static final String SHRAPNEL = "mortar.shrapnel";
    private static final String OVERCHARGE = "mortar.overcharge";

    /**
     * Nuke, Heavy shells (to tier II with both Big Bores, then the whole build without them), a Payload per kind and
     * Normal shells loaded fast; every one with Concussion also with Shrapnel, and the Payloads also without Concussion
     * or Overcharge.
     */
    private static List<Strategy> mortar() {
        Set<String> nuke = Set.of(WIDE_CRATER, "mortar.big_bore", HEAVY_POWDER, CONCUSSION, OVERCHARGE, "mortar.heavy_rack",
                "mortar.barrage", "mortar.fusion", "mortar.deep_crack");
        Set<String> bores = Set.of(WIDE_CRATER, "mortar.big_bore", HEAVY_POWDER, CONCUSSION, OVERCHARGE,
                "mortar.heavy_rack");
        Set<String> heavy = Set.of(WIDE_CRATER, "mortar.big_bore", "mortar.heavy_rack", HEAVY_POWDER, CONCUSSION, OVERCHARGE, "mortar.barrage",
                "mortar.rich_mix", "mortar.deep_crack");
        Set<String> normal = Set.of(PREDICTIVE, "mortar.flare", "mortar.packed_charge", "mortar.hard_casings",
                "mortar.quick_load", HEAVY_POWDER);
        List<Strategy> strategies = new ArrayList<>(List.of(new Strategy("nuke", nuke), bled("nuke", nuke),
                new Strategy("heavy-bores", bores), bled("heavy-bores", bores),
                new Strategy("heavy", heavy), bled("heavy", heavy), new Strategy("normal", normal)));
        for (String payload : List.of("napalm", "tar_shell", "cryo")) {
            Set<String> nodes = Set.of("mortar.packed_charge", "mortar.hard_casings", HEAVY_POWDER, CONCUSSION, "mortar." + payload, OVERCHARGE,
                    "mortar.carpet_bombing", "mortar.coated_shells", "mortar.rich_mix", "mortar.deep_crack",
                    "mortar.lasting_mix");
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
