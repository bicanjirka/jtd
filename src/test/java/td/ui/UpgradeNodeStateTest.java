package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeTier;
import td.ui.UpgradeNodeState.Look;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;

class UpgradeNodeStateTest {

    private final GameWorld world = WorldFixtures.newWorld();

    @Test
    void aNodeOfATierNotYetOpenIsTierLockedAndSoIsTheUnlockShortOfItsXp() {
        FakeTower tower = this.tower();

        assertThat(UpgradeNodeState.of(MID, tower, this.world).look()).isEqualTo(Look.TIER_LOCKED);
        assertThat(UpgradeNodeState.of(TIER_ONE, tower, this.world).look()).isEqualTo(Look.TIER_LOCKED);
    }

    @Test
    void aTiersUnlockIsBuyableOnceItsXpIsMet() {
        FakeTower tower = this.tower();
        tower.earnXp(UpgradeTier.standard().get(1).xp());

        assertThat(UpgradeNodeState.of(TIER_ONE, tower, this.world).look()).isEqualTo(Look.BUYABLE);
    }

    @Test
    void aNodeWhoseLineIsNotYetBoughtNeedsThePreviousOne() {
        FakeTower tower = this.tower();
        tower.earnXp(UpgradeTier.standard().get(1).xp());
        tower.buyPath(TIER_ONE);

        assertThat(UpgradeNodeState.of(A2, tower, this.world).look()).isEqualTo(Look.NEEDS_PREVIOUS);
    }

    @Test
    void anOpenNodeIsBuyableOrShortOfCashByWhatTheCreditsCover() {
        FakeTower tower = this.tower();

        Look covered = UpgradeNodeState.of(LONE, tower, this.world).look();
        this.world.economy().startEconomy(LONE.price() - 1, 5);
        Look uncovered = UpgradeNodeState.of(LONE, tower, this.world).look();

        assertThat(covered).isEqualTo(Look.BUYABLE);
        assertThat(uncovered).isEqualTo(Look.SHORT_OF_CASH);
    }

    @Test
    void aPickIsOwnedItsRivalSwitchableAndTheRivalsFollowUpLockedOut() {
        FakeTower tower = this.tower();
        tower.earnXp(UpgradeTier.standard().get(1).xp());
        tower.buyPath(A1);
        tower.buyPath(TIER_ONE);

        assertThat(UpgradeNodeState.of(A1, tower, this.world).look()).isEqualTo(Look.OWNED);
        assertThat(UpgradeNodeState.of(B1, tower, this.world).look()).isEqualTo(Look.SWITCHABLE);
        assertThat(UpgradeNodeState.of(B2, tower, this.world).look()).isEqualTo(Look.LOCKED_OUT);
    }

    @Test
    void aKeyIsTheBindingThatBuysTheNodeNow() {
        FakeTower tower = this.tower();

        assertThat(UpgradeNodeState.of(LONE, tower, this.world).key())
                .isEqualTo(TREE.keyOf(LONE, tower, this.world));
    }

    private FakeTower tower() {
        this.world.economy().startEconomy(1_000, 5);
        return FakeTower.offering(this.world, 0, 0, TREE);
    }
}
