package td.tower.upgrade;

import td.tower.buff.TowerBuff;

import java.util.Arrays;
import java.util.List;

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
    private static final float RANGE_STEP_BONUS = 0.1f;

    private StandardBaseSlot() {
    }

    /**
     * The base slot of a tower listed at {@code listPrice}: Range I and II, Attune and Awaken, and
     * Range III and Transcendent once the tree has a level III head for Transcendent to need.
     */
    public static List<UpgradeNode> nodes(int listPrice, UpgradeNode... levelThrees) {
        UpgradeNode range1 = UpgradeTier.RANGE_1.node(RANGE_ID, "Range", listPrice)
                .withBuff(TowerBuff.range(RANGE_BONUS));
        UpgradeNode range2 = UpgradeTier.RANGE_2.node(RANGE_2_ID, "Range II", listPrice)
                .withBuff(TowerBuff.range(RANGE_STEP_BONUS))
                .after(range1);
        UpgradeNode attune = UpgradeTier.ATTUNE.node(ATTUNE_ID, "Attune", listPrice)
                .withExtraEffect("unlocks head levels I and II");
        UpgradeNode awaken = UpgradeTier.AWAKEN.node(AWAKEN_ID, "Awaken", listPrice)
                .withExtraEffect(levelThrees.length == 0 ? "unlocks a special" : "unlocks a special and head level III");
        if (levelThrees.length == 0) {
            return List.of(range1, range2, attune, awaken);
        }
        UpgradeNode range3 = UpgradeTier.RANGE_3.node(RANGE_3_ID, "Range III", listPrice)
                .withBuff(TowerBuff.range(RANGE_STEP_BONUS))
                .after(range2);
        UpgradeNode transcendent = UpgradeTier.TRANSCENDENT.node(TRANSCENDENT_ID, "Transcendent", listPrice)
                .withGate(new TranscendentCondition(Arrays.stream(levelThrees).map(UpgradeNode::id).toList()))
                .withExtraEffect("unlocks a second special and Range III");
        return List.of(range1, range2, range3, attune, awaken, transcendent);
    }
}
