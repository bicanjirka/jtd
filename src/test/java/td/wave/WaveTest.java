package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
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
    private final EnemyCatalog catalog = EnemyCatalog.builtIn();

    @Test
    void oneEnemyMobIsSpawnedPerSpawnSlotIncludingEmpties() {
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("c e c", this.catalog));

        assertThat(wave.getEnemies()).hasSize(3);
        assertThat(wave.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        EnemyDefinition circle = this.catalog.get("c");
        EnemyDefinition square = this.catalog.get("s");
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("2 c s", this.catalog));

        assertThat(wave.enemySet()).containsExactlyInAnyOrder(circle, square);
        assertThat(wave.enemyCount(circle)).isEqualTo(2);
        assertThat(wave.enemyCount(square)).isEqualTo(1);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(this.context, 251, 2, 3, new WaveContent(List.of()));

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }
}
