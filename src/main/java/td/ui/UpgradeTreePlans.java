package td.ui;

import td.tower.TowerFactory;

import static td.ui.UpgradeTreePlan.cluster;
import static td.ui.UpgradeTreePlan.node;
import static td.ui.UpgradeTreePlan.row;
import static td.ui.UpgradeTreePlan.tier;

/**
 * Every tower's hand-placed Upgrades panel. Where a tree holds two builds, one build's nodes lean left, the other's
 * right, and what serves both sits between; the lean wins over a straight link. Where the builds are the two members
 * of a cluster, their nodes may share slots instead, so each tier keeps to one row. A slot runs from 0 to a little
 * over 6.
 */
final class UpgradeTreePlans {

    private static final UpgradeTreePlan SNIPER = UpgradeTreePlan.of(
            tier(row(node("sniper.rifling", 0f), node("sniper.heavy_rounds", 1.2f), node("sniper.steady_aim", 3.3f))),
            tier(row(node("sniper.fourth_shot", 0.5f), node("sniper.killshot", 1.5f), cluster("Steady Aim", 2.8f))),
            tier(row(cluster("Ammo", 0f), cluster("On crit", 2.3f), node("base.range.2", 5.6f))),
            tier(row(cluster("Finisher", 0f), node("sniper.heavy_rounds.2", 2.5f), node("sniper.unbroken_aim", 5f),
                    node("sniper.momentum", 6f))));

    private static final UpgradeTreePlan SPLASH = UpgradeTreePlan.of(
            tier(row(node("base.range", 0f), node("splash.wide_charge", 1f), node("splash.flint_stones", 2f))),
            tier(row(node("splash.arc", 0f), cluster("Stormcaller or Hexer", 1.6f), cluster("Hex", 4f))),
            tier(row(node("base.range.2", 0f), node("splash.heavy_stones", 1f), node("splash.short_circuit", 2.4f),
                    node("splash.agony", 2.4f), node("splash.conductor", 3.4f), node("splash.contagion", 3.4f),
                    cluster("Arc's path", 4.4f), node("splash.deep_doom", 4.4f), node("splash.blight_cloud", 5.4f))),
            tier(row(node("splash.quick_hands", 0f), node("splash.piercing_stones", 1f),
                    node("splash.high_voltage", 2.4f), node("splash.soul_harvest", 2.4f), node("splash.feedback", 3.4f),
                    node("splash.hex_of_inversion", 3.4f), cluster("Storm", 4.4f), node("splash.twin_hex", 4.4f),
                    node("splash.choking_blight", 5.4f))));

    private static final UpgradeTreePlan MORTAR = UpgradeTreePlan.of(
            tier(row(node("mortar.big_bore", 0f), node("base.range", 2f), node("mortar.heavy_charge", 3f),
                    node("mortar.predictive_fire", 4f))),
            tier(row(cluster("Heavy shell", 0f), node("mortar.flare", 2.6f), cluster("Payload", 4.25f))),
            tier(row(node("mortar.overcharge", 0f), node("mortar.big_bore_2", 1f), cluster("Volley", 2.3f),
                    node("mortar.thick_mix", 5.25f))),
            tier(row(cluster("Order", 0f), node("mortar.deep_crack", 2.25f), node("mortar.quick_reload", 3.25f),
                    node("base.range.2", 4.25f), node("mortar.lasting_mix", 5.25f))));

    private static final UpgradeTreePlan CINDER = UpgradeTreePlan.of(
            tier(row(node("cinder.hot_flame", 0f), node("cinder.long_stream", 1f), node("cinder.wide_nozzle", 2f),
                    node("base.range", 3f))),
            tier(row(node("cinder.core", 0f), cluster("Nozzle", 1.3f), node("cinder.kindling", 3.6f))),
            tier(row(cluster("Flame", 0f), node("cinder.white_flame", 2.3f), node("cinder.bellows", 3.3f))),
            tier(row(cluster("Burning", 0f), node("cinder.hot_flame_2", 3.3f), node("cinder.long_stream_2", 4.3f))));

    private static final UpgradeTreePlan SEEKER = UpgradeTreePlan.of(
            tier(row(node("seeker.deep_freeze", 0f), node("base.range", 2f), node("seeker.quick_brood", 4f),
                    node("seeker.wide_comb", 5f))),
            tier(row(node("seeker.feeding", 0f), node("seeker.swift_wings", 1.3f), cluster("Release", 2.6f),
                    node("seeker.wide_comb_2", 5f))),
            tier(row(node("seeker.sharp_sting", 2.3f), node("base.range.2", 3.3f), cluster("Second missile", 4.6f))),
            tier(row(cluster("Around the ice", 0f), node("seeker.deep_freeze_2", 2.3f),
                    node("seeker.quick_brood_2", 4.6f), node("seeker.rich_brood", 5.8f))));

    private static final UpgradeTreePlan PULSE = UpgradeTreePlan.of(
            tier(row(node("base.range", 2.5f), node("pulse.hum", 3.5f))),
            tier(row(node("pulse.sundial", 0f), node("pulse.ward", 4f), node("pulse.quick_toll", 5f),
                    node("pulse.deep_pull", 6f))),
            tier(row(node("pulse.deep_shadow", 0f), cluster("The stone", 1.4f), node("pulse.hum_2", 4.7f),
                    node("base.range.2", 5.7f))),
            tier(row(node("pulse.swift_shadow", 0f), cluster("Payoff", 2f), node("pulse.deep_pull_2", 6f))));

    private static final UpgradeTreePlan AURA = UpgradeTreePlan.of(
            tier(row(node("aura.steady_light", 2.5f), node("aura.afterglow", 3.5f))),
            tier(row(cluster("Spotlight", 0f), cluster("Lessons", 4.25f))),
            tier(row(cluster("Edge of light", 0f))),
            tier(row(node("aura.sanctuary", 2.5f), node("aura.prism", 3.5f))));

    private UpgradeTreePlans() {
    }

    static UpgradeTreePlan of(TowerFactory.Type type) {
        return switch (type) {
            case SNIPER -> SNIPER;
            case SPLASH -> SPLASH;
            case MORTAR -> MORTAR;
            case CINDER -> CINDER;
            case SEEKER -> SEEKER;
            case PULSE -> PULSE;
            case AURA -> AURA;
        };
    }
}
