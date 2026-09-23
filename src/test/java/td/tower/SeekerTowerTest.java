package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
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
 * Covers SeekerTower's targeting and the magic damage + freeze it applies on impact - the
 * missile's own homing/retargeting is exercised more thoroughly by MissileProjectileTest.
 */
class SeekerTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private SeekerTower towerAt(int cellX, int cellY) {
        return new SeekerTower(this.context, cellX, cellY);
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
        TowerFixtures.flyProjectilesToCompletion(this.context);

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
        UpgradeNode twinWarhead = UpgradePaths.named(tower, "Twin Warhead");

        boolean chosen = tower.buyUpgrade(twinWarhead);

        assertThat(chosen).isTrue();
        assertThat(tower.coolDownCurrent()).isLessThan(tower.coolDownMax);
    }

    @Test
    void deepFreezeIsNotYetChoosableBeforeTenKills() {
        this.context.economy().startEconomy(1000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradeNode deepFreeze = UpgradePaths.named(tower, "Deep Freeze");

        boolean chosen = tower.buyUpgrade(deepFreeze);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void deepFreezeBumpsTheFreezeDurationBeyondTheBase() {
        SeekerTower tower = towerAt(3, 3);
        UpgradeNode deepFreeze = UpgradePaths.named(tower, "Deep Freeze");
        int durationBeforeChoosing = tower.getFreezeDurationTicks();

        // onUpgradeBought is exercised directly - Deep Freeze's own gate (a kill-count
        // threshold) is covered generically by KillCountConditionTest; this proves the bump itself.
        tower.onUpgradeBought(deepFreeze);

        assertThat(tower.getFreezeDurationTicks()).isGreaterThan(durationBeforeChoosing);
    }
}
