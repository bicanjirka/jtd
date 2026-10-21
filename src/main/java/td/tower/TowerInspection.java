package td.tower;

import td.stat.DisruptionPenalty;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeState;

import java.util.List;

/**
 * An immutable snapshot of one tower for display: its stats as authored and as they are now, what
 * it does, what it has achieved and what changes it. Read anywhere once taken.
 *
 * @param name        what the tower is called; a forked tower may be called something other than its type
 * @param stats       range first, then damage, cadence, crit chance and the tower's own stats
 * @param behaviours  what the tower does beyond its stats
 * @param description what no row says, for the shop; empty when the rows say it all
 * @param damageDealt damage that landed, in hundredths
 * @param auras       how many other towers buff this one
 * @param lines       the owned upgrade nodes, grouped by line
 * @param conditions  the buffs and debuffs it carries now; empty unless the frame build filled it
 * @param progress    how far each counting node is toward its next go; empty unless the frame build filled it
 * @param targeting   whom it aims at, as its row words it; empty for a tower that aims at no one
 * @param applies     every effect it puts on enemies or gives towers, with what applies it
 */
public record TowerInspection(TowerFactory.Type type, String name, int price, List<TowerStatLine> stats,
                                  List<BehaviourLine> behaviours, String description, int kills, long damageDealt,
                                  UpgradeState upgrades, DisruptionPenalty disruption, int auras, int xp,
                                  List<UpgradeLine> lines, List<TowerBuffState> conditions, List<NodeProgress> progress, String targeting,
                              List<AppliedEffect> applies) {

    public TowerInspection {
        stats = List.copyOf(stats);
        behaviours = List.copyOf(behaviours);
        lines = List.copyOf(lines);
        conditions = List.copyOf(conditions);
        progress = List.copyOf(progress);
        applies = List.copyOf(applies);
    }

    /** A tower as priced and authored, with nothing done and nothing changing it yet. */
    public static TowerInspection of(TowerFactory.Type type, int price, List<TowerStatLine> stats) {
        return new TowerInspection(type, type.displayName(), price, stats, List.of(), "", 0, 0L, UpgradeState.none(), DisruptionPenalty.none(), 0, 0, List.of(), List.of(), List.of(), "", List.of());
    }

    public TowerInspection withBehaviours(List<BehaviourLine> behaviours) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withDescription(String description) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    /** The same tower at {@code price}: the shop's current price for its type. */
    public TowerInspection withPrice(int price) {
        return new TowerInspection(this.type, this.name, price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    /** The XP the tower has earned. */
    public TowerInspection withXp(int xp) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withName(String name) {
        return new TowerInspection(this.type, name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withUpgrades(UpgradeState upgrades) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withDisruption(DisruptionPenalty disruption) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, disruption, this.auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withAuras(int auras) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, auras, this.xp, this.lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    /** What the tower has achieved: kills and damage landed, in hundredths. */
    public TowerInspection withRecord(int kills, long damageDealt) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   kills, damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines, this.conditions,
                   this.progress, this.targeting, this.applies);
    }

    public TowerInspection withLines(List<UpgradeLine> lines) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, lines,
                   this.conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withConditions(List<TowerBuffState> conditions) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   conditions, this.progress, this.targeting, this.applies);
    }

    public TowerInspection withProgress(List<NodeProgress> progress) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                   this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                   this.conditions, progress, this.targeting, this.applies);
    }

    public TowerInspection withTargeting(String targeting) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, targeting, this.applies);
    }

    public TowerInspection withApplies(List<AppliedEffect> applies) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.behaviours, this.description,
                this.kills, this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, applies);
    }
}
