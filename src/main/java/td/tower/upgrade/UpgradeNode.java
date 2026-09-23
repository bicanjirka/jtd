package td.tower.upgrade;

import td.tower.buff.TowerBuff;

/**
 * One node in a tower's upgrade tree - the successor to the old, flat {@code UpgradePath}. A
 * stable {@code id} (unique within one tower's own {@link UpgradeTree}) is what a leaf's
 * {@code onUpgradeBought} hook and a sibling node's {@code requires} condition key off, rather
 * than Java reference equality: once a slot's graph branches and reconverges, "which node was
 * just bought" can no longer be answered by comparing object identity alone.
 * <p>
 * {@code requires} is the structural prerequisite (e.g. "Awaken is owned and this slot is still
 * empty") that decides whether this node is offered at all; {@code gate} is the independent
 * performance condition (kills, damage dealt, a cluster of neighbours) a player clears once the
 * node is offered. Keeping them apart is what lets the UI show an unreachable node as hidden and
 * a reachable-but-not-yet-gated one as visible with live progress.
 * <p>
 * Eight components puts this past the root {@code CLAUDE.md}'s five-component threshold, so the
 * narrow entry point is {@link #of(String, UpgradeSlot, String, int)}; every other field is set
 * through a fluent {@code withX} copy, mirroring {@code PathDefinition}/{@code EnemyDefinition}.
 */
public record UpgradeNode(String id, UpgradeSlot slot, String displayName, int price, TowerBuff statBonus,
                          UpgradeCondition requires, UpgradeCondition gate, String extraEffect) {

    /**
     * A root, unconditionally-priced node with no stat bonus, no prerequisite and no extra
     * effect text yet - every field beyond the four every node needs is added with a
     * {@code withX} copy.
     */
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
     * A short, human-readable summary of what buying this node costs and grants - the gate
     * (never {@code requires}, which is a structural precondition the UI hides rather than
     * shows unsatisfied) plus the same buff/extra-effect text {@code UpgradePath.describe()}
     * used to build.
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
