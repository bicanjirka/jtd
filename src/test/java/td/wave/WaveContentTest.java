package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaveContentTest {

    private final EnemyCatalog catalog = EnemyCatalog.builtIn();
    private final EnemyDefinition simple = this.catalog.get("c");
    private final EnemyDefinition armored = this.catalog.get("s");

    @Test
    void aNormalSlotCountsAsOneMember() {
        WaveContent content = new WaveContent(List.of(new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.normal())));

        assertThat(content.enemyCount()).isEqualTo(1);
        assertThat(content.enemyCount(this.simple)).isEqualTo(1);
    }

    @Test
    void aShapedSlotCountsItsMemberCountNotOne() {
        WaveContent content = new WaveContent(List.of(new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.swarm(4))));

        assertThat(content.enemyCount()).isEqualTo(4);
        assertThat(content.enemyCount(this.simple)).isEqualTo(4);
    }

    @Test
    void memberCountsSumAcrossMultipleShapedSlotsOfTheSameDefinition() {
        WaveContent content = new WaveContent(List.of(
                new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.swarm(3)),
                new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.line(2)),
                new EnemySlot(this.armored, Rank.GRUNT, SpawnShape.armored())));

        assertThat(content.enemyCount()).isEqualTo(6);
        assertThat(content.enemyCount(this.simple)).isEqualTo(5);
        assertThat(content.enemyCount(this.armored)).isEqualTo(1);
    }

    @Test
    void anEmptySlotContributesNoMembers() {
        WaveContent content = new WaveContent(List.of(
                new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.swarm(3)),
                new EmptySlot()));

        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void enemySetListsEachDefinitionOnceRegardlessOfItsMemberCount() {
        WaveContent content = new WaveContent(List.of(
                new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.swarm(4)),
                new EnemySlot(this.simple, Rank.GRUNT, SpawnShape.normal())));

        assertThat(content.enemySet()).containsExactly(this.simple);
    }

    @Test
    void rankForReturnsTheEffectiveRankOfTheSlotThatSpawnsADefinition() {
        WaveContent content = new WaveContent(List.of(new EnemySlot(this.simple, Rank.VETERAN, SpawnShape.normal())));

        assertThat(content.rankFor(this.simple)).isEqualTo(Rank.VETERAN);
    }
}
