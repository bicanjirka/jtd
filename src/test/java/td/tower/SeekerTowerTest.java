package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.projectile.MissileLook;
import td.projectile.MissileProjectile;
import td.stat.StatModifiers;
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

        assertThat(target.onlyHitAmount()).isEqualTo(tower.damageCurrent());
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
    void brittleLeavesWhateverItFreezesBrittleForAsLongAsTheFreeze() {
        SeekerTower tower = this.seekerWith("Brittle");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(effectOn(target, EffectKind.BRITTLE).remainingTicks())
                .isEqualTo(effectOn(target, EffectKind.FREEZE).remainingTicks());
    }

    /** Ticks {@code tower} until it has launched {@code missiles}, then flies them all. */
    private void fireAndFly(SeekerTower tower, int missiles) {
        int launched = 0;
        for (int t = 1; launched < missiles && t < 5000; t++) {
            int before = this.context.projectiles().getProjectiles().size();
            tower.doTick(t);
            launched += this.context.projectiles().getProjectiles().size() - before;
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);
    }

    private static Effect effectOn(FakeEnemyMob mob, EffectKind kind) {
        return mob.appliedEffects().stream().filter(effect -> effect.kind() == kind).findFirst().orElseThrow();
    }

    private static long freezesOn(FakeEnemyMob mob) {
        return mob.appliedEffects().stream().filter(e -> e.kind() == EffectKind.FREEZE).count();
    }

    @Test
    void arcanePayloadMakesEveryThirdMissileUnravelInsteadOfFreezing() {
        SeekerTower tower = this.seekerWith("Arcane Payload");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.fireAndFly(tower, 3);

        assertThat(freezesOn(target)).isEqualTo(2);
        assertThat(target.effectStacks(EffectKind.UNRAVELED)).isEqualTo(1);
        assertThat(target.hits()).hasSize(3);
    }

    @Test
    void empPayloadStripsShieldsAndHealsAndSilencesOnItsTurn() {
        SeekerTower tower = this.seekerWith("Arcane Payload", "EMP Payload");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.fireAndFly(tower, 6);

        assertThat(target.dispels()).isEqualTo(1);
        assertThat(target.hasEffect(EffectKind.SILENCED)).isTrue();
        assertThat(target.effectStacks(EffectKind.UNRAVELED)).isEqualTo(1);
    }

    @Test
    void tracerPayloadRevealsAndMarksOnItsTurn() {
        SeekerTower tower = this.seekerWith("Arcane Payload", "EMP Payload", "Tracer Payload");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.fireAndFly(tower, 9);

        assertThat(target.hasEffect(EffectKind.MARKED)).isTrue();
        assertThat(target.hasEffect(EffectKind.REVEALED)).isTrue();
    }

    @Test
    void aMissileIsDrawnInTheColourOfWhatItCarries() {
        SeekerTower tower = this.seekerWith("Full Rack");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int t = 1; t <= 120; t++) {
            tower.doTick(t);
        }

        assertThat(this.context.projectiles().getProjectiles().stream()
                .map(missile -> ((MissileProjectile) missile).stats().look()).toList())
                .containsExactly(MissileLook.CRYO, MissileLook.ARCANE, MissileLook.EMP);
    }

    @Test
    void absoluteZeroFreezesEverythingWithinACellOfTheImpactAndNothingFurther() {
        SeekerTower tower = this.seekerWith("Absolute Zero");
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
        SeekerTower tower = this.seekerWith("Absolute Zero");
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
        SeekerTower tower = this.seekerWith("Absolute Zero");
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
        SeekerTower tower = this.seekerWith("Absolute Zero");
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
        SeekerTower tower = this.seekerWith("Frostbite");
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
    void aFreezeLastsAsLongAsTheTowersFreezeDuration() {
        SeekerTower tower = this.seekerGiven(TowerModifiers.freezeDuration(15));
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(effectOn(target, EffectKind.FREEZE).remainingTicks())
                .isEqualTo(Math.round(tower.stats().value(TowerStat.FREEZE_DURATION)))
                .isGreaterThan(Math.round(towerAt(8, 3).stats().value(TowerStat.FREEZE_DURATION)));
    }

    @Test
    void aBiggerNestHoldsMoreMissiles() {
        SeekerTower tower = TowerFixtures.buffed(this.attunedTower(), this.context, TowerModifiers.nestCapacity(2));
        this.context.enemies().setEnemies(new EnemyMob[]{});

        int tick = tickUntilNestHolds(tower, 1, nestCapacity(tower));
        for (int t = tick; t < tick + 300; t++) {
            tower.doTick(t);
        }

        assertThat(tower.getNestStored()).isEqualTo(nestCapacity(tower)).isGreaterThan(nestCapacity(towerAt(8, 3)));
    }

    @Test
    void anExtraMissilePerLaunchFiresOneMoreMissileAtOnce() {
        SeekerTower tower = this.seekerGiven(TowerModifiers.missilesPerLaunch(1));
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100)});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(2);
    }

    /** A plain Seeker that a neighbour buffs with {@code buff}. */
    private SeekerTower seekerGiven(StatModifiers<TowerStat> buff) {
        return TowerFixtures.buffed(towerAt(3, 3), this.context, buff);
    }

    /** How many missiles {@code tower} launches on {@code tick}. */
    private int launchedOn(SeekerTower tower, int tick) {
        int before = this.context.projectiles().getProjectiles().size();
        tower.doTick(tick);
        return this.context.projectiles().getProjectiles().size() - before;
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
        return this.seekerWith("Deep Freeze II");
    }

    private SeekerTower attunedTower() {
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Attune");
        return tower;
    }

    @Test
    void downgradingTheNodeThatBuiltTheNestLetsItsStoredMissilesGo() {
        SeekerTower tower = this.attunedTower();
        int tick = tickUntilNestHolds(tower, 1, 1);

        tower.downgrade(UpgradePaths.named(tower, "Attune"));
        tower.beginTick(tick);

        assertThat(tower.getNestStored()).isZero();
    }

    /** Ticks {@code tower} from {@code from} until its nest holds {@code stored} missiles. */
    private static int tickUntilNestHolds(SeekerTower tower, int from, int stored) {
        int tick = from;
        while (tower.getNestStored() < stored && tick < from + 5000) {
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
    void theMissilesSpeedReadsInCellsASecondBesideItsSize() {
        SeekerTower tower = towerAt(3, 3);

        assertThat(tower.inspect().stats()).extracting(TowerStatLine::stat)
                .contains(TowerStatRow.PROJECTILE_SPEED, TowerStatRow.PROJECTILE_SIZE);
        TowerStatLine speed = tower.inspect().stats().stream()
                .filter(line -> line.stat() == TowerStatRow.PROJECTILE_SPEED).findFirst().orElseThrow();
        assertThat(speed.current()).isEqualTo(SeekerTower.MISSILE_SPEED * TickRate.TICKS_PER_SECOND / BoardFixtures.SCALE);
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
    void attunedTheCooldownLoadsOneMissileIntoTheNestUpToItsCapacityWithNoEnemyInRange() {
        SeekerTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{});
        int capacity = nestCapacity(tower);

        tower.doTick(1);
        int loadedAtFirst = tower.getNestStored();
        int tick = tickUntilNestHolds(tower, 2, capacity);
        for (int t = tick; t < tick + 200; t++) {
            tower.doTick(t);
        }

        assertThat(loadedAtFirst).isEqualTo(1);
        assertThat(tower.getNestStored()).isEqualTo(capacity);
        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aFullNestLaunchesItsMissilesFourTicksApartOnceAnEnemyArrives() {
        SeekerTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{});
        int tick = tickUntilNestHolds(tower, 1, 3);
        assertThat(nestCapacity(tower)).isGreaterThanOrEqualTo(3);
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
        int cooldown = (int) Math.round((tower.coolDownMax + 1) / tower.attackRate());

        for (int t = 1; t <= 3 * cooldown; t++) {
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

    private static int nestCapacity(SeekerTower tower) {
        return Math.round(tower.stats().value(TowerStat.NEST_CAPACITY));
    }

    private SeekerTower seekerWith(String... nodes) {
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    @Test
    void missilesLaunchedTogetherGoToDifferentEnemies() {
        SeekerTower tower = this.seekerGiven(TowerModifiers.missilesPerLaunch(1));
        FakeEnemyMob quick = FakeEnemyMob.at(100, 100).movingAt(3f);
        FakeEnemyMob slow = FakeEnemyMob.at(110, 100).movingAt(1f);
        this.context.enemies().setEnemies(new EnemyMob[]{quick, slow});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(quick.hits()).hasSize(1);
        assertThat(slow.hits()).hasSize(1);
    }

    @Test
    void broodsSalvoSpreadsAcrossDifferentTargets() {
        SeekerTower tower = this.seekerWith("Brood");
        this.context.enemies().setEnemies(new EnemyMob[]{});
        int tick = tickUntilNestHolds(tower, 1, nestCapacity(tower));
        FakeEnemyMob first = FakeEnemyMob.at(100, 100).movingAt(3f);
        FakeEnemyMob second = FakeEnemyMob.at(110, 100).movingAt(2f);
        FakeEnemyMob third = FakeEnemyMob.at(120, 100).movingAt(1f);
        this.context.enemies().setEnemies(new EnemyMob[]{first, second, third});

        for (int t = tick; t < tick + 4 * nestCapacity(tower); t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(first.hits()).isNotEmpty();
        assertThat(second.hits()).isNotEmpty();
        assertThat(third.hits()).isNotEmpty();
    }

    @Test
    void shatterburstBurstsAroundAFrozenEnemyItHitsAndSilencesAndUnravelsWhatItCatches() {
        SeekerTower tower = this.seekerWith("Shatterburst");
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        FakeEnemyMob neighbour = FakeEnemyMob.ghostAt(120, 100);
        FakeEnemyMob far = FakeEnemyMob.ghostAt(400, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{frozen, neighbour, far});

        int launched = this.launchedOn(tower, 1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(neighbour.hits()).hasSize(launched)
                .allSatisfy(hit -> assertThat(hit.amount()).isLessThan(tower.damageCurrent()));
        assertThat(neighbour.hasEffect(EffectKind.SILENCED)).isTrue();
        assertThat(neighbour.effectStacks(EffectKind.UNRAVELED)).isEqualTo(launched);
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void shatterburstBurstsOnlyFromAMissileThatHitAnEnemyAlreadyFrozen() {
        SeekerTower tower = this.seekerWith("Shatterburst");
        FakeEnemyMob fresh = FakeEnemyMob.at(100, 100);
        FakeEnemyMob neighbour = FakeEnemyMob.ghostAt(120, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{fresh, neighbour});

        int launched = this.launchedOn(tower, 1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        // the first missile freezes the enemy; every later one finds it frozen
        assertThat(neighbour.hits()).hasSize(launched - 1);
    }

    @Test
    void rearmLaunchesOneMoreMissileFromAFreshFreezeAndTheRearmedOneLaunchesNoMore() {
        SeekerTower rearming = this.seekerWith("Rearm");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        int launched = this.launchedOn(rearming, 1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        // one more from the first, fresh freeze, and none from the rearmed missile
        assertThat(target.hits()).hasSize(launched + 1);
    }

    @Test
    void overTheHorizonLetsItFireAtAMarkedEnemyFromOneAndAHalfTimesItsRangeAndNoOther() {
        SeekerTower tower = this.seekerWith("Range III");
        float reach = tower.rangeReal();
        FakeEnemyMob marked = FakeEnemyMob.at(112 + reach * 1.3, 112);
        marked.applyEffect(Effect.marked(100, DamageSink.none()));
        FakeEnemyMob plain = FakeEnemyMob.at(112 + reach * 1.3, 140);
        this.context.enemies().setEnemies(new EnemyMob[]{plain, marked});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isSameAs(marked);
    }

    @Test
    void withoutOverTheHorizonAMarkedEnemyBeyondRangeIsLeftAlone() {
        SeekerTower tower = towerAt(3, 3);
        FakeEnemyMob marked = FakeEnemyMob.at(112 + tower.rangeReal() * 1.3, 112);
        marked.applyEffect(Effect.marked(100, DamageSink.none()));
        this.context.enemies().setEnemies(new EnemyMob[]{marked});

        tower.doTick(1);

        assertThat(tower.getCurrentTarget()).isNull();
    }

    @Test
    void arcaneWarheadAppliesOneStackToAFreshTargetAndTwoToOneAlreadyFrozen() {
        SeekerTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Arcane Warhead");
        FakeEnemyMob fresh = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{fresh});
        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);
        FakeEnemyMob frozen = FakeEnemyMob.at(100, 100);
        frozen.reportFrozen();
        this.context.enemies().setEnemies(new EnemyMob[]{frozen});
        int wait = (int) Math.ceil((tower.coolDownMax + 1) / tower.attackRate());
        for (int t = 2; t <= wait + 2; t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(stacksApplied(fresh)).isEqualTo(1);
        assertThat(stacksApplied(frozen)).isEqualTo(2);
    }

    private static int stacksApplied(FakeEnemyMob mob) {
        return mob.appliedEffects().stream().filter(e -> e.kind() == EffectKind.UNRAVELED)
                .mapToInt(Effect::stacks).sum();
    }

    @Test
    void theNextPayloadMissileIsPrimed() {
        SeekerTower tower = this.seekerWith("Arcane Payload");
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100)});

        this.fireAndFly(tower, 2);

        assertThat(tower.conditions()).extracting(TowerBuffState::term).contains(TowerTerm.ARCANE_MISSILE);
        assertThat(tower.progress()).singleElement().satisfies(progress -> assertThat(progress.count()).isEqualTo(2));
    }
}
