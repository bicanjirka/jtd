package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave turns an already-parsed {@link WaveContent} into live, world-bound enemies - see
 * {@link WaveScriptTest} for the token-parsing behavior itself, which needs no GameWorld.
 */
class WaveTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void oneEnemyMobIsSpawnedPerSpawnSlotIncludingEmpties() {
        Wave wave = new Wave(context, 100, 5, 1, WaveScript.parse("c e c"));

        assertThat(wave.getEnemies()).hasSize(3);
        assertThat(wave.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        Wave wave = new Wave(context, 100, 5, 1, WaveScript.parse("2 c s"));

        assertThat(wave.enemySet()).containsExactlyInAnyOrder(EnemyFactory.Enemy.Circle, EnemyFactory.Enemy.Square);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
        assertThat(wave.enemyCount(EnemyFactory.Enemy.Square)).isEqualTo(1);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(context, 251, 2, 3, new WaveContent(List.of()));

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }
}
