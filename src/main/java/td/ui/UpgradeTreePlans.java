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
            tier(row(cluster("On crit", 1.3f), node("sniper.rifling", 3.4f), node("sniper.steady_aim", 4.4f))),
            tier(row(node("tier.1", 0f), node("sniper.fourth_shot", 1.8f), node("sniper.deadeye", 2.9f),
                    cluster("Aim", 3.9f))),
            tier(row(node("tier.2", 0f), cluster("Ammo", 1.3f), node("sniper.exposing_crits", 4f),
                    node("sniper.hard_rounds", 5.2f))),
            tier(row(node("tier.3", 0f), node("sniper.heavy_rounds", 1.3f), cluster("Finisher", 2.4f),
                    node("sniper.killing_spree", 4.5f), node("sniper.unbroken_aim", 5.6f))));

    private static final UpgradeTreePlan SPLASH = UpgradeTreePlan.of(
            tier(row(node("base.range", 1.3f), cluster("Stones", 2.7f))),
            tier(row(node("tier.1", 0f), node("splash.arc", 1.3f), cluster("Stormcaller or Hexer", 2.7f),
                    cluster("Hex", 4.9f))),
            tier(row(node("tier.2", 0f), node("base.range.2", 1.3f), node("splash.conductor", 3.7f),
                    node("splash.contagion", 3.7f), cluster("Arc's path", 4.9f), node("splash.reckoning", 4.9f),
                    node("splash.blight_cloud", 5.9f))),
            tier(row(node("tier.3", 0f), node("splash.hard_stones", 1.3f),
                    node("splash.high_voltage", 2.7f), node("splash.soul_harvest", 2.7f), node("splash.feedback", 3.7f),
                    node("splash.hex_of_backlash", 3.7f), cluster("Storm", 4.9f), node("splash.twin_hex", 4.9f),
                    node("splash.choking_hexes", 5.9f))));

    private static final UpgradeTreePlan MORTAR = UpgradeTreePlan.of(
            tier(row(node("mortar.big_bore", 1.3f), node("mortar.predictive_fire", 2.4f),
                    cluster("Shell charge", 3.5f))),
            tier(row(node("tier.1", 0f), cluster("Heavy shell", 1.3f), node("mortar.flare", 3.35f),
                    cluster("Payload", 4.35f))),
            tier(row(node("tier.2", 0f), node("mortar.overcharge", 1.3f), cluster("Heavy or Normal", 2.6f),
                    node("mortar.deep_crack", 4.6f), node("mortar.coated_shells", 6.35f))),
            tier(row(node("tier.3", 0f), cluster("Loading", 1.3f), cluster("Order", 4.4f),
                    node("mortar.lasting_mix", 6.35f))));

    private static final UpgradeTreePlan CINDER = UpgradeTreePlan.of(
            tier(row(node("cinder.hot_flame", 1.8f), node("cinder.long_stream", 3.4f))),
            tier(row(node("tier.1", 0f), node("cinder.core", 1.8f), cluster("Nozzle", 3.4f))),
            tier(row(node("tier.2", 0f), cluster("Flame", 1.8f), node("cinder.hot_flame_2", 4.6f))),
            tier(row(node("tier.3", 0f), cluster("Burning", 1.8f), node("cinder.blue_flame", 5.4f))));

    private static final UpgradeTreePlan SEEKER = UpgradeTreePlan.of(
            tier(row(node("seeker.deep_freeze", 1.8f), node("seeker.wide_comb", 3.2f))),
            tier(row(node("tier.1", 0f), cluster("Release", 1.3f), node("seeker.sharp_sting", 3.5f),
                    node("seeker.wide_comb_2", 4.6f))),
            tier(row(node("tier.2", 0f), node("seeker.feeding", 1.3f), node("base.range.2", 2.5f),
                    node("seeker.deep_freeze_2", 3.6f), cluster("Second missile", 4.7f))),
            tier(row(node("tier.3", 0f), cluster("Around the ice", 1.5f), cluster("Apex", 3.8f),
                    node("seeker.rich_brood", 6.047f))));

    private static final UpgradeTreePlan PULSE = UpgradeTreePlan.of(
            tier(row(node("base.range", 1.8f), node("pulse.hum", 3.3f))),
            tier(row(node("pulse.sundial", 1.3f), node("pulse.ward", 3.3f), node("pulse.quick_toll", 4.5f),
                    node("pulse.deep_pull", 5.7f))),
            tier(row(node("pulse.deep_shadow", 1.3f), cluster("The stone", 2.4f), node("pulse.hum_2", 5.4f),
                    node("base.range.2", 6.4f))),
            tier(row(node("pulse.swift_shadow", 1.3f), cluster("Payoff", 2.6f), node("pulse.deep_pull_2", 5.7f))));

    private static final UpgradeTreePlan AURA = UpgradeTreePlan.of(
            tier(row(node("aura.lantern", 2.5f), node("aura.afterglow", 3.5f))),
            tier(row(node("tier.1", 0f), cluster("Spotlight", 1.2f), cluster("Lessons", 4.35f))),
            tier(row(node("tier.2", 0f), cluster("Edge of light", 1.2f))),
            tier(row(node("tier.3", 0f), node("aura.sanctuary", 2.5f), node("aura.prism", 3.5f))));

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
