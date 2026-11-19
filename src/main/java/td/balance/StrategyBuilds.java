package td.balance;

import td.tower.TowerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    new Strategy("doom", Set.of("base.range", "splash.wide_charge", "splash.hexer", "splash.hex_of_doom", "splash.reckoning", "splash.agony", "splash.contagion", "splash.twin_hex", "splash.heavy_stones", "base.range.2", "splash.soul_harvest", "splash.quick_hands", "splash.piercing_stones")),
                    new Strategy("blight", Set.of("base.range", "splash.wide_charge", "splash.hexer", "splash.hex_of_blight", "splash.blight_cloud", "splash.agony", "splash.choking_blight", "splash.twin_hex", "splash.heavy_stones", "base.range.2", "splash.soul_harvest", "splash.quick_hands", "splash.piercing_stones")),
                    new Strategy("shockwave", Set.of("base.range", "splash.wide_charge", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "splash.heavy_stones", "base.range.2",
                            "splash.shockwave", "splash.high_voltage", "splash.quick_hands",
                            "splash.piercing_stones")),
                    new Strategy("thunderstrike", Set.of("base.range", "splash.wide_charge", "splash.stormcaller",
                            "splash.arc", "splash.chain_lightning", "splash.heavy_stones", "base.range.2",
                            "splash.thunderstrike", "splash.high_voltage", "splash.quick_hands",
                            "splash.piercing_stones"))),
            TowerFactory.Type.SNIPER, List.of(
                    new Strategy("crit", Set.of("sniper.steady_aim", "sniper.rifling", "sniper.heavy_rounds", "base.range",
                            "sniper.fourth_shot", "sniper.keen_eye", "sniper.killshot", "sniper.hollow_point",
                            "sniper.ricochet", "sniper.sundering_crits", "sniper.headhunter",
                            "sniper.heavy_rounds.2")),
                    new Strategy("speed", Set.of("sniper.steady_aim", "sniper.rifling", "sniper.heavy_rounds",
                            "sniper.fourth_shot", "sniper.killshot", "sniper.deep_breath", "base.range",
                            "sniper.silver_round", "sniper.ricochet", "sniper.sundering_crits", "sniper.executioner",
                            "sniper.killing_spree", "sniper.unbroken_aim", "sniper.heavy_rounds.2")))));

    private StrategyBuilds() {
    }

    /** The strategies named for {@code type}, none if its retune has named none. */
    static List<Strategy> of(TowerFactory.Type type) {
        return STRATEGIES.getOrDefault(type, List.of());
    }
}
