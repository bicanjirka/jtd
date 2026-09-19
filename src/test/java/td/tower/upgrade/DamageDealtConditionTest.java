package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class DamageDealtConditionTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void notSatisfiedBeforeTheThresholdIsReached() {
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.context, 0, 0);

        assertThat(new DamageDealtCondition(1).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void satisfiedOnceEnoughDamageHasLanded() {
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.context, 0, 0);
        // high health so the hit doesn't kill it - only damageDealt is under test here
        EnemyMob enemy = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        tower.doTick(0);

        assertThat(tower.getDamageDealt()).isGreaterThan(0);
        assertThat(new DamageDealtCondition(tower.getDamageDealt()).isSatisfied(tower, this.context)).isTrue();
        assertThat(new DamageDealtCondition(tower.getDamageDealt() + 1).isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void describesItselfInWholeDamagePointsNotHundredths() {
        assertThat(new DamageDealtCondition(20000).describe()).isEqualTo("200.0 damage dealt");
    }
}
