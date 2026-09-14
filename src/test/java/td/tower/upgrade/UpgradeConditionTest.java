package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeConditionTest {

    @Test
    void alwaysIsSatisfiedRegardlessOfTowerOrContextState() {
        GameWorld context = new GameWorld(new RecordingGameHost());
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, context, 0, 0);

        assertThat(UpgradeCondition.always().isSatisfied(tower, context)).isTrue();
    }
}
