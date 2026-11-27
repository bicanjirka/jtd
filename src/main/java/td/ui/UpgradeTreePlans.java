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
            tier(row(cluster("On crit", 0f), node("sniper.rifling", 2.6f), node("sniper.steady_aim", 4.4f))),
            tier(row(node("tier.1", 0f), node("sniper.fourth_shot", 1.8f), node("sniper.deadeye", 2.9f),
                    cluster("Aim", 3.9f))),
            tier(row(node("tier.2", 0f), cluster("Ammo", 1.3f), node("sniper.exposing_crits", 4f),
                    node("sniper.hard_rounds", 5.2f))),
            tier(row(node("tier.3", 0f), node("sniper.heavy_rounds", 1.3f), cluster("Finisher", 2.4f),
                    node("sniper.killing_spree", 4.5f), node("sniper.unbroken_aim", 5.6f))));

    private static final UpgradeTreePlan SPLASH = UpgradeTreePlan.of(
            tier(row(node("base.range", 0f), cluster("Stones", 1.5f))),
            tier(row(node("tier.1", 0f), node("splash.arc", 1f), cluster("Stormcaller or Hexer", 2.4f), cluster("Hex", 4.9f))),
            tier(row(node("tier.2", 0f), node("base.range.2", 1f), node("splash.hard_stones", 2.4f),
                    node("splash.conductor", 3.4f), node("splash.contagion", 3.4f),
                    cluster("Arc's path", 4.4f), node("splash.reckoning", 4.4f), node("splash.blight_cloud", 5.4f))),
            tier(row(node("tier.3", 0f), node("splash.wide_blast", 1f),
                    node("splash.high_voltage", 2.4f), node("splash.soul_harvest", 2.4f), node("splash.feedback", 3.4f),
                    node("splash.hex_of_backlash", 3.4f), cluster("Storm", 4.4f), node("splash.twin_hex", 4.4f),
                    node("splash.choking_hexes", 5.4f))));

    private static final UpgradeTreePlan MORTAR = UpgradeTreePlan.of(
            tier(row(node("mortar.big_bore", 0f), node("mortar.predictive_fire", 1f),
                    cluster("Shell charge", 2.3f))),
            tier(row(node("tier.1", 0f), cluster("Heavy shell", 1f), node("mortar.flare", 3.1f), cluster("Payload", 4.2f)),
                    row(node("mortar.heavy_powder", 3.1f))),
            tier(row(node("tier.2", 0f), node("mortar.overcharge", 1f), cluster("Heavy or Normal", 2.6f),
                    node("mortar.deep_crack", 4.6f), node("mortar.coated_shells", 5.7f))),
            tier(row(node("tier.3", 0f), cluster("Loading", 1f), cluster("Order", 4.1f),
                    node("mortar.lasting_mix", 6.2f))));

    private static final UpgradeTreePlan CINDER = UpgradeTreePlan.of(
            tier(row(node("cinder.hot_flame", 0f), node("cinder.long_stream", 1f), node("cinder.wide_nozzle", 2f),
                    node("base.range", 3f))),
            tier(row(node("cinder.core", 0f), cluster("Nozzle", 1.3f), node("cinder.kindling", 3.6f))),
            tier(row(cluster("Flame", 0f), node("cinder.white_flame", 2.3f), node("cinder.bellows", 3.3f))),
            tier(row(cluster("Burning", 0f), node("cinder.hot_flame_2", 3.3f), node("cinder.long_stream_2", 4.3f))));

    private static final UpgradeTreePlan SEEKER = UpgradeTreePlan.of(
            tier(row(node("seeker.deep_freeze", 0f), node("seeker.wide_comb", 1f))),
            tier(row(node("tier.1", 0f), cluster("Release", 1f), node("seeker.sharp_sting", 3f),
                    node("seeker.wide_comb_2", 4f))),
            tier(row(node("tier.2", 0f), node("seeker.feeding", 1f), node("base.range.2", 2f),
                    node("seeker.deep_freeze_2", 3f), cluster("Second missile", 4f))),
            tier(row(node("tier.3", 0f), node("seeker.rich_brood", 1f), cluster("Around the ice", 2f),
                    cluster("Apex", 4f))));

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
