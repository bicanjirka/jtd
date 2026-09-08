package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyRosterTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final EnemyRoster roster = new EnemyRoster(host);

    private static EnemyMob anEnemy() {
        return EnemyFactory.getEnemy("c", new Context(new RecordingGameHost()), 0, 50, 3, 1);
    }

    @Test
    void setEnemiesReplacesWhatGetEnemiesReturns() {
        EnemyMob[] enemies = {anEnemy()};

        roster.setEnemies(enemies);

        assertThat(roster.getEnemies()).isSameAs(enemies);
    }

    @Test
    void removingAnEnemyDecrementsTheCountAndReportsItToTheHost() {
        roster.setCount(3);

        roster.remove();

        assertThat(host.enemyDiedCalls).containsExactly(2);
    }

    @Test
    void removingAllEnemiesClearsTheArrayAndZeroesTheCount() {
        roster.setEnemies(new EnemyMob[]{anEnemy()});
        roster.setCount(5);

        roster.removeAll();

        assertThat(roster.getEnemies()).isEmpty();
        assertThat(host.enemyDiedCalls).containsExactly(0);
    }
}
