package td.enemy;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.util.GameWorld;
import td.util.RecordingGameHost;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises AbstractEnemyMob's damage/death and doTick movement, through
 * EnemyFactory.getEnemy("c", ...) (which builds a real EnemyMobCircle via
 * the same constructor production uses).
 */
class AbstractEnemyMobTest {

    private static GameWorld newContext() {
        GameWorld context = new GameWorld(new RecordingGameHost());
        context.setBoard(BoardGeometry.of(1, 1001, 1001));
        return context;
    }

    // xCoords are cell coordinates, converted to pixel-space cell centers the same way
    // production code (PathBuilder) does, since PathNormal stores pixel points directly.
    private static PathNormal straightPath(int scale, int... xCoords) {
        List<Vec2> points = new ArrayList<>();
        for (int x : xCoords) {
            points.add(new Vec2(x * scale + (scale / 2), scale / 2));
        }
        return new PathNormal(points);
    }

    @Test
    void enemyWithZeroDelayIsActiveImmediately() {
        GameWorld context = newContext();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void enemyStaysInactiveUntilItsDelayElapses() {
        GameWorld context = newContext();
        int delayArg = 1;
        float speed = 1.28f; // AbstractEnemyMob's default speed field, in pixels/tick
        int expectedActivationTick = Math.round(22.4f * delayArg / speed);

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
        GameWorld context = new GameWorld(host);
        context.setBoard(BoardGeometry.of(1, 1001, 1001));
        context.setEnemyCount(1);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 7, 2);
        assertThat(enemy.getHealth()).isEqualTo(5000);

        enemy.doDamage(Damage.of(5000));

        assertThat(enemy.validTarget()).isFalse();
        assertThat(context.getScore()).isEqualTo(7);
        assertThat(context.getCredits()).isEqualTo(7);
        assertThat(host.enemyDiedCalls).containsExactly(0);
    }

    @Test
    void anAlreadyDeadEnemyDamagedAgainDoesNotPayTheBountyTwice() {
        RecordingGameHost host = new RecordingGameHost();
        GameWorld context = new GameWorld(host);
        context.setBoard(BoardGeometry.of(1, 1001, 1001));
        context.setEnemyCount(1);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 7, 2);
        enemy.doDamage(Damage.of(5000));
        enemy.doDamage(Damage.of(5000));

        assertThat(context.getScore()).isEqualTo(7);
        assertThat(context.getCredits()).isEqualTo(7);
        assertThat(host.enemyDiedCalls).containsExactly(0);
    }

    @Test
    void nonLethalDamageReducesHealthWithoutKilling() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        enemy.doDamage(Damage.of(2000));

        assertThat(enemy.getHealth()).isEqualTo(3000);
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void oneTickAdvancesTheEnemyBySpeedPixelsAlongTheCurrentSegment() {
        GameWorld context = newContext();
        context.setPath(straightPath(1, 0, 100)); // one straight segment, 100px long at scale 1

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        assertThat(enemy.getX()).isEqualTo(0.0);
        assertThat(enemy.getY()).isEqualTo(0.0);

        enemy.doTick(1);

        assertThat(enemy.getX()).isCloseTo(enemy.getSpeed(), within(1e-9));
        assertThat(enemy.getY()).isEqualTo(0.0);
    }

    /**
     * This is the behavior the old fixed-point model got wrong: every segment used to take
     * the same number of ticks to cross regardless of its physical length (segmentProgression
     * always ran 0-999 once per segment). Real arc-length movement crosses a segment twice as
     * long in (roughly) twice as many ticks - this is what "distance-based progression" means,
     * and what makes a smoothed, curved path move at a consistent real-world pace.
     */
    @Test
    void crossingASegmentTwiceAsLongTakesRoughlyTwiceAsManyTicks() {
        GameWorld context = newContext();
        // segment lengths 10, then 20, then a trailing 1 so reaching x=30 happens strictly
        // before the path wraps (which would otherwise snap x back near 0 exactly at x=30,
        // and the loop below would never observe x >= 30).
        context.setPath(straightPath(1, 0, 10, 30, 31));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        int tick = 0;
        while (enemy.getX() < 10.0) {
            tick++;
            enemy.doTick(tick);
        }
        int ticksForFirstSegment = tick;

        while (enemy.getX() < 30.0) {
            tick++;
            enemy.doTick(tick);
        }
        int ticksForSecondSegment = tick - ticksForFirstSegment;

        assertThat(Math.abs(ticksForSecondSegment - 2 * ticksForFirstSegment)).isLessThanOrEqualTo(1);
    }

    @Test
    void deathFadeCompletesExactlyFadeDurationTicksAfterTheTickThatNoticedDeath() {
        GameWorld context = newContext();
        int level = 2;
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, level);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        int fadeDuration = 3 * level + 6;

        enemy.doDamage(Damage.of(5000));
        enemy.doTick(1); // first doTick after death: captures deathTick = 1

        assertThat(mob.isFadeComplete(1)).isFalse();
        assertThat(mob.isFadeComplete(1 + fadeDuration)).isFalse();
        assertThat(mob.isFadeComplete(1 + fadeDuration + 1)).isTrue();
    }

    @Test
    void fadeAlphaStaysWithinValidColorRangeBeforeDeathTickIsCaptured() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;

        // doDamage() (called by a tower) can kill an enemy mid-tick, before this
        // mob's own doTick() next runs to capture deathTick - ticksSinceDeath()
        // is -1 in that window, and a render can land here too.
        enemy.doDamage(Damage.of(5000));

        assertThat(mob.ticksSinceDeath(0)).isEqualTo(-1);
        assertThat(mob.fadeAlpha(-1)).isBetween(0, 255);
    }

    @Test
    void previousPositionTracksOneTickBehindCurrentPosition() {
        GameWorld context = newContext();
        context.setPath(straightPath(10, 0, 10));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;

        // nothing to interpolate from at spawn
        assertThat(mob.getPrevX()).isEqualTo(mob.getX());
        assertThat(mob.getPrevY()).isEqualTo(mob.getY());
        double spawnX = mob.getX();
        double spawnY = mob.getY();

        enemy.doTick(1);

        assertThat(mob.getPrevX()).isEqualTo(spawnX);
        assertThat(mob.getPrevY()).isEqualTo(spawnY);
        assertThat(mob.getX()).isNotEqualTo(spawnX);
    }

    @Test
    void reachingTheEndOfThePathSnapsRatherThanInterpolatingAcrossTheBoard() {
        GameWorld context = newContext();
        context.setPath(straightPath(10, 0, 1));
        int initialLives = context.getLives();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        int tick = 0;
        while (context.getLives() == initialLives) {
            tick++;
            enemy.doTick(tick);
        }

        // the enemy just reappeared at the path's start - a genuine teleport, not motion
        // along the path - so prev/current must coincide rather than spanning the board
        assertThat(mob.getPrevX()).isEqualTo(mob.getX());
        assertThat(mob.getPrevY()).isEqualTo(mob.getY());
    }

    @Test
    void enemyReachingEndOfPathCostsALife() {
        GameWorld context = newContext();
        context.setPath(straightPath(10, 0, 1));
        int initialLives = context.getLives();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(context.getLives()).isLessThan(initialLives);
    }
}
