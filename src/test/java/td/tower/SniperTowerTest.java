package td.tower;

import org.junit.jupiter.api.Test;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers SniperTower's two upgrade paths. Its targeting and firing are already exercised via
 * GameEngineTest/TowerPlacementTest.
 */
class SniperTowerTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

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
}
