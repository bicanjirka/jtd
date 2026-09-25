package td.tower;

import td.stat.DisruptionPenalty;
import td.tower.upgrade.UpgradeState;

import java.util.List;

/**
 * An immutable snapshot of one tower for display: its stats as authored and as they are now, what
 * it does, what it has achieved and what changes it. Read anywhere once taken.
 *
 * @param stats       range first, then damage, cadence, crit chance and the tower's own stats
 * @param behaviours  what the tower does beyond its stats
 * @param description what no row says, for the shop; empty when the rows say it all
 * @param damageDealt damage that landed, in hundredths
 * @param auras       how many other towers buff this one
 */
public record TowerInspection(TowerFactory.Type type, int price, int sellPrice, List<TowerStatLine> stats,
                              List<BehaviourLine> behaviours, String description, int kills, long damageDealt,
                              UpgradeState upgrades, DisruptionPenalty disruption, int auras) {

    public TowerInspection {
        stats = List.copyOf(stats);
        behaviours = List.copyOf(behaviours);
    }

    /** A tower as priced and authored, with nothing done and nothing changing it yet. */
    public static TowerInspection of(TowerFactory.Type type, int price, List<TowerStatLine> stats) {
        return new TowerInspection(type, price, price, stats, List.of(), "", 0, 0L, UpgradeState.none(),
                DisruptionPenalty.none(), 0);
    }

    public TowerInspection withSellPrice(int sellPrice) {
        return new TowerInspection(this.type, this.price, sellPrice, this.stats, this.behaviours, this.description, this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras);
    }

    public TowerInspection withBehaviours(List<BehaviourLine> behaviours) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, behaviours, this.description, this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras);
    }

    public TowerInspection withDescription(String description) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, this.behaviours, description, this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras);
    }

    /** What the tower has achieved: kills and damage landed, in hundredths. */
    public TowerInspection withRecord(int kills, long damageDealt) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, this.behaviours, this.description, kills, damageDealt, this.upgrades, this.disruption, this.auras);
    }

    public TowerInspection withUpgrades(UpgradeState upgrades) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, this.behaviours, this.description, this.kills, this.damageDealt, upgrades, this.disruption, this.auras);
    }

    public TowerInspection withDisruption(DisruptionPenalty disruption) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, this.behaviours, this.description, this.kills, this.damageDealt, this.upgrades, disruption, this.auras);
    }

    public TowerInspection withAuras(int auras) {
        return new TowerInspection(this.type, this.price, this.sellPrice, this.stats, this.behaviours, this.description, this.kills, this.damageDealt, this.upgrades, this.disruption, auras);
    }
}
