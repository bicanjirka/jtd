package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.IceKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.seeker.FeedingMechanic;
import td.tower.seeker.MissileKind;
import td.tower.seeker.ReleaseMechanic;
import td.tower.seeker.ShatterMechanic;
import td.util.GameWorld;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeekerTowerTest {

    private static final int SCALE = BoardFixtures.SCALE;
    /** Well inside its range. */
    private static final int OUT = 2 * SCALE;

    private GameWorld context = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
    private int tick = 1;

    private SeekerTower towerWith(String... nodes) {
        SeekerTower tower = new SeekerTower(this.context, 3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    /** A walking enemy {@code dx}, {@code dy} from the tower. */
    private FakeEnemyMob enemyAt(SeekerTower tower, double dx, double dy) {
        return FakeEnemyMob.at(tower.getX() + dx, tower.getY() + dy).movingAt(1f);
    }

    private void enemies(EnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    /** Ticks the tower until a missile is in the air. */
    private void launch(SeekerTower tower) {
        for (int i = 0; i < 200 && this.context.projectiles().getProjectiles().isEmpty(); i++) {
            tower.doTick(this.tick++);
        }
    }

    /** Launches a missile and flies it until it lands. */
    private void landOne(SeekerTower tower) {
        this.launch(tower);
        TowerFixtures.flyProjectilesToCompletion(this.context);
    }

    /** Lands {@code count} missiles, each on a fresh enemy it freezes. */
    private void landFreezes(SeekerTower tower, int count) {
        for (int i = 0; i < count; i++) {
            this.enemies(this.enemyAt(tower, OUT, 0));
            this.landOne(tower);
        }
    }

    /** Lands the Frost missiles ahead of the nest's first second-kind missile, each on a fresh enemy. */
    private void spendTheFrostAhead(SeekerTower tower) {
        while (tower.nestMissiles().getFirst() == MissileKind.FROST) {
            this.landFreezes(tower, 1);
        }
    }

    /** The ticks until the nest takes in a missile, with no enemy about. */
    private int ticksToTheNextLoad(SeekerTower tower) {
        this.enemies();
        int before = tower.nestMissiles().size();
        int ticks = 0;
        while (tower.nestMissiles().size() == before && ticks < 200) {
            tower.doTick(this.tick++);
            ticks++;
        }
        return ticks;
    }

    @Test
    void aFightRunsWhileAnEnemyIsInReachAndTheQuietBeforeItIsKept() {
        SeekerTower tower = this.towerWith();
        boolean quietBeforeTheFirstTick = tower.ticksQuiet(this.tick).isPresent();
        for (int i = 0; i < 5; i++) {
            tower.doTick(this.tick++);
        }
        this.enemies(this.enemyAt(tower, OUT, 0));
        for (int i = 0; i < 3; i++) {
            tower.doTick(this.tick++);
        }
        int fighting = tower.ticksFighting(this.tick).orElseThrow();
        int quietBefore = tower.ticksQuiet(this.tick).orElseThrow();
        this.enemies();
        tower.doTick(this.tick++);

        assertThat(quietBeforeTheFirstTick).isFalse();
        assertThat(fighting).isEqualTo(3);
        assertThat(quietBefore).isEqualTo(5);
        assertThat(tower.ticksFighting(this.tick)).isEmpty();
        assertThat(tower.ticksQuiet(this.tick)).hasValue(1);
    }

    @Test
    void theNestLoadsWithNoEnemyInRangeUpToItsSize() {
        SeekerTower tower = this.towerWith();

        for (int i = 0; i < 500; i++) {
            tower.doTick(this.tick++);
        }

        assertThat(tower.nestMissiles()).hasSize(3);
        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void everyWaveStartsWithTheNestFull() {
        SeekerTower tower = this.towerWith("Wide Comb");

        tower.readyForWave();

        assertThat(tower.nestMissiles()).hasSize(5);
    }

    @Test
    void storedMissilesLaunchAGapApart() {
        SeekerTower tower = this.towerWith();
        tower.readyForWave();
        this.enemies(this.enemyAt(tower, OUT, 0));

        tower.doTick(1);
        int afterFirst = this.context.projectiles().getProjectiles().size();
        for (int t = 2; t <= 4; t++) {
            tower.doTick(t);
        }
        int beforeGap = this.context.projectiles().getProjectiles().size();
        tower.doTick(5);

        assertThat(afterFirst).isEqualTo(1);
        assertThat(beforeGap).isEqualTo(1);
        assertThat(this.context.projectiles().getProjectiles()).hasSize(2);
    }

    @Test
    void aMissileGoesForTheEnemyThatWouldLeaveSoonest() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob slowNearTheExit = this.enemyAt(tower, OUT, 0).withDistanceToExit(100).movingAt(0.5f);
        FakeEnemyMob fastFarBack = this.enemyAt(tower, -OUT, 0).withDistanceToExit(400).movingAt(4f);
        this.enemies(slowNearTheExit, fastFarBack);

        this.landOne(tower);

        assertThat(fastFarBack.hits()).hasSize(1);
        assertThat(slowNearTheExit.hits()).isEmpty();
    }

    @Test
    void theEnemyAMissileHitsTakesMagicAndIsFrozen() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.enemies(target);

        this.landOne(tower);

        assertThat(target.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.MAGIC);
        assertThat(target.hasEffect(EffectKind.FREEZE)).isTrue();
    }

    @Test
    void aHitAShieldTakesWholeFreezesNothing() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob shielded = this.enemyAt(tower, OUT, 0).shieldingEveryHit();
        this.enemies(shielded);

        this.landOne(tower);

        assertThat(shielded.hits()).hasSize(1);
        assertThat(shielded.hasEffect(EffectKind.FREEZE)).isFalse();
    }

    @Test
    void aMissileLandsWithTheStatsItWasLaunchedWithThoughTheChargeItSpentIsGone() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.enemies(target);
        tower.grantStatus(TowerStatus.nextShots(TowerTerm.HASTE, 1, TowerModifiers.damageTimes(3f)));
        int charged = tower.damageCurrent();

        this.landOne(tower);

        assertThat(tower.damageCurrent()).isLessThan(charged);
        assertThat(target.onlyHitAmount()).isEqualTo(charged);
    }

    @Test
    void aMissileWhoseEnemyIsFrozenInFlightTurnsToAnUnfrozenOneCloseBy() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob first = this.enemyAt(tower, OUT, 0).withDistanceToExit(10);
        FakeEnemyMob beside = this.enemyAt(tower, SCALE / 2.0, SCALE / 2.0).withDistanceToExit(500);
        this.enemies(first, beside);
        this.launch(tower);

        first.applyEffect(Effect.freeze(100, DamageSink.none()));
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(first.hits()).isEmpty();
        assertThat(beside.hits()).hasSize(1);
    }

    @Test
    void aMissileWhoseEnemyIsFrozenInFlightStaysOnItWhenNothingElseWalksCloseBy() {
        SeekerTower tower = this.towerWith();
        FakeEnemyMob only = this.enemyAt(tower, OUT, 0);
        this.enemies(only);
        this.launch(tower);

        only.applyEffect(Effect.freeze(100, DamageSink.none()));
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(only.hits()).isNotEmpty();
    }

    /**
     * Whether a missile whose enemy is gone at once finds one half a cell past the bare tower's hunt, with
     * {@code wider} cells more.
     */
    private boolean huntsHalfACellPastTheBareHunt(float wider) {
        this.context = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
        SeekerTower tower = this.towerWith();
        double off = (tower.stats().value(TowerStat.HUNT_RADIUS) + 0.5) * SCALE;
        TowerFixtures.buffed(tower, this.context, TowerModifiers.huntRadius(wider));
        FakeEnemyMob first = this.enemyAt(tower, OUT, 0).withDistanceToExit(10);
        FakeEnemyMob beyond = this.enemyAt(tower, -off, 0).withDistanceToExit(500);
        this.enemies(first, beyond);
        tower.doTick(this.tick++);

        first.invalidate();
        TowerFixtures.flyProjectilesToCompletion(this.context);
        return !beyond.hits().isEmpty();
    }

    @Test
    void aMissileHuntsAsFarRoundItselfAsItsHuntRadius() {
        boolean bare = this.huntsHalfACellPastTheBareHunt(0f);
        boolean wider = this.huntsHalfACellPastTheBareHunt(1f);

        assertThat(bare).isFalse();
        assertThat(wider).isTrue();
    }

    @Test
    void aMissileWithNothingLeftToHuntFliesHomeIntoTheNest() {
        SeekerTower tower = this.towerWith();
        tower.readyForWave();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.enemies(target);
        tower.doTick(this.tick++);
        int left = tower.nestMissiles().size();

        target.invalidate();
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.hits()).isEmpty();
        assertThat(tower.nestMissiles()).hasSize(left + 1);
    }

    @Test
    void theFreezeBeforeAReleaseShowsAsABuffAndTheCountAsTheNodesProgress() {
        SeekerTower tower = this.towerWith("Swarm");

        this.landFreezes(tower, ReleaseMechanic.EVERY - 1);

        assertThat(tower.releaseReady()).isTrue();
        assertThat(tower.conditions()).extracting(TowerBuffState::term).contains(TowerTerm.SWARM);
        assertThat(tower.progress()).contains(new NodeProgress(UpgradePaths.named(tower, "Swarm").id(),
                ReleaseMechanic.EVERY - 1, ReleaseMechanic.EVERY));
    }

    @Test
    void aWaveStartsTheReleaseCountAtNone() {
        SeekerTower tower = this.towerWith("Swarm");
        this.landFreezes(tower, ReleaseMechanic.EVERY - 1);

        tower.readyForWave();

        assertThat(tower.releaseReady()).isFalse();
    }

    @Test
    void aSwarmSendsAsManyMissilesAsTheNestHoldsAtOnceEachAtADifferentEnemySoonestOutFirst() {
        SeekerTower tower = this.towerWith("Swarm");
        int size = Math.round(tower.stats().value(TowerStat.NEST_CAPACITY));
        this.landFreezes(tower, ReleaseMechanic.EVERY);
        FakeEnemyMob[] crowd = new FakeEnemyMob[size + 1];
        for (int i = 0; i < crowd.length; i++) {
            double angle = 2 * Math.PI * i / crowd.length;
            crowd[i] = this.enemyAt(tower, Math.cos(angle) * OUT, Math.sin(angle) * OUT).withDistanceToExit(100 + i);
        }
        this.enemies(crowd);

        tower.doTick(this.tick++);
        int launched = this.context.projectiles().getProjectiles().size();
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(launched).isEqualTo(size);
        for (int i = 0; i < size; i++) {
            assertThat(crowd[i].hits()).hasSize(1);
        }
        assertThat(crowd[size].hits()).isEmpty();
    }

    @Test
    void aSwarmWithFewerEnemiesThanMissilesGoesRoundThemAgainSoALoneOneTakesEveryMissile() {
        SeekerTower tower = this.towerWith("Swarm");
        int size = Math.round(tower.stats().value(TowerStat.NEST_CAPACITY));
        this.landFreezes(tower, ReleaseMechanic.EVERY);
        FakeEnemyMob lone = this.enemyAt(tower, OUT, 0);
        this.enemies(lone);

        tower.doTick(this.tick++);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(lone.hits()).hasSize(size);
    }

    @Test
    void aHatchingHatchesAsManyMissilesAsTheNestHoldsOnTopOfTheNestWhichKeepsLoading() {
        SeekerTower tower = this.towerWith("Hatching");
        int size = Math.round(tower.stats().value(TowerStat.NEST_CAPACITY));
        this.landFreezes(tower, ReleaseMechanic.EVERY);
        this.enemies();

        for (int i = 0; i < 400; i++) {
            tower.doTick(this.tick++);
        }

        assertThat(tower.nestMissiles()).hasSize(2 * size);
    }

    /**
     * A tower in a fresh world loading as slowly as a tower can, with room for {@code room} more missiles, its nest
     * full and the load after its first launch about to start.
     */
    private SeekerTower slowTower(int room, String... nodes) {
        this.context = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
        this.tick = 1;
        SeekerTower tower = TowerFixtures.buffed(this.towerWith(nodes), this.context,
                TowerModifiers.attackSpeed(-0.75f).plus(TowerModifiers.nestCapacity(room)));
        tower.readyForWave();
        return tower;
    }

    /** About the ticks a slow tower takes to load one missile: what is left of a load just begun. */
    private int slowLoadTicks() {
        SeekerTower tower = this.slowTower(0);
        this.landFreezes(tower, 1);
        this.ticksToTheNextLoad(tower);
        this.landFreezes(tower, 1);
        return this.ticksToTheNextLoad(tower);
    }

    /** The ticks left of the load in progress once {@code freezes} freezes landed, each on a fresh enemy. */
    private int ticksLeftAfter(int freezes, String... nodes) {
        SeekerTower tower = this.slowTower(freezes, nodes);
        this.landFreezes(tower, freezes);
        return this.ticksToTheNextLoad(tower);
    }

    @Test
    void feedingBringsTheNextLoadSoonerForEveryFreezeHoweverManyFedItAlready() {
        int freezes = this.slowLoadTicks() / (2 * FeedingMechanic.FED_TICKS) + 2;
        int plain = this.ticksLeftAfter(freezes);

        int fed = this.ticksLeftAfter(freezes, "Feeding");

        assertThat(plain - fed).isEqualTo(freezes * FeedingMechanic.FED_TICKS);
    }

    @Test
    void aRubyDealsItsBonusOnTopOfTheTowersDamageAndFreezesWhatIsImmuneToFreezing() {
        SeekerTower tower = this.towerWith("Ruby");
        tower.readyForWave();
        this.spendTheFrostAhead(tower);
        FakeEnemyMob immune = this.enemyAt(tower, OUT, 0).immuneTo(EffectKind.FREEZE);
        this.enemies(immune);

        this.landOne(tower);

        assertThat(immune.onlyHitAmount()).isEqualTo(
                Math.round(tower.damageCurrent() + MissileKind.RUBY.damageBonus()));
        assertThat(immune.activeEffects()).anySatisfy(effect -> assertThat(effect.ice()).isEqualTo(IceKind.RUBY));
    }

    @Test
    void aShadeGoesPastAShieldWithItsBonusOnTopOfTheTowersDamageAndFreezes() {
        SeekerTower tower = this.towerWith("Shade");
        tower.readyForWave();
        this.spendTheFrostAhead(tower);
        FakeEnemyMob shielded = this.enemyAt(tower, OUT, 0).shieldingEveryHit();
        this.enemies(shielded);

        this.landOne(tower);

        assertThat(shielded.onlyHitAmount()).isEqualTo(
                Math.round(tower.damageCurrent() + MissileKind.SHADE.damageBonus()));
        assertThat(shielded.hasEffect(EffectKind.FREEZE)).isTrue();
    }

    @Test
    void aShadeHitStopsRegenerationForAsLongAsItsFreeze() {
        SeekerTower tower = this.towerWith("Shade");
        tower.readyForWave();
        this.spendTheFrostAhead(tower);
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.enemies(target);

        this.landOne(tower);

        assertThat(target.activeEffect(EffectKind.WITHERED)).hasValueSatisfying(wither -> assertThat(
                wither.remainingTicks()).isEqualTo(target.activeEffect(EffectKind.FREEZE).orElseThrow().remainingTicks()));
    }

    @Test
    void aStrongerMissileOnTheIceTheTowerHoldsTurnsItToItsOwnAndShattersAsItsKindAtTheThaw() {
        SeekerTower tower = this.towerWith("Shade", "Shatter");
        tower.readyForWave();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob close = this.enemyAt(tower, OUT, SCALE * 0.5).movingAt(0f);
        this.enemies(target);
        while (tower.nestMissiles().getFirst() == MissileKind.FROST) {
            this.landOne(tower);
        }
        this.landOne(tower);
        IceKind ice = target.activeEffect(EffectKind.FREEZE).orElseThrow().ice();
        this.enemies(target, close);

        target.expire(EffectKind.FREEZE);
        tower.doTick(this.tick++);

        assertThat(ice).isEqualTo(IceKind.SHADE);
        assertThat(close.onlyHitAmount()).isEqualTo(
                Math.round((tower.damageCurrent() + MissileKind.SHADE.damageBonus()) * ShatterMechanic.SHARE));
    }

    @Test
    void everyWaveStartsTheNestsOrderOver() {
        SeekerTower tower = this.towerWith("Ruby");
        tower.readyForWave();
        List<MissileKind> first = tower.nestMissiles();
        this.landFreezes(tower, 1);

        tower.readyForWave();

        assertThat(first).contains(MissileKind.RUBY);
        assertThat(tower.nestMissiles()).isEqualTo(first);
    }

    @Test
    void aMissileOfAKindNoLongerOwnedLeavesTheNest() {
        SeekerTower tower = this.towerWith("Ruby");
        tower.readyForWave();

        tower.downgrade(UpgradePaths.named(tower, "Ruby"));
        tower.doTick(this.tick++);

        assertThat(tower.nestMissiles()).isNotEmpty().containsOnly(MissileKind.FROST);
    }

    @Test
    void anIceWakeChillsTheEnemiesCloseToTheIceWhileItHolds() {
        SeekerTower tower = this.towerWith("Ice Wake");
        FakeEnemyMob frozen = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob close = this.enemyAt(tower, OUT, SCALE * 0.3).movingAt(0f);
        FakeEnemyMob far = this.enemyAt(tower, OUT, SCALE * 2).movingAt(0f);
        this.enemies(frozen, close, far);
        this.landOne(tower);

        tower.doTick(this.tick++);

        assertThat(frozen.hasEffect(EffectKind.FREEZE)).isTrue();
        assertThat(close.hasEffect(EffectKind.CHILL)).isTrue();
        assertThat(far.hasEffect(EffectKind.CHILL)).isFalse();
    }

    @Test
    void aShatterBurstsOnTheEnemiesAroundAsTheFreezeEnds() {
        SeekerTower tower = this.towerWith("Shatter");
        FakeEnemyMob frozen = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob close = this.enemyAt(tower, OUT, SCALE * 0.5).movingAt(0f);
        FakeEnemyMob far = this.enemyAt(tower, OUT, SCALE * 2).movingAt(0f);
        this.enemies(frozen, close, far);
        this.landOne(tower);
        tower.doTick(this.tick++);
        List<Integer> beforeThaw = List.of(frozen.hits().size(), close.hits().size(), far.hits().size());

        frozen.expire(EffectKind.FREEZE);
        tower.doTick(this.tick++);

        assertThat(beforeThaw).containsExactly(1, 0, 0);
        assertThat(close.onlyHitAmount()).isEqualTo(
                Math.round((tower.damageCurrent() + MissileKind.FROST.damageBonus()) * ShatterMechanic.SHARE));
        assertThat(far.hits()).isEmpty();
        assertThat(frozen.hits()).hasSize(1);
    }

    @Test
    void anIceWakeShowsRoundEachEnemyTheTowerHoldsFrozenUntilItThaws() {
        SeekerTower tower = this.towerWith("Ice Wake");
        FakeEnemyMob frozen = this.enemyAt(tower, OUT, 0);
        this.enemies(frozen);
        this.landOne(tower);
        tower.doTick(this.tick++);
        List<Vec2> held = tower.iceWakes();

        frozen.expire(EffectKind.FREEZE);
        tower.doTick(this.tick++);

        assertThat(held).containsExactly(new Vec2(frozen.getX(), frozen.getY()));
        assertThat(tower.iceWakes()).isEmpty();
    }
    @Test
    void aFrozenEnemyThatDiesShattersToo() {
        SeekerTower tower = this.towerWith("Shatter");
        FakeEnemyMob frozen = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob close = this.enemyAt(tower, OUT, SCALE * 0.5).movingAt(0f);
        this.enemies(frozen, close);
        this.landOne(tower);

        frozen.invalidate();
        tower.doTick(this.tick++);

        assertThat(close.hits()).hasSize(1);
    }
}
