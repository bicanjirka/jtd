package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeekerTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private SeekerTower towerAt(int cellX, int cellY) {
        return new SeekerTower(this.context, cellX, cellY);
    }

    @Test
    void firingLaunchesExactlyOneMissileAtTheTarget() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(1);
    }

    @Test
    void theMissileEventuallyDealsMagicDamageAndFreezesTheTarget() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.onlyHitAmount()).isEqualTo(DamageUnits.ofPoints(SeekerTower.DAMAGE_POINTS));
        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.MAGIC);
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.FREEZE);
    }

    @Test
    void aGhostIsNeverTargeted() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void noTargetInRangeFiresNoMissile() {
        SeekerTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void twinWarheadIsChoosableOnceAwakenIsBoughtAndAppliesItsFireRateBonus() {
        this.context.economy().startEconomy(1000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.awakenVeteran(tower);
        UpgradeNode twinWarhead = UpgradePaths.named(tower, "Twin Warhead");
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 10; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }

        boolean chosen = tower.buyUpgrade(twinWarhead);

        assertThat(chosen).isTrue();
        assertThat(tower.coolDownCurrent()).isLessThan(tower.coolDownMax);
    }

    @Test
    void deepFreezeIsNotYetChoosableBeforeAttuneIsBought() {
        this.context.economy().startEconomy(1000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradeNode deepFreeze = UpgradePaths.named(tower, "Deep Freeze");

        boolean chosen = tower.buyUpgrade(deepFreeze);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void deepFreezeMakesEveryFreezeHalfAgainAsLong() {
        SeekerTower tower = this.seekerWith("Deep Freeze");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(tower.getFreezeDurationTicks()).isEqualTo(45);
        assertThat(target.appliedEffects().stream().filter(e -> e.kind() == EffectKind.FREEZE).findFirst().orElseThrow()
                .remainingTicks()).isEqualTo(45);
    }

    @Test
    void deepFreezeIiAddsThirtyPercentDamageAndTenPercentCrit() {
        SeekerTower plain = towerAt(3, 3);
        SeekerTower deep = this.seekerWith("Deep Freeze", "Deep Freeze II");

        assertThat(deep.damageCurrent()).isEqualTo(Math.round(plain.damageCurrent() * 1.3f));
        assertThat(deep.critChance()).isEqualTo(0.1f);
    }

    @Test
    void brittleLeavesWhateverItFreezesBrittleForAsLongAsTheFreeze() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.appliedEffects().stream().filter(e -> e.kind() == EffectKind.BRITTLE).toList())
                .singleElement().extracting(td.effect.Effect::remainingTicks).isEqualTo(45);
    }

    @Test
    void absoluteZeroFreezesEverythingWithinACellOfTheImpactAndNothingFurther() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle", "Transcendent",
                "Absolute Zero");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob near = FakeEnemyMob.ghostAt(120, 100);
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(140, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target, near, outside});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.hasEffect(EffectKind.FREEZE)).isTrue();
        assertThat(near.hasEffect(EffectKind.FREEZE)).isTrue();
        assertThat(outside.hasEffect(EffectKind.FREEZE)).isFalse();
    }

    @Test
    void absoluteZeroShattersAnEnemyItFrozeWhenItsFreezeEnds() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle", "Transcendent",
                "Absolute Zero");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob bystander = FakeEnemyMob.ghostAt(100, 145);
        this.context.enemies().setEnemies(new EnemyMob[]{target, bystander});
        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);
        int hitsBefore = bystander.hits().size();

        target.expire(EffectKind.FREEZE);
        tower.doTick(2);

        assertThat(bystander.hits()).hasSize(hitsBefore + 1);
        assertThat(bystander.hits().getLast()).isEqualTo(Damage.magic(Math.round(tower.damageCurrent() * 0.5f * 2f)));
    }

    @Test
    void absoluteZeroShattersTwiceAsHardWhenItKillsAFrozenEnemy() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle", "Transcendent",
                "Absolute Zero");
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        frozen.dieOnAnyHit();
        FakeEnemyMob neighbour = FakeEnemyMob.at(140, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{frozen, neighbour});

        tower.dealDamage(frozen, Damage.magic(1));

        assertThat(neighbour.onlyHitAmount()).isEqualTo(Math.round(tower.damageCurrent() * 0.5f * 2f));
    }

    @Test
    void anEnemyThatThawsIsShatteredOnceNotEveryTick() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle", "Transcendent",
                "Absolute Zero");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob bystander = FakeEnemyMob.ghostAt(100, 145);
        this.context.enemies().setEnemies(new EnemyMob[]{target, bystander});
        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);
        target.expire(EffectKind.FREEZE);
        tower.doTick(2);
        int afterThaw = bystander.hits().size();

        tower.doTick(3);
        tower.doTick(4);

        assertThat(bystander.hits()).hasSize(afterThaw);
    }

    @Test
    void frostbiteMakesHitsOnAFrozenOrFreezeDiminishedEnemyGuaranteedCritsAndNoOther() {
        SeekerTower tower = this.seekerWith("Deep Freeze", "Deep Freeze II", "Brittle", "Transcendent", "Frostbite");
        FakeEnemyMob fresh = FakeEnemyMob.at(100, 100);
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        FakeEnemyMob worn = FakeEnemyMob.at(100, 100);
        worn.reportFreezeDiminished();

        for (FakeEnemyMob enemy : new FakeEnemyMob[]{fresh, frozen, worn}) {
            this.context.enemies().setEnemies(new EnemyMob[]{enemy});
            for (int t = 1; t <= 50; t++) {
                tower.doTick(t);
            }
            TowerFixtures.flyProjectilesToCompletion(this.context);
        }

        assertThat(fresh.attackers().getFirst().guaranteedCrit()).isFalse();
        assertThat(frozen.attackers().getFirst().guaranteedCrit()).isTrue();
        assertThat(worn.attackers().getFirst().guaranteedCrit()).isTrue();
    }

    @Test
    void twinWarheadIiFiresTwoMissilesInsteadOfOne() {
        SeekerTower tower = towerAt(3, 3);
        tower.onUpgradeBought(UpgradePaths.named(tower, "Twin Warhead II"));
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(2);
    }

    @Test
    void deepFreezeTwoShattersAFrozenEnemyItKillsHurtingItsNeighbours() {
        SeekerTower tower = this.deepFreezeTwoSeeker();
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        frozen.dieOnAnyHit();
        FakeEnemyMob neighbour = FakeEnemyMob.at(120, 100);
        FakeEnemyMob far = FakeEnemyMob.at(400, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{frozen, neighbour, far});

        tower.dealDamage(frozen, Damage.magic(1));

        assertThat(neighbour.onlyHitAmount()).isEqualTo(Math.round(tower.damageCurrent() * 0.5f));
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void deepFreezeTwoDoesNotShatterAnEnemyThatWasNotFrozen() {
        SeekerTower tower = this.deepFreezeTwoSeeker();
        FakeEnemyMob unfrozen = FakeEnemyMob.at(100, 100);
        unfrozen.dieOnAnyHit();
        FakeEnemyMob neighbour = FakeEnemyMob.at(120, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{unfrozen, neighbour});

        tower.dealDamage(unfrozen, Damage.magic(1));

        assertThat(neighbour.hits()).isEmpty();
    }

    @Test
    void aShatterThatKillsAFrozenNeighbourDoesNotShatterItInTurn() {
        SeekerTower tower = this.deepFreezeTwoSeeker();
        FakeEnemyMob first = FakeEnemyMob.at(100, 100);
        first.reportFrozen();
        first.dieOnAnyHit();
        FakeEnemyMob second = FakeEnemyMob.at(120, 100);
        second.reportFrozen();
        second.dieOnAnyHit();
        FakeEnemyMob third = FakeEnemyMob.at(160, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{first, second, third});

        tower.dealDamage(first, Damage.magic(1));

        assertThat(second.isDead()).isTrue();
        assertThat(third.hits()).isEmpty();
    }

    private SeekerTower deepFreezeTwoSeeker() {
        this.context.economy().startEconomy(100000, 5);
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.awakenVeteran(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 25; i++) {
            tower.dealDamage(fodder, Damage.magic(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        tower.buyUpgrade(UpgradePaths.named(tower, "Deep Freeze"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Deep Freeze II"));
        return tower;
    }

    private SeekerTower attunedTower() {
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context);
        return tower;
    }

    /** Ticks {@code tower} from {@code from} until its nest holds {@code stored} missiles. */
    private static int tickUntilNestHolds(SeekerTower tower, int from, int stored) {
        int tick = from;
        while (tower.getNestStored() < stored) {
            tower.doTick(tick++);
        }
        return tick;
    }

    @Test
    void itFiresAtTheFastestEnemyInReachNotTheOneFurthestAlongThePath() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob walker = FakeEnemyMob.at(100, 100).movingAt(1f).withProgression(90);
        FakeEnemyMob runner = FakeEnemyMob.at(110, 100).movingAt(3f).withProgression(10);
        this.context.enemies().setEnemies(new EnemyMob[]{walker, runner});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isSameAs(runner);
    }

    @Test
    void itLeavesAFrozenEnemyAloneForTheNextRunner() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100).movingAt(0f).withProgression(90);
        FakeEnemyMob runner = FakeEnemyMob.at(110, 100).movingAt(2f).withProgression(10);
        this.context.enemies().setEnemies(new EnemyMob[]{frozen, runner});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isSameAs(runner);
    }

    @Test
    void theMissileFliesAtEightPixelsATickAndIsDrawnAsMagic() {
        SeekerTower tower = towerAt(3, 3);

        assertThat(SeekerTower.MISSILE_SPEED).isEqualTo(8f);
        assertThat(tower.inspect().stats()).extracting(TowerStatLine::stat)
                .contains(TowerStat.PROJECTILE_SPEED, TowerStat.PROJECTILE_SIZE);
        TowerStatLine speed = tower.inspect().stats().stream()
                .filter(line -> line.stat() == TowerStat.PROJECTILE_SPEED).findFirst().orElseThrow();
        assertThat(speed.current()).isEqualTo(8f * TickRate.TICKS_PER_SECOND / BoardFixtures.SCALE);
    }

    @Test
    void withoutAttuneNothingIsBankedWhileNoEnemyIsInRange() {
        SeekerTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        for (int t = 1; t <= 200; t++) {
            tower.doTick(t);
        }

        assertThat(tower.getNestStored()).isZero();
        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void attunedTheCooldownLoadsOneMissileIntoTheNestUpToThreeWithNoEnemyInRange() {
        SeekerTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);
        int loadedAtFirst = tower.getNestStored();
        int tick = tickUntilNestHolds(tower, 2, 3);
        for (int t = tick; t < tick + 200; t++) {
            tower.doTick(t);
        }

        assertThat(loadedAtFirst).isEqualTo(1);
        assertThat(tower.getNestStored()).isEqualTo(3);
        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aFullNestLaunchesItsMissilesFourTicksApartOnceAnEnemyArrives() {
        SeekerTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{});
        int tick = tickUntilNestHolds(tower, 1, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100)});

        List<Integer> launched = new ArrayList<>();
        int before = 0;
        for (int t = tick; t < tick + 9; t++) {
            tower.doTick(t);
            int now = this.context.projectiles().getProjectiles().size();
            if (now > before) {
                launched.add(t - tick);
            }
            before = now;
        }

        assertThat(launched).containsExactly(0, 4, 8);
    }

    @Test
    void attunedWithAnEnemyAlwaysInRangeItStillLaunchesOneMissilePerCooldown() {
        SeekerTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100)});

        for (int t = 1; t <= 100; t++) {
            tower.doTick(t);
        }

        assertThat(this.context.projectiles().getProjectiles()).hasSize(3);
    }

    @Test
    void aFreezeThatLandsCountsAsADeedOnceAttuned() {
        SeekerTower tower = this.attunedTower();
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tower.beginTick(1);

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    private SeekerTower seekerWith(String... nodes) {
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    @Test
    void twinWarheadMakesTheNestHoldOneMore() {
        SeekerTower tower = this.seekerWith("Twin Warhead");
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tickUntilNestHolds(tower, 1, 4);
        for (int t = 1000; t < 1300; t++) {
            tower.doTick(t);
        }

        assertThat(tower.getNestStored()).isEqualTo(4);
    }

    @Test
    void twinWarheadIiSendsTwoMissilesAtTwoDifferentEnemiesAndOneEnemyGetsBoth() {
        SeekerTower tower = this.seekerWith("Twin Warhead", "Twin Warhead II");
        FakeEnemyMob quick = FakeEnemyMob.at(100, 100).movingAt(3f);
        FakeEnemyMob slow = FakeEnemyMob.at(110, 100).movingAt(1f);
        this.context.enemies().setEnemies(new EnemyMob[]{quick, slow});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(quick.hits()).hasSize(1);
        assertThat(slow.hits()).hasSize(1);
    }

    @Test
    void broodMakesTheNestHoldSixAndASalvoSpreadsAcrossDifferentTargets() {
        SeekerTower tower = this.seekerWith("Twin Warhead", "Twin Warhead II", "Brood");
        this.context.enemies().setEnemies(new EnemyMob[]{});
        int tick = tickUntilNestHolds(tower, 1, 6);
        FakeEnemyMob first = FakeEnemyMob.at(100, 100).movingAt(3f);
        FakeEnemyMob second = FakeEnemyMob.at(110, 100).movingAt(2f);
        FakeEnemyMob third = FakeEnemyMob.at(120, 100).movingAt(1f);
        this.context.enemies().setEnemies(new EnemyMob[]{first, second, third});

        for (int t = tick; t < tick + 6; t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(first.hits()).isNotEmpty();
        assertThat(second.hits()).isNotEmpty();
        assertThat(third.hits()).isNotEmpty();
    }

    @Test
    void shatterburstBurstsAroundAFrozenEnemyItHitsAndSilencesAndUnravelsWhatItCatches() {
        SeekerTower tower = this.seekerWith("Twin Warhead", "Twin Warhead II", "Brood", "Transcendent", "Shatterburst");
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        FakeEnemyMob neighbour = FakeEnemyMob.ghostAt(120, 100);
        FakeEnemyMob far = FakeEnemyMob.ghostAt(400, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{frozen, neighbour, far});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        // Twin Warhead II sent two missiles, each of which hit the frozen enemy and burst.
        assertThat(neighbour.hits()).containsExactly(Damage.magic(Math.round(tower.damageCurrent() * 0.35f)),
                Damage.magic(Math.round(tower.damageCurrent() * 0.35f)));
        assertThat(neighbour.hasEffect(EffectKind.SILENCED)).isTrue();
        assertThat(neighbour.effectStacks(EffectKind.UNRAVELED)).isEqualTo(2);
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void shatterburstBurstsOnlyFromTheMissileThatHitAnEnemyAlreadyFrozen() {
        SeekerTower tower = this.seekerWith("Twin Warhead", "Twin Warhead II", "Brood", "Transcendent", "Shatterburst");
        FakeEnemyMob fresh = FakeEnemyMob.at(100, 100);
        FakeEnemyMob neighbour = FakeEnemyMob.ghostAt(120, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{fresh, neighbour});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        // Only the second of the two missiles found the enemy already frozen by the first.
        assertThat(neighbour.hits()).hasSize(1);
    }

    @Test
    void rearmLaunchesOneMoreMissileFromAFreshFreezeAndTheRearmedOneLaunchesNoMore() {
        SeekerTower rearming = this.seekerWith("Twin Warhead", "Twin Warhead II", "Brood", "Transcendent", "Rearm");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        rearming.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        // Two missiles from Twin Warhead II, one rearm from the fresh freeze, and no more.
        assertThat(target.hits()).hasSize(3);
    }

    @Test
    void overTheHorizonLetsItFireAtAMarkedEnemyFromOneAndAHalfTimesItsRangeAndNoOther() {
        SeekerTower tower = this.seekerWith("Twin Warhead", "Twin Warhead II", "Brood", "Transcendent", "Range",
                "Range II", "Range III");
        float reach = tower.rangeReal();
        FakeEnemyMob marked = FakeEnemyMob.at(112 + reach * 1.3, 112);
        marked.applyEffect(td.effect.Effect.marked(100, d -> {
        }));
        FakeEnemyMob plain = FakeEnemyMob.at(112 + reach * 1.3, 140);
        this.context.enemies().setEnemies(new EnemyMob[]{plain, marked});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isSameAs(marked);
    }

    @Test
    void withoutOverTheHorizonAMarkedEnemyBeyondRangeIsLeftAlone() {
        SeekerTower tower = this.seekerWith();
        FakeEnemyMob marked = FakeEnemyMob.at(112 + tower.rangeReal() * 1.3, 112);
        marked.applyEffect(td.effect.Effect.marked(100, d -> {
        }));
        this.context.enemies().setEnemies(new EnemyMob[]{marked});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isNull();
    }

    @Test
    void homingCurseAppliesOneStackToAFreshTargetAndTwoToOneAlreadyFrozen() {
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Homing Curse");
        FakeEnemyMob fresh = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{fresh});
        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        this.context.enemies().setEnemies(new EnemyMob[]{frozen});
        for (int t = 2; t <= tower.coolDownCurrent() + 2; t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(stacksApplied(fresh)).isEqualTo(1);
        assertThat(stacksApplied(frozen)).isEqualTo(2);
    }

    private static int stacksApplied(FakeEnemyMob mob) {
        return mob.appliedEffects().stream().filter(e -> e.kind() == EffectKind.VULNERABLE)
                .mapToInt(td.effect.Effect::stacks).sum();
    }
}
