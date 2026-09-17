package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
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
    void spawningTwiceProducesTwoIndependentSetsOfEnemies() {
        // spawn() is a factory, not an accessor: it binds fresh mobs to the path installed at
        // the moment it is called, which is what lets a level be published in one write.
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("c c", this.catalog));

        EnemyMob[] first = wave.spawn();
        EnemyMob[] second = wave.spawn();

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(first[0]).isNotSameAs(second[0]);
    }

    @Test
    void aSpacerOccupiesATimingSlotButSpawnsNoMob() {
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("c e c", this.catalog));

        assertThat(wave.spawn()).hasSize(2);
        assertThat(wave.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        EnemyDefinition simple = this.catalog.get("c");
        EnemyDefinition armored = this.catalog.get("s");
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("2 c s", this.catalog));

        assertThat(wave.enemySet()).containsExactlyInAnyOrder(simple, armored);
        assertThat(wave.enemyCount(simple)).isEqualTo(2);
        assertThat(wave.enemyCount(armored)).isEqualTo(1);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(this.context, 251, 2, 3, new WaveContent(List.of()));

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }
}
