package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers TowerSeeker's targeting and the magic damage + freeze it applies on impact - the
 * missile's own homing/retargeting is exercised more thoroughly by MissileProjectileTest.
 */
class TowerSeekerTest {

    private static final int SCALE = 32;

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private TowerSeeker towerAt(int cellX, int cellY) {
        this.context.setBoard(BoardGeometry.of(SCALE, 20, 20));
        return new TowerSeeker(this.context, cellX, cellY);
    }

    private void flyProjectilesToCompletion() {
        for (int t = 1; t <= 50 && !this.context.getProjectileRegistry().getProjectiles().isEmpty(); t++) {
            this.context.tickProjectiles(t);
        }
    }

    @Test
    void firingLaunchesExactlyOneMissileAtTheTarget() {
        TowerSeeker tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.getProjectileRegistry().getProjectiles()).hasSize(1);
    }

    @Test
    void theMissileEventuallyDealsMagicDamageAndFreezesTheTarget() {
        TowerSeeker tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        this.flyProjectilesToCompletion();

        assertThat(target.onlyHitAmount()).isEqualTo(TowerSeeker.damage);
        assertThat(target.hits().get(0).type()).isEqualTo(DamageType.MAGIC);
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().get(0).kind()).isEqualTo(EffectKind.FREEZE);
    }

    @Test
    void aGhostIsNeverTargeted() {
        TowerSeeker tower = towerAt(3, 3);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(100, 100);
        this.context.setEnemies(new EnemyMob[]{ghost});

        tower.doTick(1);

        assertThat(this.context.getProjectileRegistry().getProjectiles()).isEmpty();
    }

    @Test
    void noTargetInRangeFiresNoMissile() {
        TowerSeeker tower = towerAt(3, 3);
        this.context.setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.getProjectileRegistry().getProjectiles()).isEmpty();
    }
}
