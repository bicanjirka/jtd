package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers PulseTower's two upgrade paths.
 */
class PulseTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void expandedFieldIsChoosableWithMoneyAloneAndAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradeNode expandedField = UpgradePaths.named(tower, "Expanded Field");

        boolean chosen = tower.buyUpgrade(expandedField);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(PulseTower.RANGE * this.context.getBoard().scale());
    }

    @Test
    void overloadCoreIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.economy().startEconomy(1000, 5);
        PulseTower tower = new PulseTower(this.context, 0, 0);
        UpgradeNode overloadCore = UpgradePaths.named(tower, "Overload Core");

        boolean chosen = tower.buyUpgrade(overloadCore);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }
}
