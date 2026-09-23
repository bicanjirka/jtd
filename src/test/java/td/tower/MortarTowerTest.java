package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.TowerFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers MortarTower's targeting, its shell's flight, and the splash+slow it applies on
 * impact - the shell itself is exercised more thoroughly by CannonballProjectileTest.
 */
class MortarTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private MortarTower towerAt(int cellX, int cellY) {
        return new MortarTower(this.context, cellX, cellY);
    }

    @Test
    void firingLaunchesExactlyOneShellAtTheTarget() {
        MortarTower tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(1);
    }

    @Test
    void theShellEventuallySplashesDamageAndSlowsTheTarget() {
        MortarTower tower = towerAt(3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.onlyHitAmount()).isEqualTo(MortarTower.DAMAGE);
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.SLOW);
    }

    @Test
    void aGhostIsNeverSelectedAsTheInitialTargetButIsStillCaughtByTheSplash() {
        MortarTower tower = towerAt(3, 3);
        RecordingEnemyMob normal = RecordingEnemyMob.normalAt(100, 100);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(105, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{normal, ghost});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(normal.hits()).hasSize(1);
        assertThat(ghost.hits()).hasSize(1);
    }

    @Test
    void noTargetInRangeFiresNoShell() {
        MortarTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void concussiveChargeIsChoosableOnceTwoNeighboursExistAndAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        MortarTower tower = towerAt(3, 3);
        this.context.towers().add(tower);
        this.context.towers().add(new SniperTower(this.context, 2, 2));
        this.context.towers().add(new SniperTower(this.context, 4, 4));
        UpgradeNode concussiveCharge = UpgradePaths.named(tower, "Concussive Charge");

        boolean chosen = tower.buyUpgrade(concussiveCharge);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(MortarTower.RANGE * BoardFixtures.SCALE);
    }

    @Test
    void heavyShellIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.economy().startEconomy(1000, 5);
        MortarTower tower = towerAt(3, 3);
        UpgradeNode heavyShell = UpgradePaths.named(tower, "Heavy Shell");

        boolean chosen = tower.buyUpgrade(heavyShell);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void heavyShellBumpsTheSplashRadiusBeyondTheBase() {
        MortarTower tower = towerAt(3, 3);
        UpgradeNode heavyShell = UpgradePaths.named(tower, "Heavy Shell");
        float radiusBeforeChoosing = tower.getSplashRadius();

        // onUpgradeBought is exercised directly - Heavy Shell's own gate (a damage-dealt
        // threshold) is covered generically by DamageDealtConditionTest; this proves the bump itself.
        tower.onUpgradeBought(heavyShell);

        assertThat(tower.getSplashRadius()).isGreaterThan(radiusBeforeChoosing);
    }
}
