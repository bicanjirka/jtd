package td.tower.upgrade;

import java.util.List;

/**
 * One tier of a tree, a row of upgrades that opens from the bottom up: once the tier below is open, it opens when its
 * tower has {@code xp} XP, by itself when it is free, or on the player's click that pays {@code unlockPrice}. Once it is
 * open, everything in it costs only credits.
 */
public record UpgradeTier(int xp, int unlockPrice) {

    private static final UpgradeTier OPEN = new UpgradeTier(0, 0);
    private static final List<UpgradeTier> STANDARD = List.of(OPEN, gated(60, 0), gated(120, 0), gated(200, 0));

    public UpgradeTier {
        if (xp < 0 || unlockPrice < 0) {
            throw new IllegalArgumentException("A tier's gate is never negative: XP " + xp + ", $" + unlockPrice);
        }
    }

    /** Tier 0: open from the moment the tower is built. */
    public static UpgradeTier open() {
        return OPEN;
    }

    /** A tier that opens at {@code xp} XP, paying {@code unlockPrice} for it when that is more than nothing. */
    public static UpgradeTier gated(int xp, int unlockPrice) {
        return new UpgradeTier(xp, unlockPrice);
    }

    /** The tiers every tower climbs: 0, then I to III. */
    public static List<UpgradeTier> standard() {
        return STANDARD;
    }

    /** Whether opening it waits for the player's click and costs credits. */
    public boolean isPaid() {
        return this.unlockPrice > 0;
    }

    /** {@code tier}'s name as the panel and its hovers say it: "Tier 0", "Tier I", "Tier II". */
    public static String nameOf(int tier) {
        return "Tier " + numeralOf(tier);
    }

    /** {@code tier}'s numeral alone: "0", "I", "II". */
    public static String numeralOf(int tier) {
        if (tier == 0) {
            return "0";
        }
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return "X".repeat(tier / 10) + ones[tier % 10];
    }
}
