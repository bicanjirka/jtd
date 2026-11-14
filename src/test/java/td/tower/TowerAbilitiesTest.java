package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeAbilityMechanic;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.ability.AbilityState;
import td.tower.ability.AbilityUnits;
import td.tower.upgrade.Mechanic;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerAbilitiesTest {

    private static final UpgradeNode BOLT = UpgradeNode.of("test.bolt", "Bolt", 0);
    private static final UpgradeNode STORM = UpgradeNode.of("test.storm", "Storm", 0);

    private final GameWorld world = WorldFixtures.newWorld();

    /** A tower owning Bolt (2 Sparks), then Storm (3 Sparks), bought in that order. */
    private FakeTower withBoltThenStorm() {
        MechanicCatalogue<Mechanic> mechanics = MechanicCatalogue.<Mechanic>empty()
                .with(BOLT.id(), () -> FakeAbilityMechanic.costing("Bolt", 2))
                .with(STORM.id(), () -> FakeAbilityMechanic.costing("Storm", 3));
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none().with(BOLT, STORM), mechanics);
        this.world.economy().startEconomy(1000, 5);
        tower.buyUpgrade(BOLT);
        tower.buyUpgrade(STORM);
        return tower;
    }

    private static List<AbilityUnits> fills(Tower tower) {
        return tower.abilities().stream().map(state -> state.bar().fill()).toList();
    }

    private static String titleOf(Mechanic mechanic) {
        return mechanic.ability().orElseThrow().ability().term().title();
    }

    @Test
    void whatTheTowerEarnsGoesIntoEveryAbilitysBarAtOnce() {
        FakeTower tower = this.withBoltThenStorm();

        tower.earn(AbilityUnits.of(0.5f));

        assertThat(fills(tower)).containsExactly(AbilityUnits.of(1.5f), AbilityUnits.of(1.5f));
    }

    @Test
    void abilitiesFullOnOneTryCastInTheOrderTheyWereBoughtAndEachPaysItsOwnCost() {
        FakeTower tower = this.withBoltThenStorm();
        tower.earn(AbilityUnits.whole(2));
        List<String> cast = new ArrayList<>();

        tower.castReady(mechanic -> cast.add(titleOf(mechanic)));

        assertThat(cast).containsExactly("Bolt", "Storm");
        assertThat(fills(tower)).containsExactly(AbilityUnits.whole(1), AbilityUnits.none());
    }

    @Test
    void anAbilityWithNoTargetWaitsFullAndOneWithoutItsCostIsNotTried() {
        FakeTower tower = this.withBoltThenStorm();
        tower.earn(AbilityUnits.whole(1));
        List<String> tried = new ArrayList<>();

        tower.castReady(mechanic -> !tried.add(titleOf(mechanic)));

        assertThat(tried).containsExactly("Bolt");
        assertThat(fills(tower)).containsExactly(AbilityUnits.whole(2), AbilityUnits.whole(2));
    }

    @Test
    void everyWaveStartsEachAbilityBarAtOneUnit() {
        FakeTower tower = this.withBoltThenStorm();
        tower.earn(AbilityUnits.of(1.75f));

        tower.readyForWave();

        assertThat(fills(tower)).containsExactly(AbilityUnits.whole(1), AbilityUnits.whole(1));
    }

    @Test
    void theAbilitiesAreListedInTheOrderTheyWereBought() {
        FakeTower tower = this.withBoltThenStorm();

        List<String> titles = tower.abilities().stream().map(AbilityState::ability)
                .map(ability -> ability.term().title()).toList();

        assertThat(titles).containsExactly("Bolt", "Storm");
    }
}
