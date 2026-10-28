package td.enemy;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.PoolTuning;
import td.fixtures.EnemyFixtures;
import td.fixtures.LevelFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.util.GameWorld;
import td.util.RecordingGameHost;
import td.wave.Vec2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefinedEnemyMobTest {

    private static GameWorld newContext() {
        return WorldFixtures.newWorldOnBoard(1, 1001, 1001);
    }

    @Test
    void enemyWithZeroDelayIsActiveImmediately() {
        GameWorld context = newContext();

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void enemyStaysInactiveUntilItsDelayElapses() {
        EnemyDefinition plain = EnemyFixtures.simpleDefinition("plain");
        SpawnParameters oneSlotBack = SpawnParameters.atSlot(1, plain.baseSpeed(), 50, 3);
        EnemyMob enemy = new DefinedEnemyMob(plain, newContext(), oneSlotBack, Rank.GRUNT);
        boolean activeAtOnce = enemy.validTarget();
        for (int t = 1; t < oneSlotBack.delayTicks(); t++) {
            enemy.doTick(t);
        }
        boolean activeJustBefore = enemy.validTarget();

        enemy.doTick(oneSlotBack.delayTicks());

        assertThat(activeAtOnce).isFalse();
        assertThat(activeJustBefore).isFalse();
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void lethalDamageKillsTheEnemyAndCreditsThePlayer() {
        RecordingGameHost host = new RecordingGameHost();
        GameWorld context = WorldFixtures.newWorldOnBoard(host, 1, 1001, 1001);

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 7);
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

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 7);
        enemy.doDamage(Damage.physical(5000));
        enemy.doDamage(Damage.physical(5000));

        assertThat(context.economy().getScore()).isEqualTo(7);
        assertThat(context.economy().getCredits()).isEqualTo(7);
        assertThat(context.enemies().aliveCount()).isZero();
    }

    @Test
    void nonLethalDamageReducesHealthWithoutKilling() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        enemy.doDamage(Damage.physical(2000));

        assertThat(enemy.getHealth()).isEqualTo(3000);
        assertThat(enemy.validTarget()).isTrue();
    }

    @Test
    void aBrokenShieldLetsTheRestOfTheHitThrough() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        assertThat(enemy.getHealth()).isEqualTo(5000);
        enemy.applyEffect(Effect.shield(400f, 5, DamageSink.none()));

        Damage landed = enemy.doDamage(Damage.physical(1000));

        assertThat(landed).isEqualTo(Damage.physical(600));
        assertThat(enemy.getHealth()).isEqualTo(4400);
        assertThat(enemy.hasEffect(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void spiritScalesAShieldWhenItIsApplied() {
        GameWorld context = newContext();
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.sickened(50));

        enemy.applyEffect(Effect.shield(400f, 100, DamageSink.none()));

        assertThat(enemy.shieldGauge().max()).isCloseTo(2f, within(1e-4f));
    }

    @Test
    void aCauterizedEnemyReceivesHalfAShield() {
        GameWorld context = newContext();
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.burn(Damage.magic(100), 60, DamageSink.none())
                .withTuning(PoolTuning.standard().withCarried(
                        StatModifiers.of(EnemyStat.HEALING_RECEIVED, StatModifier.flat(-0.5f)))));

        enemy.applyEffect(Effect.shield(400f, 100, DamageSink.none()));

        assertThat(enemy.shieldGauge().max()).isCloseTo(2f, within(1e-4f));
    }

    @Test
    void aCritFromAnAttackerThatPiercesShieldsGoesStraightToHealth() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.shield(5000f, 100, DamageSink.none()));
        AttackProfile cleanShot = AttackProfile.critChance(1f).withCritsPierceShields();

        HitResult crit = enemy.takeHit(Damage.physical(1000), cleanShot);
        HitResult plain = enemy.takeHit(Damage.physical(1000), AttackProfile.none().withCritsPierceShields());

        assertThat(crit.absorbed()).isZero();
        assertThat(crit.landed().critical()).isTrue();
        assertThat(plain.absorbed()).isEqualTo(1000);
        assertThat(plain.landed().amount()).isZero();
    }

    @Test
    void aShieldTakesAnOrdinaryHitButNoneOfAnAttackThatIgnoresShields() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.shield(400f, 100, DamageSink.none()));

        HitResult ordinary = enemy.takeHit(Damage.physical(300), AttackProfile.none());
        HitResult ignoring = enemy.takeHit(Damage.physical(300), AttackProfile.none().withIgnoredShields());

        assertThat(ordinary.absorbed()).isEqualTo(300);
        assertThat(ignoring.absorbed()).isZero();
        assertThat(ignoring.landed()).isEqualTo(Damage.physical(300));
    }

    @Test
    void oneTickAdvancesTheEnemyBySpeedPixelsAlongTheCurrentSegment() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100)); // one straight segment, 100px long at scale 1

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
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

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

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
    void aCriticalHitIsCapturedOnTheMobsOwnNextDoTickNotSynchronouslyInDoDamage() {
        GameWorld context = newContext();
        DefinedEnemyMob mob = EnemyFixtures.plainMob(context, 50, 3);

        mob.doDamage(Damage.physical(100), AttackProfile.critChance(1f));

        assertThat(mob.ticksSinceCriticalHit(0)).isEqualTo(-1);

        mob.doTick(1);

        assertThat(mob.ticksSinceCriticalHit(1)).isZero();
        assertThat(mob.ticksSinceCriticalHit(2)).isEqualTo(1);
    }

    @Test
    void aNonCriticalHitLeavesTicksSinceCriticalHitAtMinusOne() {
        GameWorld context = newContext();
        DefinedEnemyMob mob = EnemyFixtures.plainMob(context, 50, 3);

        mob.doDamage(Damage.physical(100));
        mob.doTick(1);

        assertThat(mob.ticksSinceCriticalHit(1)).isEqualTo(-1);
    }

    @Test
    void previousPositionTracksOneTickBehindCurrentPosition() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(10, 0, 10));
        DefinedEnemyMob mob = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(mob.getPrevX()).isEqualTo(mob.getX());
        assertThat(mob.getPrevY()).isEqualTo(mob.getY());
        double spawnX = mob.getX();
        double spawnY = mob.getY();

        mob.doTick(1);

        assertThat(mob.getPrevX()).isEqualTo(spawnX);
        assertThat(mob.getPrevY()).isEqualTo(spawnY);
        assertThat(mob.getX()).isNotEqualTo(spawnX);
    }

    @Test
    void reachingTheEndOfThePathSnapsRatherThanInterpolatingAcrossTheBoard() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int initialLives = context.economy().getLives();

        DefinedEnemyMob mob = EnemyFixtures.plainMob(context, 10, 3);
        int tick = 0;
        while (context.economy().getLives() == initialLives) {
            tick++;
            mob.doTick(tick);
        }

        // wrapping to the start is a teleport; prev must not span the board
        assertThat(mob.getPrevX()).isEqualTo(mob.getX());
        assertThat(mob.getPrevY()).isEqualTo(mob.getY());
    }

    @Test
    void enemyReachingEndOfPathDespawnsPermanentlyInsteadOfLoopingBackForABounty() {
        RecordingGameHost host = new RecordingGameHost();
        GameWorld context = WorldFixtures.newWorldOnBoard(host, 1, 1001, 1001);
        context.setPath(LevelFixtures.straightPath(10, 0, 1));
        int creditsBeforeLeak = context.economy().getCredits();

        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
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

        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 0);
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

        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(enemy.getX()).isNegative();
    }

    @Test
    void anEnemyDespawnsWhileStillVisiblyPastTheBoardsFarEdge() {
        GameWorld context = newContext();
        context.setBoard(BoardGeometry.of(10, 5, 5));
        context.setPath(LevelFixtures.straightPath(10, 0, 6));

        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
        for (int i = 1; i <= 100; i++) {
            enemy.doTick(i);
        }

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getX()).isGreaterThan(context.getBoard().maxX());
    }

    @Test
    void anEnemyOnAPathKnowsWhereItWillStandAFewTicksFromNowAtItsCurrentPace() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 500));
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.doTick(1);
        double now = enemy.getX();

        Vec2 later = enemy.positionAfter(10);

        assertThat(later.x()).isEqualTo(now + 10 * enemy.getSpeed(), within(1e-6));
    }
}
