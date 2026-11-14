package td.tower.upgrade;

import td.tower.TowerModifiers;

import java.util.List;

/**
 * The Range upgrades every tower shares, one in each of tiers 0 to II, each a share of the tower's base range in cells
 * and priced as a share of its list price.
 */
public final class RangeNodes {

    public static final String RANGE_ID = "base.range";
    public static final String RANGE_2_ID = "base.range.2";
    public static final String RANGE_3_ID = "base.range.3";

    private static final float RANGE_BONUS = 0.15f;
    private static final float RANGE_STEP_BONUS = 0.1f;
    private static final int[] PRICE_PERCENTS = {60, 120, 200};

    private RangeNodes() {
    }

    /** Range, Range II and Range III of a tower listed at {@code listPrice} with {@code baseRange} cells of range. */
    public static List<UpgradeNode> of(int listPrice, float baseRange) {
        return of(listPrice, baseRange, RANGE_STEP_BONUS, NodeEffects.none());
    }

    /** {@link #of(int, float)} with Range III's own share of the base range and what else it does. */
    public static List<UpgradeNode> of(int listPrice, float baseRange, float rangeThreeBonus, NodeEffects rangeThree) {
        UpgradeNode first = UpgradeNode.of(RANGE_ID, "Range", priceOf(listPrice, 0))
                .withModifiers(TowerModifiers.range(RANGE_BONUS * baseRange));
        UpgradeNode second = UpgradeNode.of(RANGE_2_ID, "Range II", priceOf(listPrice, 1))
                .withModifiers(TowerModifiers.range(RANGE_STEP_BONUS * baseRange)).after(first).inTier(1);
        UpgradeNode third = UpgradeNode.of(RANGE_3_ID, "Range III", priceOf(listPrice, 2))
                .withEffects(rangeThree.withModifiers(TowerModifiers.range(rangeThreeBonus * baseRange)))
                .after(second).inTier(2);
        return List.of(first, second, third);
    }

    private static int priceOf(int listPrice, int step) {
        return (listPrice * PRICE_PERCENTS[step] + 50) / 100;
    }
}
