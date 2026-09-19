package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeConditionTest {

    @Test
    void alwaysIsSatisfiedRegardlessOfTowerOrContextState() {
        GameWorld context = WorldFixtures.newWorld();
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0);

        assertThat(UpgradeCondition.always().isSatisfied(tower, context)).isTrue();
    }

    @Test
    void alwaysDescribesItselfAsMoneyOnly() {
        assertThat(UpgradeCondition.always().describe()).isEqualTo("money only");
    }
}
