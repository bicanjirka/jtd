package td.tower.upgrade;

import td.tower.TowerModifiers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The {@code BASE} slot every tower shares: the Range line, and the chain Attune, Awaken,
 * Transcendent that opens the other slots step by step (see {@link UpgradeTier#openedBy()}).
 */
public final class StandardBaseSlot {

    public static final String RANGE_ID = "base.range";
    public static final String AWAKEN_ID = "base.awaken";
    public static final String RANGE_2_ID = "base.range.2";
    public static final String RANGE_3_ID = "base.range.3";
    public static final String ATTUNE_ID = "base.attune";
    public static final String TRANSCENDENT_ID = "base.transcendent";

    private static final float RANGE_BONUS = 0.15f;
    static final float RANGE_STEP_BONUS = 0.1f;

    private static final Set<String> RANGE_LINE_IDS = Set.of(RANGE_ID, RANGE_2_ID, RANGE_3_ID);

    private StandardBaseSlot() {
    }

    /** Whether {@code node} is Range, Range II or Range III rather than a step of the chain. */
    static boolean isRangeLine(UpgradeNode node) {
        return RANGE_LINE_IDS.contains(node.id());
    }

    /**
     * The base slot of a tower listed at {@code listPrice} with {@code baseRange} cells of range: Range I
     * and II, Attune and Awaken, and Range III and Transcendent once the tree has a level III head for
     * Transcendent to need.
     */
    public static List<UpgradeNode> nodes(int listPrice, float baseRange, UpgradeNode... levelThrees) {
        return nodes(listPrice, baseRange, BaseSlotPerks.none(), levelThrees);
    }

    /** {@link #nodes(int, float, UpgradeNode...)} with what the tower adds to Attune, Awaken and Range III. */
    public static List<UpgradeNode> nodes(int listPrice, float baseRange, BaseSlotPerks perks,
                                          UpgradeNode... levelThrees) {
        UpgradeNode range1 = UpgradeTier.RANGE_1.node(RANGE_ID, "Range", listPrice)
                .withModifiers(TowerModifiers.range(RANGE_BONUS * baseRange));
        UpgradeNode range2 = UpgradeTier.RANGE_2.node(RANGE_2_ID, "Range II", listPrice)
                .withModifiers(TowerModifiers.range(RANGE_STEP_BONUS * baseRange))
                .after(range1);
        UpgradeNode attune = UpgradeTier.ATTUNE.node(ATTUNE_ID, "Attune", listPrice)
                .withEffects(perks.attune().withNote("unlocks head levels I and II"));
        UpgradeNode awaken = UpgradeTier.AWAKEN.node(AWAKEN_ID, "Awaken", listPrice)
                .withEffects(perks.awaken().withNote(levelThrees.length == 0 ? "unlocks a special"
                        : "unlocks a special and head level III"));
        if (levelThrees.length == 0) {
            return List.of(range1, range2, attune, awaken);
        }
        UpgradeNode range3 = UpgradeTier.RANGE_3.node(RANGE_3_ID, "Range III", listPrice)
                .withEffects(perks.rangeThree()
                        .withModifiers(TowerModifiers.range(perks.rangeThreeBonus() * baseRange)))
                .after(range2);
        UpgradeNode transcendent = UpgradeTier.TRANSCENDENT.node(TRANSCENDENT_ID, "Transcendent", listPrice)
                .withGate(new TranscendentCondition(Arrays.stream(levelThrees).map(UpgradeNode::id).toList()))
                .withNote("unlocks a second special and Range III");
        return List.of(range1, range2, range3, attune, awaken, transcendent);
    }
}
