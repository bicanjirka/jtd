package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class PurposeConditionTest {

    private static final int LIST_PRICE = 10;
    private static final PurposeCondition STEADY_AIM = new PurposeCondition("Steady Aim shots", 3);
    private static final UpgradeNode HEAD_ONE = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode HEAD_THREE = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE)
            .withGate(STEADY_AIM);
    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(LIST_PRICE)).with(HEAD_ONE, HEAD_THREE);

    private final GameWorld world = WorldFixtures.newWorld();

    @Test
    void deedsCountOnlyOnceAttuneIsOwned() {
        FakeTower tower = this.attunedLater();

        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    @Test
    void thePurposeGateShowsTheDeedsDoneOfTheThreshold() {
        FakeTower tower = this.attunedLater();

        assertThat(STEADY_AIM.progress(tower, this.world)).isEqualTo("Steady Aim shots 1/3");
        assertThat(STEADY_AIM.isSatisfied(tower, this.world)).isFalse();
    }

    @Test
    void aHeadThreeNeedsBothItsXpAndItsPurpose() {
        FakeTower purposeOnly = this.withDeeds(3);
        FakeTower xpOnly = this.withDeeds(0);
        FakeTower both = this.withDeeds(3);
        xpOnly.earnXp(150);
        both.earnXp(150);

        assertThat(HEAD_THREE.gateMet(purposeOnly, this.world)).isFalse();
        assertThat(HEAD_THREE.gateMet(xpOnly, this.world)).isFalse();
        assertThat(HEAD_THREE.gateMet(both, this.world)).isTrue();
    }

    /** One deed before Attune, which doesn't count, and one after, in a later tick. */
    private FakeTower attunedLater() {
        this.world.economy().startEconomy(1000, 5);
        FakeTower tower = FakeTower.offering(this.world, 0, 0, TREE);
        tower.beginTick(1);
        tower.performDeedOfAttack();
        tower.buyUpgrade(attune());
        tower.beginTick(2);
        tower.performDeedOfAttack();
        return tower;
    }

    private FakeTower withDeeds(int deeds) {
        this.world.economy().startEconomy(1000, 5);
        FakeTower tower = FakeTower.offering(this.world, 0, 0, TREE);
        tower.buyUpgrade(attune());
        for (int tick = 1; tick <= deeds; tick++) {
            tower.beginTick(tick);
            tower.performDeedOfAttack();
        }
        return tower;
    }

    private static UpgradeNode attune() {
        return TREE.nodes().stream().filter(n -> n.id().equals(StandardBaseSlot.ATTUNE_ID)).findFirst().orElseThrow();
    }
}
