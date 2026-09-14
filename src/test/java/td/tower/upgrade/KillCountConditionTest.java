package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class KillCountConditionTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void notSatisfiedBeforeTheThresholdIsReached() {
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, this.context, 0, 0);

        assertThat(new KillCountCondition(1).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void satisfiedOnceEnoughKillsHaveLanded() {
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, this.context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", this.context, 0, 1, 3, 1);
        this.context.setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(tower.getKillCount()).isEqualTo(1);
        assertThat(new KillCountCondition(1).isSatisfied(tower, this.context)).isTrue();
        assertThat(new KillCountCondition(2).isSatisfied(tower, this.context)).isFalse();
    }
}
