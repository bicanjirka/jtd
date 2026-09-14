package td.projectile;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissileProjectileTest {

    @Test
    void homesOnItsTargetsCurrentPositionEachTickRatherThanWhereItStarted() {
        FakeTargetMob target = new FakeTargetMob(100, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, 10f, t -> {
        });

        missile.doTick(1); // aims at (100, 0)
        target.moveTo(100, 100); // target has since moved
        missile.doTick(2); // must aim at the NEW position, not the stale one

        assertThat(missile.getY()).isGreaterThan(0.0);
    }

    @Test
    void impactsOnceItReachesItsTargetAndReportsExactlyThatTarget() {
        FakeTargetMob target = new FakeTargetMob(20, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        List<EnemyMob> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, 10f, impacts::add);

        for (int t = 1; t <= 5 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }

        assertThat(missile.isFinished()).isTrue();
        assertThat(impacts).containsExactly(target);
    }

    @Test
    void retargetsToTheNearestRemainingEnemyWhenItsTargetBecomesInvalid() {
        FakeTargetMob original = new FakeTargetMob(20, 0);
        FakeTargetMob nearest = new FakeTargetMob(5, 0);
        FakeTargetMob further = new FakeTargetMob(50, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(original, nearest, further);
        List<EnemyMob> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, original, registry, 10f, impacts::add);

        original.invalidate();
        for (int t = 1; t <= 5 && !missile.isFinished(); t++) {
            missile.doTick(t);
        }

        assertThat(impacts).containsExactly(nearest);
    }

    @Test
    void givesUpWithoutImpactingAnythingWhenNoValidTargetRemainsAnywhere() {
        FakeTargetMob onlyTarget = new FakeTargetMob(20, 0);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(onlyTarget);
        List<EnemyMob> impacts = new ArrayList<>();
        MissileProjectile missile = new MissileProjectile(0, 0, onlyTarget, registry, 10f, impacts::add);

        onlyTarget.invalidate();
        missile.doTick(1);

        assertThat(missile.isFinished()).isTrue();
        assertThat(impacts).isEmpty();
    }
}
