package td.tower.upgrade;

import td.tower.buff.TowerBuff;

import java.util.ArrayList;
import java.util.List;

/**
 * One node in a tower's upgrade tree. Hooks and prerequisites match nodes by their {@code id},
 * unique within one tree, since a branching graph can reconverge.
 * <p>
 * {@code requires} decides whether the node is offered at all; {@code gate} is the performance
 * condition to clear once it is. The UI hides an unreachable node and shows a gated one with its
 * progress.
 */
public record UpgradeNode(String id, UpgradeSlot slot, String displayName, int price, TowerBuff statBonus,
                          UpgradeCondition requires, UpgradeCondition gate, String extraEffect) {

    /** A node gated on price alone; add the rest with the {@code withX} copies. */
    public static UpgradeNode of(String id, UpgradeSlot slot, String displayName, int price) {
        return new UpgradeNode(id, slot, displayName, price, TowerBuff.none(), UpgradeCondition.always(),
                UpgradeCondition.always(), "");
    }

    public UpgradeNode withBuff(TowerBuff statBonus) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, statBonus, this.requires,
                this.gate, this.extraEffect);
    }

    public UpgradeNode withRequires(UpgradeCondition requires) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, requires,
                this.gate, this.extraEffect);
    }

    public UpgradeNode withGate(UpgradeCondition gate) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, this.requires,
                gate, this.extraEffect);
    }

    public UpgradeNode withExtraEffect(String extraEffect) {
        return new UpgradeNode(this.id, this.slot, this.displayName, this.price, this.statBonus, this.requires,
                this.gate, extraEffect);
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
