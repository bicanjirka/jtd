package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers SniperTower's two upgrade paths. Its targeting and firing are already exercised via
 * GameEngineTest/TowerPlacementTest.
 */
class SniperTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void overclockIsChoosableWithMoneyAloneAndAppliesItsFireRateAndDamagePenalty() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradePath overclock = UpgradePaths.named(tower, "Overclock");

        boolean chosen = tower.chooseUpgradePath(overclock);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isLessThan(tower.damageBase);
        assertThat(tower.coolDownCurrent()).isLessThan(tower.coolDownMax);
    }

    @Test
    void veteranIsNotYetChoosableBeforeTenKills() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradePath veteran = UpgradePaths.named(tower, "Veteran");

        boolean chosen = tower.chooseUpgradePath(veteran);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void veteranGrantsACritChanceOnceKillCountIsMet() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 10; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        UpgradePath veteran = UpgradePaths.named(tower, "Veteran");

        boolean chosen = tower.chooseUpgradePath(veteran);

        assertThat(chosen).isTrue();
        assertThat(tower.critChance()).isGreaterThan(0f);
    }

    @Test
    void doTickRecordsWhetherTheShotThatJustFiredWasACriticalHit() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        alwaysCrits.setBoard(BoardGeometry.of(BoardFixtures.SCALE, 20, 20));
        SniperTower tower = new SniperTower(alwaysCrits, 3, 3);
        RecordingEnemyMob target = RecordingEnemyMob.normalAt(100, 100);
        alwaysCrits.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(tower.wasLastShotCritical()).isTrue();
    }
}
