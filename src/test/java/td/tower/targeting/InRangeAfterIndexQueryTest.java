package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class InRangeAfterIndexQueryTest {

    @Test
    void theFirstMatchAfterTheGivenIndexIsReturned() {
        EnemyRegistry enemies = () -> new EnemyMob[]{
                FakeEnemyMob.at(0, 0),
                FakeEnemyMob.at(0, 0),
                FakeEnemyMob.at(0, 0)};

        OptionalInt next = new InRangeAfterIndexQuery(0, 0, 50, EnemyMob.type.Normal).nextIndexAfter(enemies, 0);

        assertThat(next).hasValue(1);
    }

    @Test
    void scanningDoesNotWrapAroundToEarlierIndices() {
        EnemyRegistry enemies = () -> new EnemyMob[]{
                FakeEnemyMob.at(0, 0),           // index 0: a valid target, but before the given index
                FakeEnemyMob.at(0, 0).invalid()}; // index 1: not a valid target

        OptionalInt next = new InRangeAfterIndexQuery(0, 0, 50, EnemyMob.type.Normal).nextIndexAfter(enemies, 0);

        assertThat(next).isEmpty();
    }

    @Test
    void enemiesOutOfTypeOrRangeAreSkippedInFavorOfTheNextMatch() {
        EnemyRegistry enemies = () -> new EnemyMob[]{
                FakeEnemyMob.at(0, 0).withType(EnemyMob.type.Flying), // index 0: wrong type
                FakeEnemyMob.at(1000, 1000),                          // index 1: out of range
                FakeEnemyMob.at(0, 0)};                               // index 2: matches

        OptionalInt next = new InRangeAfterIndexQuery(0, 0, 50, EnemyMob.type.Normal).nextIndexAfter(enemies, -1);

        assertThat(next).hasValue(2);
    }

    @Test
    void nothingIsFoundWhenAllEnemiesHaveAlreadyBeenScanned() {
        EnemyRegistry enemies = () -> new EnemyMob[]{FakeEnemyMob.at(0, 0)};

        OptionalInt next = new InRangeAfterIndexQuery(0, 0, 50, EnemyMob.type.Normal).nextIndexAfter(enemies, 0);

        assertThat(next).isEmpty();
    }
}
