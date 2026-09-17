package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises AbstractTower's damage/range math, the TowerAura buff mechanism, and the
 * upgrade-path mechanism (through the test-only {@link FakeUpgradeableTower}, since no real
 * tower has real path content yet - see td/tower/upgrade). Lives in the same package as
 * AbstractTower so it can read the protected damageBase/damageCurrent fields directly
 * instead of parsing getStatusString().
 */
class AbstractTowerTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void sellPriceIsSeventyFivePercentOfPriceRoundedHalfUp() {
        TowerOne tower = new TowerOne(context, 0, 0);

        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * TowerOne.PRICE));
    }

    @Test
    void registerTowerAppliesAuraBuffToDamage() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.towers().add(tower);

        // constructing a TowerAura in range scans context.towers and
        // registers itself with anything nearby, buffing it immediately
        new TowerAura(context, 0, 0);

        float expectedMultiplier = 1f + TowerAura.DEFAULT_POWER; // one aura tower registered
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
        assertThat(tower.damageCurrent()).isNotEqualTo(tower.damageBase);
    }

    @Test
    void twoAuraTowersStackAdditively() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.towers().add(tower);

        new TowerAura(context, 0, 0);
        new TowerAura(context, 0, 0);

        float expectedMultiplier = 1f + 2 * TowerAura.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unequalAuraTowersStackTheirDifferentStrengths() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.towers().add(tower);

        new TowerAura(context, 0, 0, 0.1f);
        new TowerAura(context, 0, 0, 0.3f);

        float expectedMultiplier = 1f + 0.1f + 0.3f;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unregisterTowerRevertsTheBuff() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.towers().add(tower);
        TowerAura aura = new TowerAura(context, 0, 0);

        tower.unregisterTower(aura);

        assertThat(tower.damageCurrent()).isEqualTo(tower.damageBase);
    }

    @Test
    void towerOutsideAuraRangeIsNotBuffed() {
        TowerOne near = new TowerOne(context, 0, 0);
        context.towers().add(near);
        // TowerAura.RANGE is 1.5 cells; placing far away puts this well outside it
        TowerOne far = new TowerOne(context, 100, 100);
        context.towers().add(far);

        new TowerAura(context, 0, 0);

        assertThat(near.damageCurrent()).isNotEqualTo(near.damageBase);
        assertThat(far.damageCurrent()).isEqualTo(far.damageBase);
    }

    @Test
    void dealDamageTracksDamageDealtWithoutKillingTheTarget() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(tower.getDamageDealt()).isEqualTo(4000);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void dealDamageCountsAKillWhenTheHitIsLethal() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        int healthBefore = enemy.getHealth();

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.isDead()).isTrue();
        assertThat(tower.getKillCount()).isEqualTo(1);
        // the hit was far bigger than what was left, and only what was left counts
        assertThat(healthBefore).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthBefore);
    }

    @Test
    void dealDamageIntoAnAlreadyDeadEnemyIsNotCountedAgain() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        int healthBefore = enemy.getHealth();
        tower.dealDamage(enemy, Damage.physical(4000));

        // simulates a second tower's shot landing on the same tick, after this one already killed it
        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(tower.getKillCount()).isEqualTo(1);
        assertThat(tower.getDamageDealt()).isEqualTo(healthBefore);
    }

    @Test
    void aKilledEnemyIsLeftAtZeroHealthRatherThanNegative() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void damageDealtAgainstAResistantEnemyMatchesTheHealthItActuallyLost() {
        TowerOne tower = new TowerOne(context, 0, 0);
        // a square absorbs part of every hit, unlike the circle every other case here uses
        EnemyMob square = EnemyFactory.getEnemy("s", context, 0, 1000, 3, 1);
        long healthBefore = square.getHealth();

        tower.dealDamage(square, Damage.physical(4000));

        long healthLost = healthBefore - square.getHealth();
        assertThat(healthLost).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthLost);
    }

    @Test
    void multipleHitsAccumulateDamageDealt() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(1000));
        tower.dealDamage(enemy, Damage.physical(500));

        assertThat(tower.getDamageDealt()).isEqualTo(1500);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void choosingAnOfferedPathSpendsItsPriceAndAppliesItsStatBonus() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 40, new TowerBuff(0.5f, 0.25f, 0f, 0f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));

        boolean chosen = tower.chooseUpgradePath(path);

        assertThat(chosen).isTrue();
        assertThat(context.economy().getCredits()).isEqualTo(60);
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * 1.5f));
        assertThat(tower.getChosenPath()).isEqualTo(path);
    }

    @Test
    void choosingAPathTwiceIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradePath first = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        UpgradePath second = new UpgradePath("Overclock", 10, TowerBuff.amplifying(0.1f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(first, second));
        tower.chooseUpgradePath(first);

        boolean chosenAgain = tower.chooseUpgradePath(second);

        assertThat(chosenAgain).isFalse();
        assertThat(tower.getChosenPath()).isEqualTo(first);
    }

    @Test
    void choosingAPathNotInAvailablePathsIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradePath offered = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        UpgradePath foreign = new UpgradePath("Not mine", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(offered));

        boolean chosen = tower.chooseUpgradePath(foreign);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isNull();
    }

    @Test
    void choosingAnUnaffordablePathIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(5, 5);
        UpgradePath path = new UpgradePath("Veteran", 40, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));

        boolean chosen = tower.chooseUpgradePath(path);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(5);
        assertThat(tower.getChosenPath()).isNull();
    }

    @Test
    void aChosenPathComposesWithANearbyAuraTowersBuff() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));
        context.towers().add(tower);
        new TowerAura(context, 0, 0);

        tower.chooseUpgradePath(path);

        float expectedMultiplier = 1f + 0.2f + TowerAura.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void aChosenPathsFireRateBonusReducesCoolDownCurrent() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Overclock", 10, new TowerBuff(0f, 0f, 0.5f, 0f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));

        tower.chooseUpgradePath(path);

        assertThat(tower.coolDownCurrent()).isEqualTo(Math.round(tower.coolDownMax * 0.5f));
    }

    @Test
    void choosingAPathWhoseConditionIsntSatisfiedIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), new KillCountCondition(1000));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));

        boolean chosen = tower.chooseUpgradePath(path);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(100);
        assertThat(tower.getChosenPath()).isNull();
    }

    @Test
    void dealDamageIsANoOpOnceTheTowerHasBeenCleanedUp() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);

        tower.doCleanup(); // what TowerRoster.sell()/clear() call before dropping the tower
        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.isDead()).isFalse();
        assertThat(tower.getDamageDealt()).isZero();
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void aBountyBonusPathToppedUpCreditsWithoutDoublingScoreOnAKill() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 10, new TowerBuff(0f, 0f, 0f, 0.5f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));
        tower.chooseUpgradePath(path);
        int creditsAfterBuying = context.economy().getCredits();
        int scoreBefore = context.economy().getScore();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1, 20, 1);

        tower.dealDamage(enemy, Damage.physical(4000));

        // base kill bounty (20 credits, 20 score) plus 50% bonus credits (10) - no extra score
        assertThat(context.economy().getCredits()).isEqualTo(creditsAfterBuying + 20 + 10);
        assertThat(context.economy().getScore()).isEqualTo(scoreBefore + 20);
    }
}
