package td.tower;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises AbstractTower's damage/range math and the TowerUpgrade buff
 * mechanism through a concrete subclass (TowerOne). Lives in the same
 * package as AbstractTower so it can read the protected damageBase/
 * damageCurrent fields directly instead of parsing getStatusString().
 */
class AbstractTowerTest {

    private final Context context = new Context(new RecordingGameHost());

    @Test
    void sellPriceIsSeventyFivePercentOfPriceRoundedHalfUp() {
        TowerOne tower = new TowerOne(context, 0, 0);

        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * TowerOne.price));
    }

    @Test
    void registerTowerAppliesUpgradeBuffToDamage() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        // constructing a TowerUpgrade in range scans context.towers and
        // registers itself with anything nearby, buffing it immediately
        new TowerUpgrade(context, 0, 0);

        float expectedMultiplier = 1f + TowerUpgrade.DEFAULT_POWER; // one upgrade tower registered
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
        assertThat(tower.damageCurrent).isNotEqualTo(tower.damageBase);
    }

    @Test
    void twoUpgradeTowersStackAdditively() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        new TowerUpgrade(context, 0, 0);
        new TowerUpgrade(context, 0, 0);

        float expectedMultiplier = 1f + 2 * TowerUpgrade.DEFAULT_POWER;
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unequalUpgradeTowersStackTheirDifferentStrengths() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        new TowerUpgrade(context, 0, 0, 0.1f);
        new TowerUpgrade(context, 0, 0, 0.3f);

        float expectedMultiplier = 1f + 0.1f + 0.3f;
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unregisterTowerRevertsTheBuff() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);
        TowerUpgrade upgrade = new TowerUpgrade(context, 0, 0);

        tower.unregisterTower(upgrade);

        assertThat(tower.damageCurrent).isEqualTo(tower.damageBase);
    }

    @Test
    void towerOutsideUpgradeRangeIsNotBuffed() {
        TowerOne near = new TowerOne(context, 0, 0);
        context.addTower(near);
        // TowerUpgrade.range is 1.5 cells; placing far away puts this well outside it
        TowerOne far = new TowerOne(context, 100, 100);
        context.addTower(far);

        new TowerUpgrade(context, 0, 0);

        assertThat(near.damageCurrent).isNotEqualTo(near.damageBase);
        assertThat(far.damageCurrent).isEqualTo(far.damageBase);
    }
}
