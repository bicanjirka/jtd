package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyFactoryTest {

    private final Context context = new Context(new RecordingGameHost());

    @Test
    void isEnemyRecognizesKnownCodesOnly() {
        assertThat(EnemyFactory.isEnemy("c")).isTrue();
        assertThat(EnemyFactory.isEnemy("s")).isTrue();
        assertThat(EnemyFactory.isEnemy("t")).isTrue();
        assertThat(EnemyFactory.isEnemy("g")).isTrue();
        assertThat(EnemyFactory.isEnemy("e")).isTrue();
        assertThat(EnemyFactory.isEnemy("?")).isFalse();
    }

    @Test
    void identifyEnemyMapsCodeToEnumConstant() {
        assertThat(EnemyFactory.identifyEnemy("c")).isEqualTo(EnemyFactory.Enemy.Circle);
        assertThat(EnemyFactory.identifyEnemy("g")).isEqualTo(EnemyFactory.Enemy.Ghost);
    }

    @Test
    void getEnemyBuildsARealEnemyWithHealthScaledByOneHundred() {
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(enemy).isInstanceOf(EnemyMobCircle.class);
        assertThat(enemy.getHealth()).isEqualTo(5000);
    }

    @Test
    void repeatedCallsReturnIndependentInstances() {
        EnemyMob first = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        EnemyMob second = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(first).isNotSameAs(second);

        first.doDamage(100_00);
        assertThat(first.validTarget()).isFalse();
        assertThat(second.validTarget()).isTrue();
    }
}
