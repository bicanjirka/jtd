package td.projectile;

import org.junit.jupiter.api.Test;
import td.enemy.HitReceiver;
import td.fixtures.FakeEnemyMob;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissileProjectileTest {

    @Test
    void homesOnItsTargetsCurrentPositionEachTickRatherThanWhereItStarted() {
        FakeEnemyMob target = FakeEnemyMob.at(100, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, ProjectileStats.of(10f), t -> {
        });

        missile.doTick(1); // aims at (100, 0)
        target.moveTo(100, 100); // target has since moved
        missile.doTick(2); // must aim at the NEW position, not the stale one

        assertThat(missile.getY()).isGreaterThan(0.0);
    }

    @Test
    void impactsOnceItReachesItsTargetAndReportsExactlyThatTarget() {
        FakeEnemyMob target = FakeEnemyMob.at(20, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        List<HitReceiver> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, ProjectileStats.of(10f), impacts::add);

        for (int t = 1; t <= 5 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }

        assertThat(missile.isFinished()).isTrue();
        assertThat(impacts).containsExactly(target);
    }

    @Test
    void retargetsToTheNearestRemainingEnemyWhenItsTargetBecomesInvalid() {
        FakeEnemyMob original = FakeEnemyMob.at(20, 0);
        FakeEnemyMob nearest = FakeEnemyMob.at(5, 0);
        FakeEnemyMob further = FakeEnemyMob.at(50, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(original, nearest, further);
        List<HitReceiver> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, original, registry, ProjectileStats.of(10f), impacts::add);

        original.invalidate();
        for (int t = 1; t <= 5 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }

        assertThat(impacts).containsExactly(nearest);
    }

    @Test
    void keepsItsLockOnATargetThatIsHiddenInsteadOfRetargetingToAVisibleOne() {
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(20, 0);
        FakeEnemyMob visible = FakeEnemyMob.at(5, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(hidden, visible);
        List<HitReceiver> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, hidden, registry, ProjectileStats.of(10f),
                impacts::add);

        for (int t = 1; t <= 5 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }

        assertThat(impacts).containsExactly(hidden);
    }

    @Test
    void aSlowMissileTakesTicksInProportionToItsSpeedAndRemembersItsLastPositions() {
        FakeEnemyMob target = FakeEnemyMob.at(80, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, ProjectileStats.of(8f), t -> {
        });

        int ticks = 0;
        while (!missile.isFinished()) {
            missile.doTick(++ticks);
        }

        assertThat(ticks).isEqualTo(10);
        assertThat(missile.trail()).hasSize(6);
        assertThat(missile.trail().getFirst().x()).isLessThan(missile.trail().getLast().x());
    }

    @Test
    void givesUpWithoutImpactingAnythingWhenNoValidTargetRemainsAnywhere() {
        FakeEnemyMob onlyTarget = FakeEnemyMob.at(20, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(onlyTarget);
        List<HitReceiver> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, onlyTarget, registry, ProjectileStats.of(10f), impacts::add);

        onlyTarget.invalidate();
        missile.doTick(1);

        assertThat(missile.isFinished()).isTrue();
        assertThat(impacts).isEmpty();
    }
}
