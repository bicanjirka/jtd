package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.TowerCapability;
import td.tower.TowerStat;
import td.tower.proc.HitBonus;
import td.tower.proc.Proc;
import td.tower.targeting.TargetPriority;


/**
 * One upgrade in a tower's tree. Hooks and prerequisites match nodes by their {@code id}, unique within one tree.
 * <p>
 * It sits in {@code tier}, which must be open before it is bought; {@code requires} names the nodes it comes after.
 * Once both hold, it costs only its {@code price}.
 */
public record UpgradeNode(String id, String displayName, int price, NodeEffects effects, UpgradeCondition requires,
                          int tier) {

    public UpgradeNode {
        if (tier < 0) {
            throw new IllegalArgumentException("No tier below 0: " + id);
        }
    }

    /** A node of tier 0 that needs nothing before it; add the rest with the {@code withX} copies. */
    public static UpgradeNode of(String id, String displayName, int price) {
        return new UpgradeNode(id, displayName, price, NodeEffects.none(), UpgradeCondition.always(), 0);
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
        return new UpgradeNode(this.id, this.displayName, this.price, effects, this.requires, this.tier);
    }

    /** This node in {@code tier}. */
    public UpgradeNode inTier(int tier) {
        return new UpgradeNode(this.id, this.displayName, this.price, this.effects, this.requires, tier);
    }

    public UpgradeNode withRequires(UpgradeCondition requires) {
        return new UpgradeNode(this.id, this.displayName, this.price, this.effects, requires, this.tier);
    }

    /** This node, offered only once {@code previous}, the node it does nothing without, is owned too. */
    public UpgradeNode after(UpgradeNode previous) {
        return this.withRequires(this.requires.and(UpgradeCondition.owns(previous.id())));
    }

    /** This node with {@code note} as the words for what its data can't say. */
    public UpgradeNode withNote(String note) {
        return this.withEffects(this.effects.withNote(note));
    }
}
