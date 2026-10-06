package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyRosterTest {

    private final EnemyRoster roster = new EnemyRoster();

    private static EnemyMob anEnemy() {
        return EnemyFactory.getEnemy("c", WorldFixtures.newWorld(), 0, 50, 3, Rank.GRUNT);
    }

    @Test
    void setEnemiesReplacesWhatGetEnemiesReturns() {
        EnemyMob[] enemies = {anEnemy()};

        roster.setEnemies(enemies);

        // content, not identity: getEnemies() returns a fresh copy
        assertThat(roster.getEnemies()).containsExactly(enemies);
    }

    @Test
    void addAppendsAnEnemyAndGrowsTheCount() {
        roster.setEnemies(new EnemyMob[]{anEnemy()});
        roster.setCount(1);
        EnemyMob reinforcement = anEnemy();

        roster.add(reinforcement);

        assertThat(roster.getEnemies()).hasSize(2).contains(reinforcement);
    }

    @Test
    void replaceSwapsOneEnemyForAnotherWithoutReportingAKill() {
        EnemyMob outgoing = anEnemy();
        roster.setEnemies(new EnemyMob[]{outgoing});
        roster.setCount(1);
        EnemyMob incoming = anEnemy();

        roster.replace(outgoing, incoming);

        assertThat(roster.getEnemies()).containsExactly(incoming);
        assertThat(roster.aliveCount()).isEqualTo(1);
    }

    @Test
    void reportingADeathDecrementsTheAliveCount() {
        roster.setCount(3);

        roster.reportDeath(anEnemy());

        assertThat(roster.aliveCount()).isEqualTo(2);
    }

    @Test
    void clearingEveryEnemyZeroesTheCount() {
        roster.setEnemies(new EnemyMob[]{anEnemy()});
        roster.setCount(5);

        roster.clear();

        assertThat(roster.getEnemies()).isEmpty();
        assertThat(roster.aliveCount()).isZero();
    }
}
