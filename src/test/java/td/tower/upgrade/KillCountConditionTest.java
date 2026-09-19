package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class KillCountConditionTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void notSatisfiedBeforeTheThresholdIsReached() {
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.context, 0, 0);

        assertThat(new KillCountCondition(1).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void satisfiedOnceEnoughKillsHaveLanded() {
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", this.context, 0, 1, 3, 1);
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(tower.getKillCount()).isEqualTo(1);
        assertThat(new KillCountCondition(1).isSatisfied(tower, this.context)).isTrue();
        assertThat(new KillCountCondition(2).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void describesItselfByItsThreshold() {
        assertThat(new KillCountCondition(10).describe()).isEqualTo("10 kills");
    }
}
