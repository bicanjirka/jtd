package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers SeekerTower's targeting and the magic damage + freeze it applies on impact - the
 * missile's own homing/retargeting is exercised more thoroughly by MissileProjectileTest.
 */
class SeekerTowerTest {

    private static final int SCALE = 32;

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private SeekerTower towerAt(int cellX, int cellY) {
        this.context.setBoard(BoardGeometry.of(SCALE, 20, 20));
        return new SeekerTower(this.context, cellX, cellY);
    }

    private void flyProjectilesToCompletion() {
        for (int t = 1; t <= 50 && !this.context.projectiles().getProjectiles().isEmpty(); t++) {
            this.context.projectiles().doTick(t);
        }
    }

    @Test
    void firingLaunchesExactlyOneMissileAtTheTarget() {
        SeekerTower tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(1);
    }

    @Test
    void theMissileEventuallyDealsMagicDamageAndFreezesTheTarget() {
        SeekerTower tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        this.flyProjectilesToCompletion();

        assertThat(target.onlyHitAmount()).isEqualTo(SeekerTower.DAMAGE);
        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.MAGIC);
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.FREEZE);
    }

    @Test
    void aGhostIsNeverTargeted() {
        SeekerTower tower = towerAt(3, 3);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void noTargetInRangeFiresNoMissile() {
        SeekerTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void twinWarheadIsChoosableWithMoneyAloneAndAppliesItsFireRateBonus() {
        this.context.economy().startEconomy(1000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradePath twinWarhead = UpgradePaths.named(tower, "Twin Warhead");

        boolean chosen = tower.chooseUpgradePath(twinWarhead);

        assertThat(chosen).isTrue();
        assertThat(tower.coolDownCurrent()).isLessThan(tower.coolDownMax);
    }

    @Test
    void deepFreezeIsNotYetChoosableBeforeTenKills() {
        this.context.economy().startEconomy(1000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradePath deepFreeze = UpgradePaths.named(tower, "Deep Freeze");

        boolean chosen = tower.chooseUpgradePath(deepFreeze);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void deepFreezeBumpsTheFreezeDurationBeyondTheBase() {
        SeekerTower tower = towerAt(3, 3);
        UpgradePath deepFreeze = UpgradePaths.named(tower, "Deep Freeze");
        int durationBeforeChoosing = tower.getFreezeDurationTicks();

        // onUpgradePathChosen is exercised directly - Deep Freeze's own gate (a kill-count
        // threshold) is covered generically by KillCountConditionTest; this proves the bump itself.
        tower.onUpgradePathChosen(deepFreeze);

        assertThat(tower.getFreezeDurationTicks()).isGreaterThan(durationBeforeChoosing);
    }
}
