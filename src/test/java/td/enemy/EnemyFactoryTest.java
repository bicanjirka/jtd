package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyFactoryTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void isEnemyRecognizesKnownCodesOnly() {
        assertThat(EnemyFactory.isEnemy("c")).isTrue();
        assertThat(EnemyFactory.isEnemy("s")).isTrue();
        assertThat(EnemyFactory.isEnemy("t")).isTrue();
        assertThat(EnemyFactory.isEnemy("g")).isTrue();
        assertThat(EnemyFactory.isEnemy("e")).isFalse();
        assertThat(EnemyFactory.isEnemy("?")).isFalse();
    }

    @Test
    void getEnemyBuildsARealEnemyWithHealthScaledByOneHundred() {
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(enemy).isInstanceOf(DefinedEnemyMob.class);
        assertThat(enemy.getHealth()).isEqualTo(5000);
    }

    @Test
    void repeatedCallsReturnIndependentInstances() {
        EnemyMob first = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        EnemyMob second = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(first).isNotSameAs(second);

        first.doDamage(Damage.physical(100_00));
        assertThat(first.validTarget()).isFalse();
        assertThat(second.validTarget()).isTrue();
    }
}
