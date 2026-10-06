package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.buff.TowerBuff;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeStateTest {

    private static final UpgradeNode RANGE = UpgradeNode.of("range", UpgradeSlot.BASE, "Range", 10).withBuff(TowerBuff.range(0.15f));
    private static final UpgradeNode HEAD_LV1 = UpgradeNode.of("head.a.1", UpgradeSlot.HEAD, "A1", 10)
            .withBuff(TowerBuff.damage(0.25f));
    private static final UpgradeNode HEAD_LV2 = UpgradeNode.of("head.a.2", UpgradeSlot.HEAD, "A2", 20)
            .withBuff(TowerBuff.damage(0.25f));

    @Test
    void noneOwnsNothingAndHasNoTipInAnySlot() {
        UpgradeState state = UpgradeState.none();

        assertThat(state.owns(RANGE.id())).isFalse();
        assertThat(state.tip(UpgradeSlot.BASE)).isEmpty();
        assertThat(state.totalBuff()).isEqualTo(TowerBuff.none());
    }

    @Test
    void withAddsANodeWithoutMutatingTheOriginalState() {
        UpgradeState before = UpgradeState.none();

        UpgradeState after = before.with(RANGE);

        assertThat(before.owns(RANGE.id())).isFalse();
        assertThat(after.owns(RANGE.id())).isTrue();
    }

    @Test
    void tipIsTheMostRecentlyAddedNodeInThatSlot() {
        UpgradeState state = UpgradeState.none().with(HEAD_LV1).with(HEAD_LV2);

        assertThat(state.tip(UpgradeSlot.HEAD)).contains(HEAD_LV2);
    }

    @Test
    void inSlotListsEveryOwnedNodeInThatSlotInPurchaseOrder() {
        UpgradeState state = UpgradeState.none().with(HEAD_LV1).with(RANGE).with(HEAD_LV2);

        assertThat(state.inSlot(UpgradeSlot.HEAD)).containsExactly(HEAD_LV1, HEAD_LV2);
    }

    @Test
    void totalBuffCombinesEveryOwnedNodesStatBonusAdditively() {
        UpgradeState state = UpgradeState.none().with(HEAD_LV1).with(HEAD_LV2);

        assertThat(state.totalBuff()).isEqualTo(TowerBuff.damage(0.5f));
    }
}
