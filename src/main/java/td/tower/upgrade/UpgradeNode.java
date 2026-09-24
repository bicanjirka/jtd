package td.tower.upgrade;

import td.tower.buff.TowerBuff;

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
     * What buying this node costs and grants: its gate and its bonuses. The structural prerequisite
     * is not shown.
     */
    public String describe() {
        String buffText = describeBuff(this.statBonus);
        String stats = this.extraEffect.isEmpty()
                ? buffText
                : buffText.isEmpty() ? this.extraEffect : buffText + ", " + this.extraEffect;
        return this.displayName + " (" + this.gate.describe() + "): " + stats;
    }

    private static String describeBuff(TowerBuff buff) {
        StringBuilder parts = new StringBuilder();
        appendIfNonZero(parts, buff.damageBonus(), "damage");
        appendIfNonZero(parts, buff.rangeBonus(), "range");
        appendIfNonZero(parts, buff.fireRateBonus(), "fire rate");
        appendIfNonZero(parts, buff.bountyBonus(), "bounty");
        appendIfNonZero(parts, buff.critChanceBonus(), "crit chance");
        return parts.toString();
    }

    private static void appendIfNonZero(StringBuilder parts, float fraction, String label) {
        if (fraction == 0f) {
            return;
        }
        if (!parts.isEmpty()) {
            parts.append(", ");
        }
        parts.append(signedPercent(fraction)).append(' ').append(label);
    }

    private static String signedPercent(float fraction) {
        return (fraction >= 0 ? "+" : "") + Math.round(fraction * 100) + "%";
    }
}
