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
    void deepFreezeIiBumpsTheFreezeDurationBeyondTheBase() {
        SeekerTower tower = towerAt(3, 3);
        UpgradeNode deepFreezeTwo = UpgradePaths.named(tower, "Deep Freeze II");
        int durationBeforeChoosing = tower.getFreezeDurationTicks();

        tower.onUpgradeBought(deepFreezeTwo);

        assertThat(tower.getFreezeDurationTicks()).isGreaterThan(durationBeforeChoosing);
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
