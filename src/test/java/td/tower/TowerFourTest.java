package td.tower;

import org.junit.jupiter.api.Test;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers TowerFour's two upgrade paths. */
class TowerFourTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void expandedFieldIsChoosableWithMoneyAloneAndAppliesItsRangeBonus() {
        this.context.startEconomy(1000, 5);
        TowerFour tower = new TowerFour(this.context, 0, 0);
        UpgradePath expandedField = UpgradePaths.named(tower, "Expanded Field");

        boolean chosen = tower.chooseUpgradePath(expandedField);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(TowerFour.range * this.context.getBoard().scale());
    }

    @Test
    void overloadCoreIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.startEconomy(1000, 5);
        TowerFour tower = new TowerFour(this.context, 0, 0);
        UpgradePath overloadCore = UpgradePaths.named(tower, "Overload Core");

        boolean chosen = tower.chooseUpgradePath(overloadCore);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isNull();
    }
}
