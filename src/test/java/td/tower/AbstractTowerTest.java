package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises AbstractTower's damage/range math, the AuraTower buff mechanism, and the
 * upgrade-tree mechanism (through the test-only {@link FakeUpgradeableTower}, since no real
 * tower's own content is exercised here - see td/tower/upgrade).
 * Lives in the same package as AbstractTower so it can read the protected
 * damageBase/damageCurrent fields directly instead of parsing getStatusString().
 */
class AbstractTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

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
    void buffedTowersListsExactlyTheInRangeNonAuraTowersAndNoOthers() {
        AuraTower aura = new AuraTower(context, 0, 0);
        context.towers().add(aura);
        SniperTower near = new SniperTower(context, 0, 0);
        context.towers().add(near);
        SniperTower far = new SniperTower(context, 100, 100);
        context.towers().add(far);
        AuraTower otherAura = new AuraTower(context, 0, 0);
        context.towers().add(otherAura);

        assertThat(aura.buffedTowers()).containsExactly(near);
    }

    @Test
    void dealDamageTracksDamageDealtWithoutKillingTheTarget() {
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(tower.getDamageDealt()).isEqualTo(4000);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void dealDamageCountsAKillWhenTheHitIsLethal() {
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
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
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
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
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void damageDealtAgainstAResistantEnemyMatchesTheHealthItActuallyLost() {
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.none());
        // an armored mob absorbs part of every hit, unlike the simple mob every other case uses
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 1000, 3, Rank.GRUNT);
        long healthBefore = armored.getHealth();

        tower.dealDamage(armored, Damage.physical(4000));

        long healthLost = healthBefore - armored.getHealth();
        assertThat(healthLost).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthLost);
    }

    @Test
    void multipleHitsAccumulateDamageDealt() {
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(1000));
        tower.dealDamage(enemy, Damage.physical(500));

        assertThat(tower.getDamageDealt()).isEqualTo(1500);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void choosingAnOfferedNodeSpendsItsPriceAndAppliesItsStatBonus() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 40)
                .withBuff(new TowerBuff(0.5f, 0.25f, 0f, 0f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isTrue();
        assertThat(context.economy().getCredits()).isEqualTo(60);
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * 1.5f));
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).contains(node);
    }

    @Test
    void buyingANodeTwiceIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradeNode first = UpgradeNode.of("first", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f))
                .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD));
        UpgradeNode second = UpgradeNode.of("second", UpgradeSlot.HEAD, "Overclock", 10)
                .withBuff(TowerBuff.amplifying(0.1f))
                .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(first, second));
        tower.buyUpgrade(first);

        boolean chosenAgain = tower.buyUpgrade(second);

        assertThat(chosenAgain).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).contains(first);
    }

    @Test
    void buyingANodeNotInThisTowersOwnTreeIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradeNode offered = UpgradeNode.of("offered", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f));
        UpgradeNode foreign = UpgradeNode.of("foreign", UpgradeSlot.HEAD, "Not mine", 10)
                .withBuff(TowerBuff.amplifying(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(offered));

        boolean chosen = tower.buyUpgrade(foreign);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void buyingAnUnaffordableNodeIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(5, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 40)
                .withBuff(TowerBuff.amplifying(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(5);
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void aBoughtNodeComposesWithANearbyAuraTowersBuff() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));
        context.towers().add(tower);
        context.towers().add(new AuraTower(context, 0, 0));

        tower.buyUpgrade(node);

        float expectedMultiplier = 1f + 0.2f + AuraTower.DEFAULT_POWER;
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void aBoughtNodesFireRateBonusReducesCoolDownCurrent() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("overclock", UpgradeSlot.HEAD, "Overclock", 10)
                .withBuff(new TowerBuff(0f, 0f, 0.5f, 0f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));

        tower.buyUpgrade(node);

        assertThat(tower.coolDownCurrent()).isEqualTo(Math.round(tower.coolDownMax * 0.5f));
    }

    @Test
    void buyingANodeWhoseGateIsntSatisfiedIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f))
                .withGate(new KillCountCondition(1000));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(100);
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void dealDamageIsANoOpOnceTheTowerHasBeenCleanedUp() {
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);

        tower.doCleanup(); // what TowerRoster.sell()/clear() call before dropping the tower
        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.isDead()).isFalse();
        assertThat(tower.getDamageDealt()).isZero();
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void aBountyBonusNodeToppedUpCreditsWithoutDoublingScoreOnAKill() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(new TowerBuff(0f, 0f, 0f, 0.5f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(context, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        int creditsAfterBuying = context.economy().getCredits();
        int scoreBefore = context.economy().getScore();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1, 20, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(4000));

        // base kill bounty (20 credits, 20 score) plus 50% bonus credits (10) - no extra score
        assertThat(context.economy().getCredits()).isEqualTo(creditsAfterBuying + 20 + 10);
        assertThat(context.economy().getScore()).isEqualTo(scoreBefore + 20);
    }

    @Test
    void dealDamageRollsACriticalHitWhenTheRandomRollIsBelowCritChance() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", UpgradeSlot.HEAD, "Precision", 10)
                .withBuff(TowerBuff.critChance(0.5f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(alwaysCrits, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(Math.round(1000 * Damage.CRITICAL_MULTIPLIER));
    }

    @Test
    void dealDamageDoesNotRollACriticalHitWhenTheRandomRollIsAboveCritChance() {
        GameWorld neverCrits = WorldFixtures.newWorld(() -> 0.99);
        neverCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", UpgradeSlot.HEAD, "Precision", 10)
                .withBuff(TowerBuff.critChance(0.5f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(neverCrits, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFactory.getEnemy("c", neverCrits, 0, 100000, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aTowerWithNoCritChanceNeverRollsACriticalHitEvenWithAnAlwaysSucceedingRandomSource() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        FakeUpgradeableTower tower = new FakeUpgradeableTower(alwaysCrits, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, Rank.GRUNT);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aBurningTargetDoublesTheEffectiveCritChance() {
        // roll lands strictly between the base 20% chance and its doubled 40% - only a burning
        // target's doubled chance should turn this into a critical hit
        GameWorld world = WorldFixtures.newWorld(() -> 0.3);
        world.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", UpgradeSlot.HEAD, "Precision", 10)
                .withBuff(TowerBuff.critChance(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(world, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob burning = EnemyFactory.getEnemy("c", world, 0, 100000, 3, Rank.GRUNT);
        burning.applyEffect(Effect.burn(Damage.magic(1), 100, d -> {
        }));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(Math.round(1000 * Damage.CRITICAL_MULTIPLIER));
    }

    @Test
    void aNonBurningTargetDoesNotGetTheDoubledCritChance() {
        // same roll and base chance as aBurningTargetDoublesTheEffectiveCritChance, but no burn
        // active - the same roll that crit there must not crit here
        GameWorld world = WorldFixtures.newWorld(() -> 0.3);
        world.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", UpgradeSlot.HEAD, "Precision", 10)
                .withBuff(TowerBuff.critChance(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(world, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob notBurning = EnemyFactory.getEnemy("c", world, 0, 100000, 3, Rank.GRUNT);

        tower.dealDamage(notBurning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void getStatusStringListsEveryOfferedNodeWithItsGateProgressUntilOneIsBoughtThenListsNone() {
        this.context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f))
                .withGate(new KillCountCondition(10));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(this.context, 0, 0, UpgradeTree.of(node));

        assertThat(tower.getStatusString()).contains("Upgrades:").contains("Veteran").contains("0/10 kills");

        tower.dealDamage(EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT), Damage.physical(1_000_000));
        assertThat(tower.getStatusString()).contains("✘");

        for (int i = 0; i < 9; i++) {
            tower.dealDamage(EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT), Damage.physical(1_000_000));
        }
        assertThat(tower.getStatusString()).contains("✔");

        tower.buyUpgrade(node);

        assertThat(tower.getStatusString()).doesNotContain("Upgrades:");
    }

    @Test
    void getInfoStringNeverListsUpgradeContentEvenBeforeAnyoneHasBoughtOne() {
        UpgradeNode node = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
                .withBuff(TowerBuff.amplifying(0.2f));
        FakeUpgradeableTower tower = new FakeUpgradeableTower(this.context, 0, 0, UpgradeTree.of(node));

        assertThat(tower.getInfoString()).doesNotContain("Upgrades:").doesNotContain("Veteran");
    }

    @Test
    void aTowerWithNoUpgradeTreeShowsNoUpgradePathsBlock() {
        FakeUpgradeableTower noTree = new FakeUpgradeableTower(this.context, 0, 0, UpgradeTree.none());

        assertThat(noTree.getStatusString()).doesNotContain("Upgrades:");
        assertThat(noTree.getInfoString()).doesNotContain("Upgrades:");
    }

    @Test
    void dealDamageReturnsWhetherTheHitWasCriticalMatchingTheDamageItLanded() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", UpgradeSlot.HEAD, "Precision", 10)
                .withBuff(TowerBuff.critChance(0.5f));
        FakeUpgradeableTower crittingTower = new FakeUpgradeableTower(alwaysCrits, 0, 0, UpgradeTree.of(node));
        crittingTower.buyUpgrade(node);
        EnemyMob crittingTarget = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, Rank.GRUNT);

        boolean wasCritical = crittingTower.dealDamage(crittingTarget, Damage.physical(1000));

        assertThat(wasCritical).isTrue();

        GameWorld neverCrits = WorldFixtures.newWorld(() -> 0.99);
        neverCrits.economy().startEconomy(100, 5);
        FakeUpgradeableTower nonCrittingTower = new FakeUpgradeableTower(neverCrits, 0, 0, UpgradeTree.of(node));
        nonCrittingTower.buyUpgrade(node);
        EnemyMob nonCrittingTarget = EnemyFactory.getEnemy("c", neverCrits, 0, 100000, 3, Rank.GRUNT);

        boolean wasNotCritical = nonCrittingTower.dealDamage(nonCrittingTarget, Damage.physical(1000));

        assertThat(wasNotCritical).isFalse();
    }

    @Test
    void aBurningTargetAgainstATowerWithNoCritChanceStillNeverCrits() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        FakeUpgradeableTower tower = new FakeUpgradeableTower(alwaysCrits, 0, 0, UpgradeTree.none());
        EnemyMob burning = EnemyFactory.getEnemy("c", alwaysCrits, 0, 100000, 3, Rank.GRUNT);
        burning.applyEffect(Effect.burn(Damage.magic(1), 100, d -> {
        }));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }
}
