package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PreferringSelectorTest {

    private static FakeEnemyMob prioritised(int health) {
        FakeEnemyMob mob = FakeEnemyMob.at(0, 0).withHealth(health);
        mob.applyEffect(Effect.priority(100, d -> {
        }));
        return mob;
    }

    @Test
    void aPriorityEnemyBeatsTheOneTheInnerSelectorWouldPick() {
        EnemyMob strong = FakeEnemyMob.at(0, 0).withHealth(9000);
        EnemyMob marked = prioritised(10);

        Optional<EnemyMob> selected = PreferringSelector.priority(new HighestHealthSelector())
                .selectFrom(List.of(strong, marked));

        assertThat(selected).contains(marked);
    }

    @Test
    void theInnerSelectorChoosesAmongSeveralPriorityEnemies() {
        EnemyMob weak = prioritised(10);
        EnemyMob stronger = prioritised(500);
        EnemyMob unmarked = FakeEnemyMob.at(0, 0).withHealth(9000);

        Optional<EnemyMob> selected = PreferringSelector.priority(new HighestHealthSelector())
                .selectFrom(List.of(weak, unmarked, stronger));

        assertThat(selected).contains(stronger);
    }

    @Test
    void withNoPriorityEnemyItPicksAsTheInnerSelectorDoes() {
        EnemyMob weak = FakeEnemyMob.at(0, 0).withHealth(10);
        EnemyMob strong = FakeEnemyMob.at(0, 0).withHealth(9000);

        Optional<EnemyMob> selected = PreferringSelector.priority(new HighestHealthSelector())
                .selectFrom(List.of(weak, strong));

        assertThat(selected).contains(strong);
    }

    @Test
    void selectingFromNoCandidatesReturnsEmpty() {
        assertThat(PreferringSelector.priority(new HighestHealthSelector()).selectFrom(List.of())).isEmpty();
    }

    @Test
    void stoppedFirstPrefersAFrozenEnemyOverWhatTheInnerSelectorWouldPick() {
        FakeEnemyMob strong = FakeEnemyMob.at(0, 0).withHealth(9000);
        FakeEnemyMob frozen = FakeEnemyMob.at(0, 0).withHealth(10);
        frozen.reportFrozen();

        var selected = PreferringSelector.stoppedFirst(new HighestHealthSelector()).selectFrom(List.of(strong, frozen));

        assertThat(selected).contains(frozen);
    }
}
