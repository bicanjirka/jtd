package td.tower.upgrade;

import td.tower.Tower;
import td.tower.buff.TowerBuff;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * One node in a tower's upgrade tree. Hooks and prerequisites match nodes by their {@code id},
 * unique within one tree, since a branching graph can reconverge.
 * <p>
 * {@code requires} decides whether the node is offered at all. Once offered, it is bought when the
 * tower has {@code xp} XP and its {@code gate} (a purpose or layout condition) is met: see
 * {@link #gateMet}. The UI hides an unreachable node and shows a gated one with its progress.
 */
public record UpgradeNode(String id, UpgradeSlot slot, String displayName, int price, TowerBuff statBonus,
                          UpgradeCondition requires, UpgradeCondition gate, String extraEffect, int xp) {

    /** A node gated on price alone; add the rest with the {@code withX} copies. */
    public static UpgradeNode of(String id, UpgradeSlot slot, String displayName, int price) {
        return new UpgradeNode(id, slot, displayName, price, TowerBuff.none(), UpgradeCondition.always(),
                UpgradeCondition.always(), "", 0);
    }

    public UpgradeNode withBuff(TowerBuff statBonus) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, statBonus, this.requires,
                this.gate, this.extraEffect, this.xp);
    }

    public UpgradeNode withRequires(UpgradeCondition requires) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, requires,
                this.gate, this.extraEffect, this.xp);
    }

    public UpgradeNode withGate(UpgradeCondition gate) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, this.requires,
                gate, this.extraEffect, this.xp);
    }

    /** The XP a tower needs before it can buy this node. */
    public UpgradeNode withXp(int xp) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, this.requires,
                this.gate, this.extraEffect, xp);
    }

    /** This node, offered only once {@code previous} (the level before it in its line) is owned too. */
    public UpgradeNode after(UpgradeNode previous) {
        return this.withRequires(this.requires.and(UpgradeCondition.owns(previous.id())));
    }

    public UpgradeNode withExtraEffect(String extraEffect) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, this.requires,
                this.gate, extraEffect, this.xp);
    }

    /** Whether {@code tower} has this node's XP and meets its gate: price aside, it may buy it. */
    public boolean gateMet(Tower tower, GameWorld context) {
        return this.xpMet(tower) && this.gate.isSatisfied(tower, context);
    }

    public boolean xpMet(Tower tower) {
        return tower.experience().xp() >= this.xp;
    }

    /** Progress toward its XP, e.g. {@code "XP 120/150"}. */
    public String xpProgress(Tower tower) {
        return "XP " + Math.min(tower.experience().xp(), this.xp) + "/" + this.xp;
    }

    /**
     * What buying this node grants: every non-zero buff axis, then the extra effect. The gate and
     * the structural prerequisite are not bonuses.
     */
    public List<UpgradeBonus> bonuses() {
        List<UpgradeBonus> bonuses = new ArrayList<>();
        addIfNonZero(bonuses, this.statBonus.damageBonus(), "Damage");
        addIfNonZero(bonuses, this.statBonus.rangeBonus(), "Range");
        addIfNonZero(bonuses, this.statBonus.fireRateBonus(), "Fire rate");
        addIfNonZero(bonuses, this.statBonus.bountyBonus(), "Bounty");
        addIfNonZero(bonuses, this.statBonus.critChanceBonus(), "Crit chance");
        addIfNonZero(bonuses, this.statBonus.armorPenetrationBonus(), "Armor penetration");
        addIfNonZero(bonuses, this.statBonus.magicPenetrationBonus(), "Magic penetration");
        if (!this.extraEffect.isEmpty()) {
            bonuses.add(new UpgradeBonus(Character.toUpperCase(this.extraEffect.charAt(0)) + this.extraEffect.substring(1), ""));
        }
        return bonuses;
    }

    private static void addIfNonZero(List<UpgradeBonus> bonuses, float fraction, String label) {
        if (fraction != 0f) {
            bonuses.add(new UpgradeBonus(label, (fraction >= 0 ? "+" : "") + Math.round(fraction * 100) + "%"));
        }
    }
}
