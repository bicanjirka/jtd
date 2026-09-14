package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyFactoryTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

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
    void getEnemyBuildsARealEmptyMobForTheSpacerToken() {
        EnemyMob enemy = EnemyFactory.getEnemy("e", context, 0, 50, 3, 1);

        assertThat(enemy).isInstanceOf(EnemyMobEmpty.class);
        assertThat(enemy.validTarget()).isFalse();
    }

    @Test
    void getEnemyBuildsARealEnemyWithHealthScaledByOneHundred() {
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(enemy).isInstanceOf(DefinedEnemyMob.class);
        assertThat(enemy.getHealth()).isEqualTo(5000);
    }

    @Test
    void repeatedCallsReturnIndependentInstances() {
        EnemyMob first = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        EnemyMob second = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(first).isNotSameAs(second);

        first.doDamage(Damage.physical(100_00));
        assertThat(first.validTarget()).isFalse();
        assertThat(second.validTarget()).isTrue();
    }
}
