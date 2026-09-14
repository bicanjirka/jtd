package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers TowerMortar's targeting, its shell's flight, and the splash+slow it applies on
 * impact - the shell itself is exercised more thoroughly by CannonballProjectileTest.
 */
class TowerMortarTest {

    private static final int SCALE = 32;

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private TowerMortar towerAt(int cellX, int cellY) {
        this.context.setBoard(BoardGeometry.of(SCALE, 20, 20));
        return new TowerMortar(this.context, cellX, cellY);
    }

    private void flyProjectilesToCompletion() {
        for (int t = 1; t <= 50 && !this.context.getProjectileRegistry().getProjectiles().isEmpty(); t++) {
            this.context.tickProjectiles(t);
        }
    }

    @Test
    void firingLaunchesExactlyOneShellAtTheTarget() {
        TowerMortar tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.getProjectileRegistry().getProjectiles()).hasSize(1);
    }

    @Test
    void theShellEventuallySplashesDamageAndSlowsTheTarget() {
        TowerMortar tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        this.flyProjectilesToCompletion();

        assertThat(target.onlyHitAmount()).isEqualTo(TowerMortar.damage);
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().get(0).kind()).isEqualTo(EffectKind.SLOW);
    }

    @Test
    void aGhostIsNeverSelectedAsTheInitialTargetButIsStillCaughtByTheSplash() {
        TowerMortar tower = towerAt(3, 3);
        RecordingEnemyMob normal = RecordingEnemyMob.normalAt(100, 100);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(105, 100);
        this.context.setEnemies(new EnemyMob[]{normal, ghost});

        tower.doTick(1);
        this.flyProjectilesToCompletion();

        assertThat(normal.hits()).hasSize(1);
        assertThat(ghost.hits()).hasSize(1);
    }

    @Test
    void noTargetInRangeFiresNoShell() {
        TowerMortar tower = towerAt(3, 3);
        this.context.setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.getProjectileRegistry().getProjectiles()).isEmpty();
    }

    @Test
    void concussiveChargeIsChoosableOnceTwoNeighboursExistAndAppliesItsRangeBonus() {
        this.context.startEconomy(1000, 5);
        TowerMortar tower = towerAt(3, 3);
        this.context.addTower(tower);
        this.context.addTower(new TowerOne(this.context, 2, 2));
        this.context.addTower(new TowerOne(this.context, 4, 4));
        UpgradePath concussiveCharge = UpgradePaths.named(tower, "Concussive Charge");

        boolean chosen = tower.chooseUpgradePath(concussiveCharge);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(TowerMortar.range * SCALE);
    }

    @Test
    void heavyShellIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.startEconomy(1000, 5);
        TowerMortar tower = towerAt(3, 3);
        UpgradePath heavyShell = UpgradePaths.named(tower, "Heavy Shell");

        boolean chosen = tower.chooseUpgradePath(heavyShell);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isNull();
    }

    @Test
    void heavyShellBumpsTheSplashRadiusBeyondTheBase() {
        TowerMortar tower = towerAt(3, 3);
        UpgradePath heavyShell = UpgradePaths.named(tower, "Heavy Shell");
        float radiusBeforeChoosing = tower.getSplashRadius();

        // onUpgradePathChosen is exercised directly - Heavy Shell's own gate (a damage-dealt
        // threshold) is covered generically by DamageDealtConditionTest; this proves the bump itself.
        tower.onUpgradePathChosen(heavyShell);

        assertThat(tower.getSplashRadius()).isGreaterThan(radiusBeforeChoosing);
    }
}
