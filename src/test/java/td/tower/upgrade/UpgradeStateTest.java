package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.TowerModifiers;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeStateTest {

    private static final UpgradeNode RANGE = UpgradeNode.of("range", "Range", 10)
            .withModifiers(TowerModifiers.rangePercent(0.15f));
    private static final UpgradeNode FIRST = UpgradeNode.of("head.a.1", "A1", 10)
            .withModifiers(TowerModifiers.damagePercent(0.25f));
    private static final UpgradeNode SECOND = UpgradeNode.of("head.a.2", "A2", 20).inTier(1)
            .withModifiers(TowerModifiers.damagePercent(0.25f));

    @Test
    void noneOwnsNothing() {
        UpgradeState state = UpgradeState.none();

        assertThat(state.owns(RANGE.id())).isFalse();
        assertThat(state.countInTier(0)).isZero();
    }

    @Test
    void withAddsANodeWithoutMutatingTheOriginalState() {
        UpgradeState before = UpgradeState.none();

        UpgradeState after = before.with(RANGE);

        assertThat(before.owns(RANGE.id())).isFalse();
        assertThat(after.owns(RANGE.id())).isTrue();
    }

    @Test
    void countInTierCountsOnlyTheOwnedNodesOfThatTier() {
        UpgradeState state = UpgradeState.none().with(FIRST).with(RANGE).with(SECOND);

        assertThat(state.countInTier(0)).isEqualTo(2);
        assertThat(state.countInTier(1)).isEqualTo(1);
    }
}
