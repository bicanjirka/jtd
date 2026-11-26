package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.TowerStat;
import td.tower.proc.Proc;

import java.util.ArrayList;
import java.util.List;

/**
 * One tier of a tree, a row of upgrades that opens from the bottom up: once the tier below is open, it opens when its
 * tower has {@code xp} XP, by itself when it is free, or on the player's click that pays {@code unlockPrice}. Once it is
 * open, everything in it costs only credits.
 * <p>
 * Its {@code bonus}, any number of stats, its {@code perks}, procs, and, when it {@code bringsMechanics}, what the
 * tower's mechanic catalogue lists under its node, are the tower's own: the tier's own node carries them, and the tower
 * holds them from the moment its XP opens the tier, whatever it buys there, and keeps them. Selling pays nothing back
 * for them. Tier 0 is the tower's base and has none of them.
 */
public record UpgradeTier(int xp, int unlockPrice, StatModifiers<TowerStat> bonus, List<Proc> perks,
                          boolean bringsMechanics) {

    private static final UpgradeTier OPEN = new UpgradeTier(0, 0, StatModifiers.none(), List.of(), false);
    private static final List<UpgradeTier> STANDARD = List.of(OPEN, gated(60, 0), gated(120, 0), gated(200, 0));

    public UpgradeTier {
        if (xp < 0 || unlockPrice < 0) {
            throw new IllegalArgumentException("A tier's gate is never negative: XP " + xp + ", $" + unlockPrice);
        }
        perks = List.copyOf(perks);
    }

    /** Tier 0: open from the moment the tower is built. */
    public static UpgradeTier open() {
        return OPEN;
    }

    /** A tier that opens at {@code xp} XP, paying {@code unlockPrice} for it when that is more than nothing. */
    public static UpgradeTier gated(int xp, int unlockPrice) {
        return new UpgradeTier(xp, unlockPrice, StatModifiers.none(), List.of(), false);
    }

    /** The same gate, with {@code bonus} for the tower once its XP opens the tier. */
    public UpgradeTier withBonus(StatModifiers<TowerStat> bonus) {
        return new UpgradeTier(this.xp, this.unlockPrice, bonus, this.perks, this.bringsMechanics);
    }

    /** The same gate, with {@code perk}, called {@code name}, among what the tower holds once its XP opens the tier. */
    public UpgradeTier withPerk(String name, Proc perk) {
        List<Proc> next = new ArrayList<>(this.perks);
        next.add(perk.named(name));
        return new UpgradeTier(this.xp, this.unlockPrice, this.bonus, next, this.bringsMechanics);
    }

    /**
     * The same gate, whose own node brings what the tower's mechanic catalogue lists under it
     * ({@link UpgradeTree#tierNodeId(int)}), as a bought node brings its own.
     */
    public UpgradeTier withMechanics() {
        return new UpgradeTier(this.xp, this.unlockPrice, this.bonus, this.perks, true);
    }

    /** Whether it gives the tower anything of its own: a bonus, a perk or mechanics. */
    public boolean gives() {
        return !this.bonus.equals(StatModifiers.none()) || !this.perks.isEmpty() || this.bringsMechanics;
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
