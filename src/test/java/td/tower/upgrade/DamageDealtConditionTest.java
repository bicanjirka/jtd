package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class DamageDealtConditionTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void notSatisfiedBeforeTheThresholdIsReached() {
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, this.context, 0, 0);

        assertThat(new DamageDealtCondition(1).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void satisfiedOnceEnoughDamageHasLanded() {
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, this.context, 0, 0);
        // high health so the hit doesn't kill it - only damageDealt is under test here
        EnemyMob enemy = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, 1);
        this.context.setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(tower.getDamageDealt()).isGreaterThan(0);
        assertThat(new DamageDealtCondition(tower.getDamageDealt()).isSatisfied(tower, this.context)).isTrue();
        assertThat(new DamageDealtCondition(tower.getDamageDealt() + 1).isSatisfied(tower, this.context)).isFalse();
    }
}
