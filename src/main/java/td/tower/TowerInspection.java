package td.tower;

import td.stat.DisruptionPenalty;
import td.stat.StatModifiers;
import td.tower.ability.AbilityState;
import td.tower.mortar.MagazineView;
import td.tower.seeker.NestView;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A tower as the info panel shows it: each row with its base, what changed it and what it is now, what it applies, what it holds and
 * how far its counting nodes are. Taken on the game-loop thread and read anywhere.
 *
 * @param description what no row says, for the shop; empty when the rows say it all
 * @param damageDealt damage that landed, in hundredths
 * @param auras       how many other towers buff this one
 * @param lines       the owned upgrade nodes, grouped by line
 * @param conditions  the buffs and debuffs it carries now; empty unless the frame build filled it
 * @param progress    how far each counting node is toward its next go; empty unless the frame build filled it
 * @param targeting   whom it aims at, as its row words it; empty for a tower that aims at no one
 * @param applies     every effect it puts on enemies or gives towers, with what applies it
 * @param words       what each owned node says, by node id
 * @param gives       what it hands the towers around it
 * @param abilities   every ability it owns, in the order bought, with its bar; empty unless the frame build filled it
 * @param magazine    a Mortar's shells and their chances; empty for any other tower, or unless the frame build filled it
 * @param nest        an Aerie's missiles and their chances; empty for any other tower, or unless the frame build filled it
 */
public record TowerInspection(TowerFactory.Type type, String name, int price, List<TowerStatLine> stats,
                              String description, int kills, long damageDealt, UpgradeState upgrades,
                              DisruptionPenalty disruption, int auras, int xp, List<UpgradeLine> lines,
                              List<TowerBuffState> conditions, List<NodeProgress> progress, String targeting,
                              List<AppliedEffect> applies, Map<String, NodeWords> words,
                              StatModifiers<TowerStat> gives, List<AbilityState> abilities,
                              Optional<MagazineView> magazine, Optional<NestView> nest) {

    public TowerInspection {
        stats = List.copyOf(stats);
        lines = List.copyOf(lines);
        conditions = List.copyOf(conditions);
        progress = List.copyOf(progress);
        applies = List.copyOf(applies);
        words = Map.copyOf(words);
        abilities = List.copyOf(abilities);
    }

    /** A tower as priced and authored, with nothing done and nothing changing it yet. */
    public static TowerInspection of(TowerFactory.Type type, int price, List<TowerStatLine> stats) {
        return new TowerInspection(type, type.displayName(), price, stats, "", 0, 0L, UpgradeState.none(),
                DisruptionPenalty.none(), 0, 0, List.of(), List.of(), List.of(), "", List.of(), Map.of(),
                StatModifiers.none(), List.of(), Optional.empty(), Optional.empty());
    }

    public TowerInspection withDescription(String description) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    /** The same tower at {@code price}: the shop's current price for its type. */
    public TowerInspection withPrice(int price) {
        return new TowerInspection(this.type, this.name, price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    /** The XP the tower has earned. */
    public TowerInspection withXp(int xp) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, xp, this.lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withName(String name) {
        return new TowerInspection(this.type, name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withUpgrades(UpgradeState upgrades) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, upgrades, this.disruption, this.auras, this.xp, this.lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withDisruption(DisruptionPenalty disruption) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, disruption, this.auras, this.xp, this.lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withAuras(int auras) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, auras, this.xp, this.lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withLines(List<UpgradeLine> lines) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withConditions(List<TowerBuffState> conditions) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines, conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withProgress(List<NodeProgress> progress) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withTargeting(String targeting) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withApplies(List<AppliedEffect> applies) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withWords(Map<String, NodeWords> words) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, words, this.gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withGives(StatModifiers<TowerStat> gives) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, gives, this.abilities, this.magazine, this.nest);
    }

    public TowerInspection withAbilities(List<AbilityState> abilities) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, abilities, this.magazine, this.nest);
    }

    public TowerInspection withMagazine(MagazineView magazine) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities,
                Optional.of(magazine), this.nest);
    }

    public TowerInspection withNest(NestView nest) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, this.kills,
                this.damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines,
                this.conditions, this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities,
                this.magazine, Optional.of(nest));
    }

    /** What the tower has achieved: kills and damage landed, in hundredths. */
    public TowerInspection withRecord(int kills, long damageDealt) {
        return new TowerInspection(this.type, this.name, this.price, this.stats, this.description, kills,
                damageDealt, this.upgrades, this.disruption, this.auras, this.xp, this.lines, this.conditions,
                this.progress, this.targeting, this.applies, this.words, this.gives, this.abilities, this.magazine, this.nest);
    }
}
