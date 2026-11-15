package td.projectile;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissileProjectileTest {

    private static final float TOP_SPEED = 8f;

    private static MissileProjectile missileAt(EnemyMob target, double heading, FakeMissileGuide guide) {
        return new MissileProjectile(0, 0, heading, target, ProjectileStats.of(TOP_SPEED), guide);
    }

    private static void fly(MissileProjectile missile) {
        for (int t = 1; t <= 300 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }
    }

    @Test
    void itLeavesSlowAndSpeedsUpGraduallyToItsTopSpeed() {
        MissileProjectile missile = missileAt(FakeEnemyMob.at(2000, 0), 0, new FakeMissileGuide());
        List<Double> speeds = new ArrayList<>();

        for (int t = 1; t <= 30; t++) {
            missile.doTick(t);
            speeds.add(missile.speed());
        }

        assertThat(speeds.getFirst()).isLessThan(TOP_SPEED / 2.0);
        assertThat(speeds).isSorted();
        assertThat(speeds.getLast()).isEqualTo(TOP_SPEED);
    }

    @Test
    void farFromItsTargetItTurnsOnACurveRatherThanJumpingToItsHeading() {
        MissileProjectile missile = missileAt(FakeEnemyMob.at(0, 2000), 0, new FakeMissileGuide());

        missile.doTick(1);

        assertThat(missile.headingRadians()).isBetween(0.01, Math.PI / 4);
    }

    @Test
    void launchedAwayFromAnEnemyBesideItItStillReachesItWithoutCircling() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 30);
        FakeMissileGuide guide = new FakeMissileGuide();
        MissileProjectile missile = missileAt(target, -Math.PI / 2, guide);
        int ticks = 0;

        while (!missile.isFinished() && ticks < 300) {
            missile.doTick(++ticks);
        }

        assertThat(guide.landed()).containsExactly(target);
        assertThat(ticks).isLessThan(40);
    }

    @Test
    void itFollowsItsTargetWhereItHasMovedTo() {
        FakeEnemyMob target = FakeEnemyMob.at(100, 0);
        FakeMissileGuide guide = new FakeMissileGuide();
        MissileProjectile missile = missileAt(target, 0, guide);

        missile.doTick(1);
        target.moveTo(100, 100);
        fly(missile);

        assertThat(guide.landed()).containsExactly(target);
        assertThat(missile.getY()).isEqualTo(100.0);
    }

    @Test
    void aTargetItsGuideGivesUpIsLeftForTheOneTheGuideTurnsItTo() {
        FakeEnemyMob first = FakeEnemyMob.at(100, 0);
        FakeEnemyMob next = FakeEnemyMob.at(60, 40);
        FakeMissileGuide guide = new FakeMissileGuide().dropping(first).turningTo(next);
        MissileProjectile missile = missileAt(first, 0, guide);

        fly(missile);

        assertThat(guide.landed()).containsExactly(next);
    }

    @Test
    void withNothingToHuntItFliesBackToWhereItWasLaunched() {
        FakeEnemyMob target = FakeEnemyMob.at(100, 0);
        FakeMissileGuide guide = new FakeMissileGuide();
        MissileProjectile missile = missileAt(target, 0, guide);

        missile.doTick(1);
        missile.doTick(2);
        target.invalidate();
        fly(missile);

        assertThat(guide.landed()).isEmpty();
        assertThat(guide.homes()).isEqualTo(1);
        assertThat(missile.getX()).isZero();
        assertThat(missile.getY()).isZero();
    }

    @Test
    void itRemembersItsLastFewPositionsForTheSmokeItDraws() {
        MissileProjectile missile = missileAt(FakeEnemyMob.at(2000, 0), 0, new FakeMissileGuide());

        for (int t = 1; t <= 20; t++) {
            missile.doTick(t);
        }

        assertThat(missile.trail()).hasSizeBetween(2, 10);
        assertThat(missile.trail().getLast().x()).isGreaterThan(missile.trail().getFirst().x());
    }
}
