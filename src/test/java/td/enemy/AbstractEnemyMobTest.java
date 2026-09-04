package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;
import td.wave.PathNormal;
import td.wave.RecordingCell;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises AbstractEnemyMob's damage/death and doTick movement, through
 * EnemyFactory.getEnemy("c", ...) (which builds a real EnemyMobCircle via
 * the same clone-and-doInit pipeline production uses).
 */
class AbstractEnemyMobTest {

    private static Context newContext() {
        Context context = new Context(new RecordingGameHost());
        context.maxX = 1000;
        context.maxY = 1000;
        return context;
    }

    private static PathNormal straightPath(int scale, int... xCoords) {
        PathNormal path = new PathNormal(scale);
        for (int x : xCoords) {
            path.addStep(x, 0);
        }
        int width = 0;
        for (int x : xCoords) width = Math.max(width, x + 1);
        RecordingCell[][] grid = new RecordingCell[width][1];
        for (int x = 0; x < width; x++) {
            grid[x][0] = new RecordingCell();
        }
        path.finalise(grid);
        return path;
    }

    @Test
    void enemyWithZeroDelayIsActiveImmediately() {
        Context context = newContext();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void enemyStaysInactiveUntilItsDelayElapses() {
        Context context = newContext();
        int delayArg = 1;
        int speed = 40; // AbstractEnemyMob's default speed field
        int expectedActivationTick = Math.round(700f * delayArg / speed);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, delayArg, 50, 3, 1);
        assertThat(enemy.validTarget()).isFalse();

        for (int i = 1; i < expectedActivationTick; i++) {
            enemy.doTick(i);
        }
        assertThat(enemy.validTarget()).isFalse();

        enemy.doTick(expectedActivationTick);
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void lethalDamageKillsTheEnemyAndCreditsThePlayer() {
        RecordingGameHost host = new RecordingGameHost();
        Context context = new Context(host);
        context.maxX = 1000;
        context.maxY = 1000;
        context.setEnemyCount(1);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 7, 2);
        assertThat(enemy.getHealth()).isEqualTo(5000);

        enemy.doDamage(5000);

        assertThat(enemy.validTarget()).isFalse();
        assertThat(context.getScore()).isEqualTo(7);
        assertThat(context.getCredits()).isEqualTo(7);
        assertThat(host.enemyDiedCalls).containsExactly(0);
    }

    @Test
    void nonLethalDamageReducesHealthWithoutKilling() {
        Context context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        enemy.doDamage(2000);

        assertThat(enemy.getHealth()).isEqualTo(3000);
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void doTickMovesEnemyAlongPathProportionally() {
        Context context = newContext();
        context.setPath(straightPath(10, 0, 10));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        // pixel centers: step0=(5,5), step1=(105,5); speed=40 => 40/1000 of the way after 1 tick
        assertThat(enemy.getX()).isEqualTo(5);
        assertThat(enemy.getY()).isEqualTo(5);

        enemy.doTick(1);

        assertThat(enemy.getX()).isEqualTo(9); // 5 + (105-5)*40/1000
        assertThat(enemy.getY()).isEqualTo(5);
    }

    @Test
    void enemyReachingEndOfPathCostsALife() {
        Context context = newContext();
        context.setPath(straightPath(10, 0, 1));
        int initialLives = context.getLives();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(context.getLives()).isLessThan(initialLives);
    }
}
