package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.Tower;
import td.tower.TowerCapability;
import td.tower.TowerStat;
import td.tower.proc.HitBonus;
import td.tower.proc.Proc;
import td.tower.targeting.TargetPriority;
import td.util.GameWorld;


/**
 * One node in a tower's upgrade tree. Hooks and prerequisites match nodes by their {@code id},
 * unique within one tree, since a branching graph can reconverge.
 * <p>
 * {@code requires} decides whether the node is offered at all. Once offered, it is bought when the
 * tower has {@code xp} XP and its {@code gate} (a purpose or layout condition) is met: see
 * {@link #gateMet}. The UI hides an unreachable node and shows a gated one with its progress.
 */
public record UpgradeNode(String id, UpgradeSlot slot, String displayName, int price, NodeEffects effects,
                          UpgradeCondition requires, UpgradeCondition gate, int xp, UpgradeTier tier) {

    /** A node gated on price alone; add the rest with the {@code withX} copies. */
    public static UpgradeNode of(String id, UpgradeSlot slot, String displayName, int price) {
        return new UpgradeNode(id, slot, displayName, price, NodeEffects.none(), UpgradeCondition.always(),
                UpgradeCondition.always(), 0, UpgradeTier.startOf(slot));
    }

    /** This node with {@code modifiers} added to what it does to its tower's stats. */
    public UpgradeNode withModifiers(StatModifiers<TowerStat> modifiers) {
        return this.withEffects(this.effects.withModifiers(modifiers));
    }

    /** This node with {@code emission} added to what it gives the towers an aura buffs. */
    public UpgradeNode withEmission(StatModifiers<TowerStat> emission) {
        return this.withEffects(this.effects.withEmission(emission));
    }

    public UpgradeNode withCapability(TowerCapability capability) {
        return this.withEffects(this.effects.withCapability(capability));
    }

    public UpgradeNode withTargeting(TargetPriority priority) {
        return this.withEffects(this.effects.withTargeting(priority));
    }

    public UpgradeNode withHitBonus(HitBonus bonus) {
        return this.withEffects(this.effects.withHitBonus(bonus));
    }

    public UpgradeNode withProc(Proc proc) {
        return this.withEffects(this.effects.withProc(proc));
    }

    /** This node with {@code effects} as everything it does. */
    public UpgradeNode withEffects(NodeEffects effects) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, effects, this.requires,
                this.gate, this.xp, this.tier);
    }

    /** This node at {@code tier}, the step of its line it is built from. */
    public UpgradeNode withTier(UpgradeTier tier) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.effects, this.requires,
                this.gate, this.xp, tier);
    }

    public UpgradeNode withRequires(UpgradeCondition requires) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.effects, requires,
                this.gate, this.xp, this.tier);
    }

    public UpgradeNode withGate(UpgradeCondition gate) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.effects, this.requires,
                gate, this.xp, this.tier);
    }

    /** The XP a tower needs before it can buy this node. */
    public UpgradeNode withXp(int xp) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.effects, this.requires,
                this.gate, xp, this.tier);
    }

    /** This node, offered only once {@code previous} (the level before it in its line) is owned too. */
    public UpgradeNode after(UpgradeNode previous) {
        return this.withRequires(this.requires.and(UpgradeCondition.owns(previous.id())));
    }

    /** This node with {@code note} as the words for what its data can't say. */
    public UpgradeNode withNote(String note) {
        return this.withEffects(this.effects.withNote(note));
    }

    /**
     * Whether {@code tower} has this node's XP and meets its gate, or playtesting ignores both:
     * price aside, it may buy it.
     */
    public boolean gateMet(Tower tower, GameWorld context) {
        return context.playtestRules().upgradeGatesIgnored()
                || (this.xpMet(tower) && this.gate.isSatisfied(tower, context));
    }

    public boolean xpMet(Tower tower) {
        return tower.experience().xp() >= this.xp;
    }

    /** Progress toward its XP, e.g. {@code "XP 120/150"}. */
    public String xpProgress(Tower tower) {
        return "XP " + Math.min(tower.experience().xp(), this.xp) + "/" + this.xp;
    }
}
