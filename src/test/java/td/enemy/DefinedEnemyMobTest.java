package td.enemy;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.fixtures.LevelFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefinedEnemyMobTest {

    private static GameWorld newContext() {
        return WorldFixtures.newWorldOnBoard(1, 1001, 1001);
    }

    @Test
    void enemyWithZeroDelayIsActiveImmediately() {
        GameWorld context = newContext();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void enemyStaysInactiveUntilItsDelayElapses() {
        GameWorld context = newContext();
        int delayArg = 1;
        float speed = 1.28f; // default speed, px/tick
        int expectedActivationTick = Math.round(22.4f * delayArg / speed);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, delayArg, 50, 3, Rank.GRUNT);
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
        GameWorld context = WorldFixtures.newWorldOnBoard(host, 1, 1001, 1001);
        context.enemies().setCount(1);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 7, Rank.GRUNT);
        assertThat(enemy.getHealth()).isEqualTo(5000);

        enemy.doDamage(Damage.physical(5000));

        assertThat(enemy.validTarget()).isFalse();
        assertThat(context.economy().getScore()).isEqualTo(7);
        assertThat(context.economy().getCredits()).isEqualTo(7);
        assertThat(context.enemies().aliveCount()).isZero();
    }

    @Test
    void anAlreadyDeadEnemyDamagedAgainDoesNotPayTheBountyTwice() {
        RecordingGameHost host = new RecordingGameHost();
        GameWorld context = WorldFixtures.newWorldOnBoard(host, 1, 1001, 1001);
        context.enemies().setCount(1);

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 7, Rank.GRUNT);
        enemy.doDamage(Damage.physical(5000));
        enemy.doDamage(Damage.physical(5000));

        assertThat(context.economy().getScore()).isEqualTo(7);
        assertThat(context.economy().getCredits()).isEqualTo(7);
        assertThat(context.enemies().aliveCount()).isZero();
    }

    @Test
    void nonLethalDamageReducesHealthWithoutKilling() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        enemy.doDamage(Damage.physical(2000));

        assertThat(enemy.getHealth()).isEqualTo(3000);
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void anActiveShieldReducesLandedDamageByItsPercent() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        assertThat(enemy.getHealth()).isEqualTo(5000);
        enemy.applyEffect(Effect.shield(0.4f, 5, d -> {
        }));

        Damage landed = enemy.doDamage(Damage.physical(1000));

        assertThat(landed).isEqualTo(Damage.physical(600));
        assertThat(enemy.getHealth()).isEqualTo(4400);
    }

    @Test
    void oneTickAdvancesTheEnemyBySpeedPixelsAlongTheCurrentSegment() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100)); // one straight segment, 100px long at scale 1

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        assertThat(enemy.getX()).isEqualTo(0.0);
        assertThat(enemy.getY()).isEqualTo(0.0);

        enemy.doTick(1);

        assertThat(enemy.getX()).isCloseTo(enemy.getSpeed(), within(1e-9));
        assertThat(enemy.getY()).isEqualTo(0.0);
    }

    /** Movement is by arc length, so a segment's crossing time scales with its length. */
    @Test
    void crossingASegmentTwiceAsLongTakesRoughlyTwiceAsManyTicks() {
        GameWorld context = newContext();
        // a trailing 1px leg so x=30 is reached strictly before the path wraps back to 0
        context.setPath(LevelFixtures.straightPath(1, 0, 10, 30, 31));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

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
    void ticksSinceCriticalHitIsMinusOneUntilOneLands() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;

        assertThat(mob.ticksSinceCriticalHit(5)).isEqualTo(-1);
    }

    @Test
    void aCriticalHitIsCapturedOnTheMobsOwnNextDoTickNotSynchronouslyInDoDamage() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;

        enemy.doDamage(Damage.physical(100), AttackProfile.critChance(1f));

        assertThat(mob.ticksSinceCriticalHit(0)).isEqualTo(-1);

        enemy.doTick(1);

        assertThat(mob.ticksSinceCriticalHit(1)).isZero();
        assertThat(mob.ticksSinceCriticalHit(2)).isEqualTo(1);
    }

    @Test
    void aNonCriticalHitLeavesTicksSinceCriticalHitAtMinusOne() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;

        enemy.doDamage(Damage.physical(100));
        enemy.doTick(1);

        assertThat(mob.ticksSinceCriticalHit(1)).isEqualTo(-1);
    }

    @Test
    void previousPositionTracksOneTickBehindCurrentPosition() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(10, 0, 10));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;

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
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int initialLives = context.economy().getLives();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        int tick = 0;
        while (context.economy().getLives() == initialLives) {
            tick++;
            enemy.doTick(tick);
        }

        // wrapping to the start is a teleport; prev must not span the board
        assertThat(mob.getPrevX()).isEqualTo(mob.getX());
        assertThat(mob.getPrevY()).isEqualTo(mob.getY());
    }

    @Test
    void enemyReachingEndOfPathCostsALife() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int initialLives = context.economy().getLives();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(context.economy().getLives()).isLessThan(initialLives);
    }

    @Test
    void enemyReachingEndOfPathDespawnsPermanentlyInsteadOfLoopingBackForABounty() {
        RecordingGameHost host = new RecordingGameHost();
        GameWorld context = WorldFixtures.newWorldOnBoard(host, 1, 1001, 1001);
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int creditsBeforeLeak = context.economy().getCredits();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(enemy.isDead()).isTrue();
        assertThat(context.economy().getCredits()).isEqualTo(creditsBeforeLeak);

        enemy.doDamage(Damage.physical(1000));
        assertThat(context.economy().getCredits()).isEqualTo(creditsBeforeLeak);
    }

    @Test
    void aPriceZeroEnemyLeakingCostsTheLifeButNoScore() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int initialLives = context.economy().getLives();
        int initialScore = context.economy().getScore();

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 0, Rank.GRUNT);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(enemy.isDead()).isTrue();
        assertThat(context.economy().getLives()).isEqualTo(initialLives - 1);
        assertThat(context.economy().getScore()).isEqualTo(initialScore);
    }

    @Test
    void anEnemySpawnedAtAnOffBoardPathPointStartsOffscreenAndWalksOntoTheBoard() {
        GameWorld context = newContext();
        context.setBoard(BoardGeometry.of(10, 5, 5));
        context.setPath(LevelFixtures.straightPath(10, -1, 4));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(enemy.getX()).isNegative();
    }

    @Test
    void anEnemyDespawnsWhileStillVisiblyPastTheBoardsFarEdge() {
        GameWorld context = newContext();
        context.setBoard(BoardGeometry.of(10, 5, 5));
        context.setPath(LevelFixtures.straightPath(10, 0, 6));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getX()).isGreaterThan(context.getBoard().maxX());
    }
}
