package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class WaveTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void plainTokensCountAsOneEnemyEach() {
        Wave wave = new Wave(context, 100, 5, 1, "c e c".split(" "));

        // "e" (Empty) is a filler mob and is excluded from enemyCount()
        assertThat(wave.enemyCount()).isEqualTo(2);
        assertThat(wave.getEnemies()).hasSize(3);
    }

    @Test
    void numericPrefixMultipliesTheFollowingToken() {
        Wave wave = new Wave(context, 100, 5, 1, "2 c".split(" "));

        assertThat(wave.enemyCount()).isEqualTo(2);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
    }

    @Test
    void enemySetAndPerEnemyCountReflectTheParsedTokens() {
        Wave wave = new Wave(context, 100, 5, 1, "c e c".split(" "));

        assertThat(wave.enemySet()).containsExactlyInAnyOrder(EnemyFactory.Enemy.Circle, EnemyFactory.Enemy.Empty);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Empty)).isEqualTo(1);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Ghost)).isZero();
    }

    @Test
    void unrecognizedTokenIsTreatedAsMultiplierOneAndDoesNotThrow() {
        Wave wave = new Wave(context, 100, 5, 1, "c ? c".split(" "));

        assertThat(wave.enemyCount()).isEqualTo(2);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(context, 251, 2, 3, new String[0]);

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }
}
