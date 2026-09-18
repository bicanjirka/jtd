package td.tower.upgrade;

import td.tower.buff.TowerBuff;

/**
 * One of a tower's (exactly two, for v1) permanent specializations: a name shown in the UI,
 * its price (paid the same way buying a tower is - see {@code EconomyLedger.doPay}), the stat
 * bonus it grants via {@link TowerBuff}'s existing additive algebra, the
 * {@link UpgradeCondition} gating it, and (for the few paths that bump a stat
 * {@code TowerBuff} can't express - {@code SplashTower}'s splash radius, {@code SonarTower}'s
 * sweep speed - applied via a leaf's own {@code onUpgradePathChosen} override) a short
 * human-readable phrase for that extra effect. Choosing one is a one-time, exclusive event
 * handled by {@code Tower.chooseUpgradePath} - this record is just the immutable description of
 * the choice, not the choice itself.
 */
public record UpgradePath(String displayName, int price, TowerBuff statBonus, UpgradeCondition condition,
                          String extraEffect) {

    /**
     * Equivalent to the five-argument canonical constructor with {@code extraEffect = ""} -
     * every path whose whole bonus is expressible through {@link TowerBuff} alone stays exactly
     * as concise to construct as before.
     */
    public UpgradePath(String displayName, int price, TowerBuff statBonus, UpgradeCondition condition) {
        this(displayName, price, statBonus, condition, "");
    }

    /**
     * A short, human-readable summary of what choosing this path costs and grants - what the
     * info panel lists for every path a tower still offers (see
     * {@code AbstractTower.upgradePathsBlock}). Never empty: a path with no {@link TowerBuff}
     * bonus and no {@link #extraEffect} would be a no-op specialization, which no v1 content is.
     */
    public String describe() {
        String buffText = describeBuff(this.statBonus);
        String stats = this.extraEffect.isEmpty()
                ? buffText
                : buffText.isEmpty() ? this.extraEffect : buffText + ", " + this.extraEffect;
        return this.displayName + " (" + this.condition.describe() + "): " + stats;
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
