package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers SniperTower's HEAD upgrades. Its targeting and firing are already exercised via
 * GameEngineTest/TowerPlacementTest. {@link td.tower.targeting.HighestHealthSelectorTest}
 * covers the special-slot retargeting selector's own logic.
 */
class SniperTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void focusedOpticsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode focusedOptics = UpgradePaths.named(tower, "Focused Optics");

        boolean chosen = tower.buyUpgrade(focusedOptics);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void marksmansEyeIsNotYetChoosableBeforeFifteenKills() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode marksmansEye = UpgradePaths.named(tower, "Marksman's Eye");

        boolean chosen = tower.buyUpgrade(marksmansEye);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void marksmansEyeGrantsACritChanceOnceKillCountIsMet() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 15; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        UpgradeNode marksmansEye = UpgradePaths.named(tower, "Marksman's Eye");

        boolean chosen = tower.buyUpgrade(marksmansEye);

        assertThat(chosen).isTrue();
        assertThat(tower.critChance()).isGreaterThan(0f);
    }

    @Test
    void buyingAnyHeadRootForeclosesTheOtherHeadRootForever() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));

        boolean chosenMarksmansEye = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));

        assertThat(chosenMarksmansEye).isFalse();
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
