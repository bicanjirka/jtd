package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
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
 * Exercises AbstractTower's damage/range math, the AuraTower buff mechanism, and the
 * upgrade-path mechanism (through the test-only {@link FakeUpgradeableTower}, since no real
 * tower has real path content yet - see td/tower/upgrade). Lives in the same package as
 * AbstractTower so it can read the protected damageBase/damageCurrent fields directly
 * instead of parsing getStatusString().
 */
class AbstractTowerTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void sellPriceIsSeventyFivePercentOfPriceRoundedHalfUp() {
        SniperTower tower = new SniperTower(context, 0, 0);

        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * SniperTower.PRICE));
    }

    @Test
    void anAuraTowerAddedToTheBoardBuffsTheTowersInRangeOfIt() {
        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);

        // Putting the aura on the board is what applies the buff - its constructor has no
        // side effects on other towers. TowerRoster recomputes every tower's stats, and each
        // one asks the towers around it what they contribute (Tower.buffFor).
        context.towers().add(new AuraTower(context, 0, 0));

        float expectedMultiplier = 1f + AuraTower.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
        assertThat(tower.damageCurrent()).isNotEqualTo(tower.damageBase);
    }

    @Test
    void anAuraTowerBuffsATowerBuiltAfterItJustTheSame() {
        context.towers().add(new AuraTower(context, 0, 0));

        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);

        float expectedMultiplier = 1f + AuraTower.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void anAuraTowerDoesNotBuffAnotherAuraTowerOrItself() {
        AuraTower first = new AuraTower(context, 0, 0);
        context.towers().add(first);
        AuraTower second = new AuraTower(context, 0, 0);
        context.towers().add(second);

        assertThat(first.buffFor(second)).isEqualTo(TowerBuff.none());
        assertThat(first.buffFor(first)).isEqualTo(TowerBuff.none());
    }

    @Test
    void twoAuraTowersStackAdditively() {
        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);

        context.towers().add(new AuraTower(context, 0, 0));
        context.towers().add(new AuraTower(context, 0, 0));

        float expectedMultiplier = 1f + 2 * AuraTower.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unequalAuraTowersStackTheirDifferentStrengths() {
        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);

        context.towers().add(new AuraTower(context, 0, 0, 0.1f));
        context.towers().add(new AuraTower(context, 0, 0, 0.3f));

        float expectedMultiplier = 1f + 0.1f + 0.3f;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void sellingTheAuraTowerRevertsTheBuffItWasGiving() {
        context.economy().startEconomy(100, 5);
        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);
        AuraTower aura = new AuraTower(context, 0, 0);
        context.towers().add(aura);

        context.towers().sell(aura);

        assertThat(tower.damageCurrent()).isEqualTo(tower.damageBase);
    }

    @Test
    void sellingOneOfTwoAuraTowersLeavesTheOthersBuffInPlace() {
        context.economy().startEconomy(100, 5);
        SniperTower tower = new SniperTower(context, 0, 0);
        context.towers().add(tower);
        AuraTower sold = new AuraTower(context, 0, 0);
        context.towers().add(sold);
        context.towers().add(new AuraTower(context, 0, 0));

        context.towers().sell(sold);

        float expectedMultiplier = 1f + AuraTower.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void towerOutsideAuraRangeIsNotBuffed() {
        SniperTower near = new SniperTower(context, 0, 0);
        context.towers().add(near);
        // AuraTower.RANGE is 1.5 cells; placing far away puts this well outside it
        SniperTower far = new SniperTower(context, 100, 100);
        context.towers().add(far);

        context.towers().add(new AuraTower(context, 0, 0));

        assertThat(near.damageCurrent()).isNotEqualTo(near.damageBase);
        assertThat(far.damageCurrent()).isEqualTo(far.damageBase);
    }

    @Test
    void dealDamageTracksDamageDealtWithoutKillingTheTarget() {
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(tower.getDamageDealt()).isEqualTo(4000);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void dealDamageCountsAKillWhenTheHitIsLethal() {
        SniperTower tower = new SniperTower(context, 0, 0);
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
        SniperTower tower = new SniperTower(context, 0, 0);
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
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void damageDealtAgainstAResistantEnemyMatchesTheHealthItActuallyLost() {
        SniperTower tower = new SniperTower(context, 0, 0);
        // an armored mob absorbs part of every hit, unlike the simple mob every other case uses
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 1000, 3, 1);
        long healthBefore = armored.getHealth();

        tower.dealDamage(armored, Damage.physical(4000));

        long healthLost = healthBefore - armored.getHealth();
        assertThat(healthLost).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthLost);
    }

    @Test
    void multipleHitsAccumulateDamageDealt() {
        SniperTower tower = new SniperTower(context, 0, 0);
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
        assertThat(tower.getChosenPath()).contains(path);
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
        assertThat(tower.getChosenPath()).contains(first);
    }

    @Test
    void choosingAPathNotInAvailablePathsIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradePath offered = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        UpgradePath foreign = new UpgradePath("Not mine", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(offered));

        boolean chosen = tower.chooseUpgradePath(foreign);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void choosingAnUnaffordablePathIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(5, 5);
        UpgradePath path = new UpgradePath("Veteran", 40, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));

        boolean chosen = tower.chooseUpgradePath(path);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(5);
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void aChosenPathComposesWithANearbyAuraTowersBuff() {
        context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, List.of(path));
        context.towers().add(tower);
        context.towers().add(new AuraTower(context, 0, 0));

        tower.chooseUpgradePath(path);

        float expectedMultiplier = 1f + 0.2f + AuraTower.DEFAULT_POWER;
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
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void dealDamageIsANoOpOnceTheTowerHasBeenCleanedUp() {
        SniperTower tower = new SniperTower(context, 0, 0);
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

    @Test
    void dealDamageRollsACriticalHitWhenTheRandomRollIsBelowCritChance() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Precision", 10, new TowerBuff(0f, 0f, 0f, 0f, 0.5f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(alwaysCrits, 0, 0, List.of(path));
        tower.chooseUpgradePath(path);
        EnemyMob enemy = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(Math.round(1000 * Damage.CRITICAL_MULTIPLIER));
    }

    @Test
    void dealDamageDoesNotRollACriticalHitWhenTheRandomRollIsAboveCritChance() {
        GameWorld neverCrits = new GameWorld(new RecordingGameHost(), () -> 0.99);
        neverCrits.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Precision", 10, new TowerBuff(0f, 0f, 0f, 0f, 0.5f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(neverCrits, 0, 0, List.of(path));
        tower.chooseUpgradePath(path);
        EnemyMob enemy = EnemyFactory.getEnemy("c", neverCrits, 0, 100000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aTowerWithNoCritChanceNeverRollsACriticalHitEvenWithAnAlwaysSucceedingRandomSource() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        SniperTower tower = new SniperTower(alwaysCrits, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, 1);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aBurningTargetDoublesTheEffectiveCritChance() {
        // roll lands strictly between the base 20% chance and its doubled 40% - only a burning
        // target's doubled chance should turn this into a critical hit
        GameWorld world = new GameWorld(new RecordingGameHost(), () -> 0.3);
        world.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Precision", 10, new TowerBuff(0f, 0f, 0f, 0f, 0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(world, 0, 0, List.of(path));
        tower.chooseUpgradePath(path);
        EnemyMob burning = EnemyFactory.getEnemy("c", world, 0, 100000, 3, 1);
        burning.applyEffect(Effect.burn(Damage.magic(1), 100, d -> {
        }));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(Math.round(1000 * Damage.CRITICAL_MULTIPLIER));
    }

    @Test
    void aNonBurningTargetDoesNotGetTheDoubledCritChance() {
        // same roll and base chance as aBurningTargetDoublesTheEffectiveCritChance, but no burn
        // active - the same roll that crit there must not crit here
        GameWorld world = new GameWorld(new RecordingGameHost(), () -> 0.3);
        world.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Precision", 10, new TowerBuff(0f, 0f, 0f, 0f, 0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(world, 0, 0, List.of(path));
        tower.chooseUpgradePath(path);
        EnemyMob notBurning = EnemyFactory.getEnemy("c", world, 0, 100000, 3, 1);

        tower.dealDamage(notBurning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void getStatusStringListsEveryAvailablePathUntilOneIsChosenThenListsNone() {
        this.context.economy().startEconomy(100, 5);
        UpgradePath path = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(this.context, 0, 0, List.of(path));

        assertThat(tower.getStatusString()).contains("Upgrade paths:").contains(path.describe());

        tower.chooseUpgradePath(path);

        assertThat(tower.getStatusString()).doesNotContain("Upgrade paths:");
    }

    @Test
    void getInfoStringAlsoListsAvailablePathsBeforeAnyoneHasBoughtOne() {
        UpgradePath path = new UpgradePath("Veteran", 10, TowerBuff.amplifying(0.2f), UpgradeCondition.always());
        FakeUpgradeableTower tower = new FakeUpgradeableTower(this.context, 0, 0, List.of(path));

        assertThat(tower.getInfoString()).contains("Upgrade paths:").contains(path.describe());
    }

    @Test
    void aPassiveTowerWithNoUpgradePathsShowsNoUpgradePathsBlock() {
        AuraTower passive = new AuraTower(this.context, 0, 0);

        assertThat(passive.getStatusString()).doesNotContain("Upgrade paths:");
        assertThat(passive.getInfoString()).doesNotContain("Upgrade paths:");
    }

    @Test
    void aBurningTargetAgainstATowerWithNoCritChanceStillNeverCrits() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        SniperTower tower = new SniperTower(alwaysCrits, 0, 0);
        EnemyMob burning = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, 1);
        burning.applyEffect(Effect.burn(Damage.magic(1), 100, d -> {
        }));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }
}
