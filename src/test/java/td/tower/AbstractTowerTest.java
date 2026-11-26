package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.economy.SellRefund;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.fixtures.EnemyFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.FakeTower;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.StatTerm;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcPayload;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** In {@code AbstractTower}'s package so it can read protected stats directly. */
class AbstractTowerTest {

    /** A tree whose tier I opens for free at 150 XP. */
    private static final List<UpgradeTier> FREE_AT_150 = List.of(UpgradeTier.open(), UpgradeTier.gated(150, 0));

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void sellPriceIsSeventyFivePercentOfPriceRoundedHalfUp() {
        SniperTower tower = new SniperTower(context, 0, 0);
        WorldFixtures.passAWave(this.context);

        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * SniperTower.PRICE));
    }

    @Test
    void sellPriceRefundsSeventyFivePercentOfEveryUpgradeBoughtToo() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradeNode first = tower.offeredUpgrades(this.context).get(0);
        UpgradeNode second = tower.offeredUpgrades(this.context).get(1);

        tower.buyUpgrade(first);
        tower.buyUpgrade(second);
        WorldFixtures.passAWave(this.context);

        int paid = SniperTower.PRICE + first.price() + second.price();
        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * paid));
    }

    @Test
    void aTowerNoWaveHasRunWithSellsBackEverythingPaidForItAndItsUpgrades() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradeNode first = tower.offeredUpgrades(this.context).get(0);

        tower.buyUpgrade(first);

        assertThat(tower.use().allUnused()).isTrue();
        assertThat(tower.getSellPrice()).isEqualTo(SniperTower.PRICE + first.price());
    }

    @Test
    void aNodeBoughtSinceTheLastWaveSellsBackInFullBesideTheShareOfWhatWasUsed() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        WorldFixtures.passAWave(this.context);
        UpgradeNode fresh = tower.offeredUpgrades(this.context).get(0);

        tower.buyUpgrade(fresh);

        assertThat(tower.use().isUnused(fresh)).isTrue();
        assertThat(tower.use().allUnused()).isFalse();
        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * SniperTower.PRICE) + fresh.price());
    }

    @Test
    void downgradingANodeBoughtSinceTheLastWaveGivesBackAllItCost() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        WorldFixtures.passAWave(this.context);
        UpgradeNode fresh = tower.offeredUpgrades(this.context).get(0);
        tower.buyUpgrade(fresh);
        int before = this.context.economy().getCredits();

        tower.downgrade(fresh);

        assertThat(this.context.economy().getCredits() - before).isEqualTo(fresh.price());
        assertThat(tower.use().anyUnused()).isFalse();
    }

    @Test
    void anEmissionReachesATowerBuiltAfterItsEmitter() {
        this.context.towers().add(FakeTower.offering(this.context, 0, 0, UpgradeTree.none())
                .giving(TowerModifiers.damagePercent(0.2f)));

        SniperTower tower = new SniperTower(this.context, 0, 0);
        this.context.towers().add(tower);

        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * 1.2f));
    }

    @Test
    void aFlatRangeNodeIsAmplifiedByAReceivedRangeShare() {
        GameWorld elsewhere = WorldFixtures.newWorld();
        SeekerTower buffed = new SeekerTower(this.context, 0, 0);
        SeekerTower alone = new SeekerTower(elsewhere, 0, 0);
        this.context.towers().add(buffed);
        elsewhere.towers().add(alone);
        TowerFixtures.buffed(buffed, this.context, TowerModifiers.rangePercent(0.2f));

        UpgradePaths.buy(buffed, this.context, "High Perch");
        UpgradePaths.buy(alone, elsewhere, "High Perch");

        assertThat(buffed.stats().range()).isCloseTo(alone.stats().range() * 1.2f, within(1e-5f));
    }

    @Test
    void twoEmittersSharesAddUp() {
        SniperTower tower = new SniperTower(this.context, 0, 0);
        this.context.towers().add(tower);

        TowerFixtures.buffed(tower, this.context, TowerModifiers.damagePercent(0.1f));
        TowerFixtures.buffed(tower, this.context, TowerModifiers.damagePercent(0.3f));

        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * (1f + 0.1f + 0.3f)));
    }

    @Test
    void sellingAnEmitterRevertsWhatItWasGiving() {
        this.context.economy().startEconomy(100, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        this.context.towers().add(tower);
        FakeTower emitter = FakeTower.offering(this.context, 0, 0, UpgradeTree.none())
                .giving(TowerModifiers.damagePercent(0.2f));
        this.context.towers().add(emitter);

        this.context.towers().sell(emitter);

        assertThat(tower.damageCurrent()).isEqualTo(tower.damageBase);
    }

    @Test
    void dealDamageTracksDamageDealtWithoutKillingTheTarget() {
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1000, 3);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(tower.getDamageDealt()).isEqualTo(4000);
        assertThat(tower.getKillCount()).isZero();
    }

    private static List<TowerStatLine> waveDpsRows(Tower tower) {
        return tower.inspect().stats().stream().filter(line -> line.stat() == TowerStatRow.WAVE_DPS).toList();
    }

    @Test
    void hitsAndDamageOverTimeInAWaveShowAsWhatItDealtASecondUntilTheNextWaveStarts() {
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1000, 3);
        int second = Math.round(TickRate.TICKS_PER_SECOND);
        context.waves().announce(0);

        tower.beginTick(1);
        tower.dealDamage(enemy, Damage.physical(4000));
        tower.beginTick(1 + second);
        tower.dealDamage(enemy, Damage.physical(4000));
        tower.beginTick(1 + 2 * second);
        tower.dealPeriodicDamage(enemy, Damage.physical(4000));

        assertThat(waveDpsRows(tower)).singleElement().satisfies(row -> assertThat(row.current())
                .isCloseTo(DamageUnits.inPoints(8000) / 2f, within(0.01f)));

        context.waves().announce(0);

        assertThat(waveDpsRows(tower)).isEmpty();
    }

    @Test
    void theRosterHearsWhatALethalHitTookAndWhatWentPastTheHealthLeft() {
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
        int healthBefore = enemy.getHealth();
        List<HitResult> heard = new ArrayList<>();
        context.towers().addHitListener((who, whom, result) -> heard.add(result));

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(heard).singleElement().satisfies(result -> {
            assertThat(result.credited()).isEqualTo(healthBefore);
            assertThat(result.overkill()).isEqualTo(4000 - healthBefore);
        });
    }

    @Test
    void dealDamageCountsAKillWhenTheHitIsLethal() {
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
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
        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
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
        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);

        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void damageDealtAgainstAResistantEnemyMatchesTheHealthItActuallyLost() {
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.none());
        EnemyMob armored = EnemyFixtures.liveMob(context,
                EnemyFixtures.simpleDefinition("armored").withHealthAndPrice(1000, 3).withArmor(50f));
        long healthBefore = armored.getHealth();

        tower.dealDamage(armored, Damage.physical(4000));

        long healthLost = healthBefore - armored.getHealth();
        assertThat(healthLost).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthLost);
    }

    @Test
    void multipleHitsAccumulateDamageDealt() {
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1000, 3);

        tower.dealDamage(enemy, Damage.physical(1000));
        tower.dealDamage(enemy, Damage.physical(500));

        assertThat(tower.getDamageDealt()).isEqualTo(1500);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void choosingAnOfferedNodeSpendsItsPriceAndAppliesItsStatBonus() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 40)
                .withModifiers(TowerModifiers.damagePercent(0.5f).plus(TowerModifiers.rangePercent(0.25f)));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isTrue();
        assertThat(context.economy().getCredits()).isEqualTo(60);
        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * 1.5f));
        assertThat(tower.upgrades().owned()).containsExactly(node);
    }

    @Test
    void buyingANodeTwiceIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradeNode first = UpgradeNode.of("first", "Veteran", 10)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(first));
        tower.buyUpgrade(first);

        boolean chosenAgain = tower.buyUpgrade(first);

        assertThat(chosenAgain).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(90);
        assertThat(tower.upgrades().owned()).containsExactly(first);
    }

    @Test
    void buyingAClustersOtherMemberIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(100, 5);
        UpgradeNode chosen = UpgradeNode.of("chosen", "Chosen", 10);
        UpgradeNode rival = UpgradeNode.of("rival", "Rival", 10);
        FakeTower tower = FakeTower.offering(context, 0, 0,
                UpgradeTree.of(chosen, rival).withCluster(UpgradeCluster.of("Pick", chosen, rival)));
        tower.buyUpgrade(chosen);

        boolean boughtRival = tower.buyUpgrade(rival);

        assertThat(boughtRival).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(90);
    }

    @Test
    void buyingANodeNotInThisTowersOwnTreeIsRejected() {
        context.economy().startEconomy(100, 5);
        UpgradeNode offered = UpgradeNode.of("offered", "Veteran", 10)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        UpgradeNode foreign = UpgradeNode.of("foreign", "Not mine", 10)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(offered));

        boolean chosen = tower.buyUpgrade(foreign);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().owned()).isEmpty();
    }

    @Test
    void buyingAnUnaffordableNodeIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(5, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 40)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(5);
        assertThat(tower.upgrades().owned()).isEmpty();
    }

    @Test
    void aBoughtNodeComposesWithAReceivedBuff() {
        this.context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(node));
        this.context.towers().add(tower);
        TowerFixtures.buffed(tower, this.context, TowerModifiers.damagePercent(0.3f));

        tower.buyUpgrade(node);

        assertThat(tower.damageCurrent()).isEqualTo((int) (tower.damageBase * (1f + 0.2f + 0.3f)));
    }

    @Test
    void aBoughtNodesAttackSpeedRaisesTheRate() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("overclock", "Overclock", 10)
                .withModifiers(TowerModifiers.attackSpeed(0.5f));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node));

        tower.buyUpgrade(node);

        assertThat(tower.attackRate()).isEqualTo(1.5);
    }

    @Test
    void buyingANodeBeforeTheTowerHasItsTiersXpIsRejectedAndSpendsNothing() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1)
                .withModifiers(TowerModifiers.damagePercent(0.2f).plus(TowerModifiers.rangePercent(0.2f)));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node).withTiers(FREE_AT_150));

        boolean chosen = tower.buyUpgrade(node);

        assertThat(chosen).isFalse();
        assertThat(context.economy().getCredits()).isEqualTo(100);
        assertThat(tower.upgrades().owned()).isEmpty();
    }

    @Test
    void aNodeBecomesBuyableTheMomentTheTowerEarnsItsTiersXp() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node).withTiers(FREE_AT_150));
        tower.earnXp(149);
        boolean atOneShort = tower.buyUpgrade(node);
        tower.earnXp(1);

        boolean atThreshold = tower.buyUpgrade(node);

        assertThat(atOneShort).isFalse();
        assertThat(atThreshold).isTrue();
    }

    @Test
    void aPaidTiersNodeIsBoughtOnlyAfterItsUnlockAndAPathPaysForBoth() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(), UpgradeTier.gated(0, 25)));
        FakeTower tower = FakeTower.offering(context, 0, 0, tree);
        boolean beforeUnlock = tower.buyUpgrade(node);

        boolean bought = tower.buyPath(node);

        assertThat(beforeUnlock).isFalse();
        assertThat(bought).isTrue();
        assertThat(tower.upgrades().owned()).containsExactly(tree.unlockOf(1).orElseThrow(), node);
        assertThat(context.economy().getCredits()).isEqualTo(100 - 25 - 10);
    }

    @Test
    void switchingAClusterPickRefundsWhatCameAfterTheOldMemberButNotTheMember() {
        context.economy().startEconomy(100, 5);
        UpgradeNode old = UpgradeNode.of("old", "Old", 20);
        UpgradeNode next = UpgradeNode.of("next", "Next", 20);
        UpgradeNode follow = UpgradeNode.of("follow", "Follow", 40).after(old);
        FakeTower tower = FakeTower.offering(context, 0, 0,
                UpgradeTree.of(old, next, follow).withCluster(UpgradeCluster.of("Pick", old, next)));
        tower.buyUpgrade(old);
        tower.buyUpgrade(follow);
        WorldFixtures.passAWave(context);

        boolean switched = tower.switchTo(next);

        assertThat(switched).isTrue();
        assertThat(tower.upgrades().owned()).containsExactly(next);
        assertThat(context.economy().getCredits()).isEqualTo(100 - 20 - 40 + Math.round(40 * SellRefund.SHARE));
    }

    @Test
    void aSwitchAndADowngradeWaitWhileAWaveIsOut() {
        context.economy().startEconomy(100, 5);
        UpgradeNode old = UpgradeNode.of("old", "Old", 20);
        UpgradeNode next = UpgradeNode.of("next", "Next", 20);
        FakeTower tower = FakeTower.offering(context, 0, 0,
                UpgradeTree.of(old, next).withCluster(UpgradeCluster.of("Pick", old, next)));
        tower.buyUpgrade(old);
        context.waves().announce(0);

        boolean switched = tower.switchTo(next);
        boolean downgraded = tower.downgrade(old);

        assertThat(switched).isFalse();
        assertThat(downgraded).isFalse();
        assertThat(tower.upgrades().owned()).containsExactly(old);
        assertThat(context.economy().getCredits()).isEqualTo(80);
    }

    @Test
    void dealDamageIsANoOpOnceTheTowerHasBeenCleanedUp() {
        SniperTower tower = new SniperTower(context, 0, 0);
        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);

        tower.doCleanup();
        tower.dealDamage(enemy, Damage.physical(4000));

        assertThat(enemy.isDead()).isFalse();
        assertThat(tower.getDamageDealt()).isZero();
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void aBountyBonusNodeToppedUpCreditsWithoutDoublingScoreOnAKill() {
        context.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10)
                .withModifiers(TowerModifiers.bounty(0.5f));
        FakeTower tower = FakeTower.offering(context, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        int creditsAfterBuying = context.economy().getCredits();
        int scoreBefore = context.economy().getScore();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 1, 20);

        tower.dealDamage(enemy, Damage.physical(4000));

        // base kill bounty (20 credits, 20 score) plus 50% bonus credits (10) - no extra score
        assertThat(context.economy().getCredits()).isEqualTo(creditsAfterBuying + 20 + 10);
        assertThat(context.economy().getScore()).isEqualTo(scoreBefore + 20);
    }

    @Test
    void dealDamageRollsACriticalHitWhenTheRandomRollIsBelowCritChance() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", "Precision", 10)
                .withModifiers(TowerModifiers.critChance(0.5f));
        FakeTower tower = FakeTower.offering(alwaysCrits, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFixtures.plainMob(alwaysCrits, 100000, 3);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(Math.round(1000 * AttackProfile.DEFAULT_CRIT_MULTIPLIER));
    }

    @Test
    void dealDamageDoesNotRollACriticalHitWhenTheRandomRollIsAboveCritChance() {
        GameWorld neverCrits = WorldFixtures.newWorld(() -> 0.99);
        neverCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", "Precision", 10)
                .withModifiers(TowerModifiers.critChance(0.5f));
        FakeTower tower = FakeTower.offering(neverCrits, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFixtures.plainMob(neverCrits, 100000, 3);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aTowerWithNoCritChanceNeverRollsACriticalHitEvenWithAnAlwaysSucceedingRandomSource() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        FakeTower tower = FakeTower.offering(alwaysCrits, 0, 0, UpgradeTree.none());
        EnemyMob enemy = EnemyFixtures.plainMob(alwaysCrits, 100000, 3);

        tower.dealDamage(enemy, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void burningAloneDoesNotDoubleTheCritChance() {
        // roll lands strictly between the tower's 20% chance and a doubled 40%
        GameWorld world = WorldFixtures.newWorld(() -> 0.3);
        world.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", "Precision", 10)
                .withModifiers(TowerModifiers.critChance(0.2f));
        FakeTower tower = FakeTower.offering(world, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob burning = EnemyFixtures.plainMob(world, 100000, 3);
        burning.applyEffect(Effect.burn(Damage.magic(1), DamageSink.none()));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aDamageOverTimeTickNeverCritsEvenWhenEveryRollWouldSucceed() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", "Precision", 10)
                .withModifiers(TowerModifiers.critChance(1f));
        FakeTower tower = FakeTower.offering(alwaysCrits, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFixtures.plainMob(alwaysCrits, 100000, 3);

        boolean critical = tower.dealPeriodicDamage(enemy, Damage.magic(1000));

        assertThat(critical).isFalse();
        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void aCriticalHitStartsStrongerDamageOverTimeButAPlainHitDoesNot() {
        GameWorld world = WorldFixtures.newWorld();
        world.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("sharp", "Sharp", 10)
                .withModifiers(TowerModifiers.critMultiplier(0.5f));
        FakeTower tower = FakeTower.offering(world, 0, 0, UpgradeTree.of(node));
        tower.buyUpgrade(node);
        EnemyMob enemy = EnemyFixtures.plainMob(world, 100000, 3);

        assertThat(tower.potencyOfHit(enemy, true)).isEqualTo(AttackProfile.DEFAULT_CRIT_MULTIPLIER + 0.5f);
        assertThat(tower.potencyOfHit(enemy, false)).isEqualTo(1f);
    }

    @Test
    void aTiersBonusIsHeldFromTheXpThatOpensItAndSellingPaysNothingForIt() {
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(),
                UpgradeTier.gated(150, 0).withBonus(TowerModifiers.range(1f))));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, tree);
        this.context.towers().add(tower);
        float base = tower.stats().range();
        int sellBare = tower.getSellPrice();
        tower.earnXp(149);
        float oneShort = tower.stats().range();

        tower.earnXp(1);

        assertThat(oneShort).isEqualTo(base);
        assertThat(tower.stats().range()).isEqualTo(base + 1f);
        assertThat(tower.getSellPrice()).isEqualTo(sellBare);
    }

    @Test
    void aBonusThatGrowsWithFocusIsInThePublishedStatsAsTheFocusStandsAndNoHitAddsItAgain() {
        this.context.economy().startEconomy(100, 5);
        UpgradeNode focused = UpgradeNode.of("focused", "Focused", 1).withHitBonus(HitBonus.per(
                new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0), HitAdjustment.critChance(0.1f)
                        .plus(HitAdjustment.range(0.5f))));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(List.of(focused)));
        tower.buyUpgrade(focused);
        TowerStats unfocused = tower.stats();

        tower.focusOn(3);
        TowerStats built = tower.stats();
        HitAdjustment onAHit = tower.hitOn(FakeEnemyMob.at(0, 0), 3);
        tower.focusOn(1);
        TowerStats faded = tower.stats();

        assertThat(built.critChance()).isCloseTo(unfocused.critChance() + 0.3f, within(1e-6f));
        assertThat(built.range()).isCloseTo(unfocused.range() + 1.5f, within(1e-6f));
        assertThat(faded.critChance()).isCloseTo(unfocused.critChance() + 0.1f, within(1e-6f));
        assertThat(onAHit).isEqualTo(HitAdjustment.none());
    }

    @Test
    void aFreeTiersOwnNodeIsNeverBoughtOrOwnedSoNoSaveStoresIt() {
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(),
                UpgradeTier.gated(150, 0).withBonus(TowerModifiers.range(1f))));
        this.context.economy().startEconomy(100, 5);
        FakeTower tower = FakeTower.offering(this.context, 0, 0, tree);
        this.context.towers().add(tower);
        UpgradeNode granted = tree.tierNodeOf(1).orElseThrow();
        tower.earnXp(150);
        int credits = this.context.economy().state().credits();

        boolean bought = tower.buyUpgrade(granted) || tower.buyPath(granted);

        assertThat(bought).isFalse();
        assertThat(tower.upgrades().owned()).isEmpty();
        assertThat(this.context.economy().state().credits()).isEqualTo(credits);
        assertThat(tree.isHeld(granted, tower.experience().xp(), tower.upgrades())).isTrue();
    }

    @Test
    void aFreeTiersPerkActsFromTheXpThatOpensItWithoutBeingOwned() {
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(),
                UpgradeTier.gated(150, 0).withPerk("Mark", Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 2)))));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, tree);
        this.context.towers().add(tower);
        FakeEnemyMob before = FakeEnemyMob.at(0, 0);
        tower.reportHit(before);
        FakeEnemyMob after = FakeEnemyMob.at(0, 0);

        tower.earnXp(150);
        tower.reportHit(after);

        assertThat(before.effectStacks(EffectKind.SUNDERED)).isZero();
        assertThat(after.effectStacks(EffectKind.SUNDERED)).isEqualTo(2);
        assertThat(tower.upgrades().owned()).isEmpty();
        assertThat(tree.isGranted(tree.tierNodeOf(1).orElseThrow())).isTrue();
    }

    @Test
    void aPaidTiersBonusWaitsForItsUnlockAsWellAsItsXp() {
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(),
                UpgradeTier.gated(0, 25).withBonus(TowerModifiers.range(1f))));
        this.context.economy().startEconomy(100, 5);
        FakeTower tower = FakeTower.offering(this.context, 0, 0, tree);
        this.context.towers().add(tower);
        float base = tower.stats().range();

        tower.buyUpgrade(tree.unlockOf(1).orElseThrow());

        assertThat(tower.stats().range()).isEqualTo(base + 1f);
    }

    @Test
    void theInfoPanelShowsATiersBonusApartFromTheStatsBase() {
        UpgradeNode node = UpgradeNode.of("veteran", "Veteran", 10).inTier(1);
        UpgradeTree tree = UpgradeTree.of(node).withTiers(List.of(UpgradeTier.open(),
                UpgradeTier.gated(150, 0).withBonus(TowerModifiers.range(1f))));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, tree);
        this.context.towers().add(tower);
        tower.earnXp(150);

        TowerStatLine range = tower.inspect().stats().getFirst();

        assertThat(range.stat()).isEqualTo(TowerStat.RANGE);
        assertThat(range.base()).isEqualTo(tower.baseRange());
        assertThat(range.current()).isEqualTo(tower.baseRange() + 1f);
        assertThat(range.breakdown().added()).extracting(part -> part.source().plain())
                .containsExactly(UpgradeTier.nameOf(1));
    }

    @Test
    void inspectReportsEachStatAsAuthoredAndAsItIsNow() {
        SniperTower tower = new SniperTower(this.context, 0, 0);
        this.context.towers().add(tower);

        TowerFixtures.buffed(tower, this.context, TowerModifiers.rangePercent(0.2f));

        TowerInspection inspection = tower.inspect();
        assertThat(inspection.stats().getFirst().stat()).isEqualTo(TowerStat.RANGE);
        assertThat(inspection.stats().getFirst().base()).isEqualTo(SniperTower.RANGE);
        assertThat(inspection.stats().getFirst().current()).isEqualTo(tower.stats().range());
        assertThat(inspection.stats().getFirst().breakdown().added()).extracting(part -> part.source().mentions())
                .containsExactly(List.of(TowerTerm.LIT));
        assertThat(tower.stats().range()).isGreaterThan(SniperTower.RANGE);
        assertThat(inspection.auras()).isEqualTo(1);
    }

    @Test
    void inspectCountsKillsAndTheDamageThatLanded() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.none());

        tower.dealDamage(EnemyFixtures.plainMob(this.context, 1, 1), Damage.physical(1_000_000));

        assertThat(tower.inspect().kills()).isEqualTo(1);
        assertThat(tower.inspect().damageDealt()).isEqualTo(tower.getDamageDealt()).isPositive();
    }

    @Test
    void aNodeNotYetOwnedPreviewsTheRowItMovesOnTheLastingStats() {
        UpgradeNode range = UpgradeNode.of("test.range", "Range", 1).withModifiers(TowerModifiers.range(1f));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(List.of(range)));
        this.context.towers().add(tower);

        UpgradeBonus bonus = tower.wordsOf(range).stats().getFirst();

        assertThat(bonus.stat()).isEqualTo(TowerStat.RANGE);
        assertThat(bonus.preview()).hasValueSatisfying(preview -> {
            assertThat(preview.stat()).isEqualTo(TowerStat.RANGE);
            assertThat(preview.before()).isEqualTo(tower.stats().range());
            assertThat(preview.after()).isGreaterThan(preview.before());
        });
    }

    @Test
    void aPassiveTowerReportsNoDamageNoCadenceAndNoCritChance() {
        AuraTower aura = new AuraTower(this.context, 0, 0);

        assertThat(aura.inspect().stats()).extracting(TowerStatLine::stat)
                .doesNotContain(TowerStatRow.PHYSICAL_DAMAGE, TowerStatRow.MAGIC_DAMAGE, TowerStatRow.FIRE_RATE,
                        TowerStatRow.PHYSICAL_DPS, TowerStatRow.MAGIC_DPS, TowerStat.CRIT_CHANCE);
    }

    @Test
    void dealDamageReturnsWhetherTheHitWasCriticalMatchingTheDamageItLanded() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        alwaysCrits.economy().startEconomy(100, 5);
        UpgradeNode node = UpgradeNode.of("precision", "Precision", 10)
                .withModifiers(TowerModifiers.critChance(0.5f));
        FakeTower crittingTower = FakeTower.offering(alwaysCrits, 0, 0, UpgradeTree.of(node));
        crittingTower.buyUpgrade(node);
        EnemyMob crittingTarget = EnemyFixtures.plainMob(alwaysCrits, 100000, 3);

        boolean wasCritical = crittingTower.dealDamage(crittingTarget, Damage.physical(1000));

        assertThat(wasCritical).isTrue();

        GameWorld neverCrits = WorldFixtures.newWorld(() -> 0.99);
        neverCrits.economy().startEconomy(100, 5);
        FakeTower nonCrittingTower = FakeTower.offering(neverCrits, 0, 0, UpgradeTree.of(node));
        nonCrittingTower.buyUpgrade(node);
        EnemyMob nonCrittingTarget = EnemyFixtures.plainMob(neverCrits, 100000, 3);

        boolean wasNotCritical = nonCrittingTower.dealDamage(nonCrittingTarget, Damage.physical(1000));

        assertThat(wasNotCritical).isFalse();
    }

    @Test
    void aBurningTargetAgainstATowerWithNoCritChanceStillNeverCrits() {
        GameWorld alwaysCrits = WorldFixtures.newWorld(() -> 0.0);
        FakeTower tower = FakeTower.offering(alwaysCrits, 0, 0, UpgradeTree.none());
        EnemyMob burning = EnemyFixtures.plainMob(alwaysCrits, 100000, 3);
        burning.applyEffect(Effect.burn(Damage.magic(1), DamageSink.none()));

        tower.dealDamage(burning, Damage.physical(1000));

        assertThat(tower.getDamageDealt()).isEqualTo(1000);
    }

    @Test
    void everyHitATowerDealsCarriesItsOwnOriginAndNoOtherTowersOne() {
        SniperTower first = new SniperTower(this.context, 1, 1);
        SniperTower second = new SniperTower(this.context, 2, 2);
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);

        first.dealDamage(target, Damage.physical(1));
        first.dealPeriodicDamage(target, Damage.physical(1));
        second.dealDamage(target, Damage.physical(1));

        List<AttackOrigin> origins = target.attackers().stream().map(AttackProfile::origin).toList();
        assertThat(origins.get(0)).isSameAs(origins.get(1)).isNotSameAs(AttackOrigin.none());
        assertThat(origins.get(2)).isNotSameAs(origins.get(0)).isNotSameAs(AttackOrigin.none());
    }

    @Test
    void damageAbsorbedByAShieldCountsTowardDamageDealtButNotAKill() {
        SniperTower tower = new SniperTower(this.context, 0, 0);
        EnemyMob enemy = EnemyFixtures.plainMob(this.context, 50, 3);
        enemy.applyEffect(Effect.shield(5000f, 100, DamageSink.none()));

        tower.dealDamage(enemy, Damage.physical(3000), AttackProfile.none());

        assertThat(tower.getDamageDealt()).isEqualTo(3000);
        assertThat(tower.getKillCount()).isZero();
        assertThat(enemy.getHealth()).isEqualTo(5000);
    }

    @Test
    void aStatusAnEveryNthShotProcGrantsIsInThatShotsStats() {
        Proc everySecond = Proc.everyNthShot(2, new ProcPayload.GrantStatus(
                TowerStatus.nextShots(TowerTerm.FOURTH_SHOT, 1, TowerModifiers.critChance(0.5f))));
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.none()).withBaseProcs(List.of(everySecond));

        TowerStats first = tower.beginShot();
        TowerStats second = tower.beginShot();

        assertThat(first.critChance()).isZero();
        assertThat(second.critChance()).isEqualTo(0.5f);
        assertThat(tower.stats().critChance()).isZero();
    }

    @Test
    void aTowerThatDealsMagicReportsItsDamageRowAsMagic() {
        UpgradeNode arcane = UpgradeNode.of("arcane", "Arcane", 10)
                .withCapability(TowerCapability.MAGIC_DAMAGE);
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(arcane));
        StatTerm before = tower.inspect().stats().get(1).stat();

        UpgradePaths.buy(tower, this.context, "Arcane");

        assertThat(before).isEqualTo(TowerStatRow.PHYSICAL_DAMAGE);
        assertThat(tower.inspect().stats().get(1).stat()).isEqualTo(TowerStatRow.MAGIC_DAMAGE);
    }

    @Test
    void anOnHitProcThatAppliesStacksPutsThemOnTheEnemyHit() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.none())
                .withBaseProcs(List.of(Proc.onHit(new ProcPayload.ApplyStacks(StackingDebuff.SUNDERED, 2))));
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);

        tower.reportHit(target);

        assertThat(target.effectStacks(EffectKind.SUNDERED)).isEqualTo(2);
    }

    @Test
    void aTowerWithNoBonusesAdjustsNothing() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.none());

        HitAdjustment adjustment = tower.hitAdjustment(FakeEnemyMob.at(0, 0),
                HitKind.DIRECT, 0.5f, tower.stats());

        assertThat(adjustment).isSameAs(HitAdjustment.none());
    }

    @Test
    void aTierThatXpOpensBetweenTwoTurnsIsStampedAsARiseAtTheSecond() {
        UpgradeNode node = UpgradeNode.of("n", "N", 5).inTier(1);
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(node).withTiers(FREE_AT_150));
        tower.beginTick(10);
        tower.earnXp(150);

        TierRise before = tower.tierRise();
        tower.beginTick(11);

        assertThat(before).isEqualTo(TierRise.none());
        assertThat(tower.tierRise()).isEqualTo(new TierRise(0, 1, 11));
    }

    @Test
    void aTowerWhoseTierIsAlreadyOpenAtItsFirstTurnHasNotRisen() {
        UpgradeNode node = UpgradeNode.of("n", "N", 5).inTier(1);
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.of(node).withTiers(FREE_AT_150));
        tower.earnXp(150);

        tower.beginTick(10);

        assertThat(tower.tierRise()).isEqualTo(TierRise.none());
    }

    @Test
    void aTowerPlacedInPlayCountsItsTicksFromItsFirstTurnUntilTheWindowCloses() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, UpgradeTree.none());

        OptionalInt before = tower.ticksSincePlaced(10, 5);
        tower.beginTick(10);
        tower.beginTick(11);

        assertThat(before).isEmpty();
        assertThat(tower.ticksSincePlaced(13, 5)).hasValue(3);
        assertThat(tower.ticksSincePlaced(16, 5)).isEmpty();
    }

    @Test
    void aTowerASaveBroughtBackWasNotPlacedNow() {
        FakeTower built = FakeTower.offering(this.context, 0, 0, UpgradeTree.none());
        FakeTower loaded = FakeTower.offering(this.context, 1, 0, UpgradeTree.none());

        loaded.restore(built.record());
        loaded.beginTick(10);

        assertThat(loaded.ticksSincePlaced(11, 5)).isEmpty();
    }
}
