package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.FakeTower;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeTree;
import td.ui.TowerSpriteFrameBuilder;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TurretHeadDraw;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Scale 32. The scan starts pointing east and turns counterclockwise. */
class SonarTowerTest {

    private static final int TICKS_PER_REVOLUTION = Math.round(SonarTower.SECONDS_PER_REVOLUTION
            * TickRate.TICKS_PER_SECOND);
    private static final int QUARTER_TURN = TICKS_PER_REVOLUTION / 4;
    // cell (3,3) centre, in board pixels
    private static final int TOWER_X = 3 * BoardFixtures.SCALE + BoardFixtures.SCALE / 2;
    private static final int TOWER_Y = 3 * BoardFixtures.SCALE + BoardFixtures.SCALE / 2;
    private static final int NEAR = 50;
    /**
     * Bearings just past each quarter of the turn, counterclockwise: an enemy exactly on a tick's
     * edge would be up to floating point, so none sits there.
     */
    private static final double JUST_PAST = 0.04;
    private static final double EAST = -JUST_PAST;
    private static final double NORTH = -Math.PI / 2 - JUST_PAST;
    private static final double WEST = -Math.PI - JUST_PAST;
    private static final double SOUTH = -3 * Math.PI / 2 - JUST_PAST;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private static FakeEnemyMob at(double bearing, double distance) {
        return FakeEnemyMob.at(TOWER_X + Math.cos(bearing) * distance, TOWER_Y + Math.sin(bearing) * distance);
    }

    private static int hitCount(FakeEnemyMob mob) {
        return mob.hits().size();
    }

    private static List<Effect> effectsOf(FakeEnemyMob mob, EffectKind kind) {
        return mob.appliedEffects().stream().filter(effect -> effect.kind() == kind).toList();
    }

    private SonarTower tower() {
        return new SonarTower(this.context, 3, 3);
    }

    /** The ticks one turn of {@code tower}'s beam takes, at its own rotation. */
    private static int revolutionOf(SonarTower tower) {
        return Math.round(tower.cadence().orElseThrow().current() * TickRate.TICKS_PER_SECOND);
    }

    private SonarTower upgradedTower(String... nodes) {
        SonarTower tower = tower();
        this.context.towers().add(tower);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private void enemies(EnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    private static void run(SonarTower tower, int ticks) {
        for (int tick = 1; tick <= ticks; tick++) {
            tower.doTick(tick);
        }
    }

    @Test
    void enemiesAreSweptInCounterclockwiseOrderOfTheirBearingNotOfTheirWaveOrder() {
        SonarTower tower = tower();
        FakeEnemyMob east = at(EAST, NEAR);
        FakeEnemyMob north = at(NORTH, NEAR);
        FakeEnemyMob west = at(WEST, NEAR);
        FakeEnemyMob south = at(SOUTH, NEAR);
        // deliberately not in sweep order: where they sit decides, not where they are in the array
        this.enemies(south, west, north, east);

        Map<FakeEnemyMob, Integer> firstHit = new LinkedHashMap<>();
        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
            for (FakeEnemyMob mob : List.of(east, north, west, south)) {
                if (!mob.hits().isEmpty()) {
                    firstHit.putIfAbsent(mob, tick);
                }
            }
        }

        assertThat(firstHit.get(east)).isEqualTo(1);
        assertThat(firstHit.get(north)).isEqualTo(1 + QUARTER_TURN);
        assertThat(firstHit.get(west)).isEqualTo(1 + 2 * QUARTER_TURN);
        assertThat(firstHit.get(south)).isEqualTo(1 + 3 * QUARTER_TURN);
    }

    @Test
    void aStationaryEnemyIsHitExactlyOncePerRevolution() {
        SonarTower tower = tower();
        FakeEnemyMob enemy = at(NORTH, NEAR);
        this.enemies(enemy);

        run(tower, TICKS_PER_REVOLUTION * 3);

        assertThat(hitCount(enemy)).isEqualTo(3);
    }

    @Test
    void everyEnemyOnTheSameBearingIsHitOnTheSameTick() {
        SonarTower tower = tower();
        FakeEnemyMob close = at(NORTH, 40);
        FakeEnemyMob far = at(NORTH, 80);
        this.enemies(close, far);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(hitCount(close)).isEqualTo(1);
        assertThat(hitCount(far)).isEqualTo(1);
    }

    @Test
    void anEnemyBeyondTheTowersRangeIsNeverHit() {
        SonarTower tower = tower();
        FakeEnemyMob outOfRange = at(EAST, tower.getRangeReal() + 10);
        this.enemies(outOfRange);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(outOfRange.hits()).isEmpty();
    }

    @Test
    void withoutWideBandTheBeamNeverTouchesAHiddenEnemy() {
        SonarTower tower = tower();
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(TOWER_X + Math.cos(EAST) * NEAR, TOWER_Y + Math.sin(EAST) * NEAR);
        this.enemies(ghost);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(ghost.hits()).isEmpty();
    }

    @Test
    void anEnemyThatTheBeamWouldHaveJumpedOverBetweenTicksIsStillHit() {
        SonarTower tower = tower();
        // off any exact tick boundary, so "is the beam pointing at it right now" would step past it
        double awkward = -(Math.PI * 2 / TICKS_PER_REVOLUTION) * 4.37;
        FakeEnemyMob enemy = FakeEnemyMob.at(TOWER_X + Math.cos(awkward) * NEAR, TOWER_Y + Math.sin(awkward) * NEAR);
        this.enemies(enemy);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(hitCount(enemy)).isEqualTo(1);
    }

    @Test
    void theTurretHeadPointsWhereTheBeamIs() {
        SonarTower tower = tower();
        this.enemies();

        tower.doTick(1);

        // counterclockwise on screen is a decreasing angle, one revolution's share per tick
        assertThat(tower.sweepRadiansAt(0) - tower.sweepRadiansAt(1))
                .isCloseTo(Math.PI * 2 / TICKS_PER_REVOLUTION, within(1e-9));
    }

    @Test
    void itsCadenceIsARotationRatherThanAFireRate() {
        SonarTower plain = tower();

        assertThat(plain.inspect().stats()).extracting(TowerStatLine::stat)
                .contains(TowerStatRow.ROTATION).doesNotContain(TowerStatRow.FIRE_RATE);
        assertThat(plain.cadence()).map(TowerStatLine::current).contains(SonarTower.SECONDS_PER_REVOLUTION);
    }

    @Test
    void aPingExposesTheHealthiestEnemyTheBeamPassedUntilTheNextPassAndCountsADeed() {
        SonarTower tower = this.upgradedTower("Attune");
        FakeEnemyMob weak = at(EAST, NEAR).withHealth(100);
        FakeEnemyMob strong = at(WEST, NEAR).withHealth(900);
        this.enemies(weak, strong);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(effectsOf(weak, EffectKind.EXPOSED)).isEmpty();
        assertThat(effectsOf(strong, EffectKind.EXPOSED)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(TICKS_PER_REVOLUTION + 2);
        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    @Test
    void aRevolutionThatPassesNobodyPingsNobodyAndCountsNoDeed() {
        SonarTower tower = this.upgradedTower("Attune");
        this.enemies();

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(tower.experience().deeds()).isZero();
    }

    @Test
    void anExtraPingExposesTheNextHealthiestToo() {
        SonarTower tower = this.upgradedTower("Attune");
        this.context.towers().add(FakeTower.offering(this.context, 3, 4, UpgradeTree.none())
                .giving(TowerModifiers.pingCount(1)));
        tower.recalculateStats();
        FakeEnemyMob weak = at(EAST, NEAR).withHealth(100);
        FakeEnemyMob middle = at(NORTH, NEAR).withHealth(500);
        FakeEnemyMob strong = at(WEST, NEAR).withHealth(900);
        this.enemies(weak, middle, strong);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(effectsOf(weak, EffectKind.EXPOSED)).isEmpty();
        assertThat(effectsOf(middle, EffectKind.EXPOSED)).hasSize(1);
        assertThat(effectsOf(strong, EffectKind.EXPOSED)).hasSize(1);
    }

    @Test
    void longReachPingsTheHealthiestPastHalfRangeForTwoPasses() {
        SonarTower tower = this.upgradedTower("Long Reach");
        float range = tower.getRangeReal();
        FakeEnemyMob closeAndStrong = at(EAST, range * 0.3f).withHealth(900);
        FakeEnemyMob farAndWeak = at(WEST, range * 0.8f).withHealth(100);
        this.enemies(closeAndStrong, farAndWeak);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(effectsOf(closeAndStrong, EffectKind.EXPOSED)).isEmpty();
        assertThat(effectsOf(farAndWeak, EffectKind.EXPOSED)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(2 * TICKS_PER_REVOLUTION + 2);
    }

    @Test
    void aBeamCritOnAnExposedEnemyRefreshesItsExposure() {
        SonarTower tower = this.upgradedTower("Rapid Array II");
        FakeEnemyMob exposed = at(EAST, NEAR);
        exposed.applyEffect(Effect.exposed(5, DamageSink.none()));
        exposed.landEveryHitCritical();
        this.enemies(exposed);

        tower.doTick(1);

        assertThat(effectsOf(exposed, EffectKind.EXPOSED)).extracting(Effect::remainingTicks)
                .containsExactly(5, TICKS_PER_REVOLUTION + 2);
    }

    @Test
    void commandPingMakesThePingedEnemyThePriorityUntilTheNextPass() {
        SonarTower tower = this.upgradedTower("Command Ping");
        FakeEnemyMob strong = at(WEST, NEAR).withHealth(900);
        FakeEnemyMob weak = at(EAST, NEAR).withHealth(100);
        this.enemies(strong, weak);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(effectsOf(strong, EffectKind.PRIORITY)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(TICKS_PER_REVOLUTION + 2);
        assertThat(effectsOf(weak, EffectKind.PRIORITY)).isEmpty();
    }

    @Test
    void markOnSweepMarksEveryEnemyTheBeamHitsUntilTheNextPass() {
        SonarTower tower = this.upgradedTower("Mark on Sweep");
        FakeEnemyMob east = at(EAST, NEAR);
        FakeEnemyMob west = at(WEST, NEAR);
        this.enemies(east, west);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(effectsOf(east, EffectKind.MARKED)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(TICKS_PER_REVOLUTION + 2);
        assertThat(effectsOf(west, EffectKind.MARKED)).hasSize(1);
    }

    @Test
    void wideBandHitsAHiddenEnemyRevealsItAndExposesIt() {
        this.context.towers().add(new SniperTower(this.context, 3, 3));
        this.context.towers().add(new SniperTower(this.context, 3, 3));
        SonarTower tower = this.upgradedTower("Wide Band");
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(TOWER_X + Math.cos(EAST) * NEAR, TOWER_Y + Math.sin(EAST) * NEAR);
        this.enemies(ghost);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(hitCount(ghost)).isEqualTo(1);
        assertThat(effectsOf(ghost, EffectKind.REVEALED)).hasSize(1);
        assertThat(effectsOf(ghost, EffectKind.EXPOSED)).isNotEmpty();
    }

    @Test
    void deepScanRevealsHiddenEnemiesInTheOuterQuarterOnlyOncePerRevolution() {
        SonarTower tower = this.upgradedTower("Range III");
        float range = tower.getRangeReal();
        FakeEnemyMob outer = FakeEnemyMob.ghostAt(TOWER_X + range * 0.9f, TOWER_Y);
        FakeEnemyMob inner = FakeEnemyMob.ghostAt(TOWER_X + range * 0.5f, TOWER_Y);
        this.enemies(outer, inner);

        run(tower, revolutionOf(tower));

        assertThat(effectsOf(outer, EffectKind.REVEALED)).hasSize(1);
        assertThat(effectsOf(inner, EffectKind.REVEALED)).isEmpty();
    }

    @Test
    void twinBeamSweepsASecondBeamOppositeAtItsShareOfTheDamage() {
        SonarTower twin = this.upgradedTower("Twin Beam");
        FakeEnemyMob enemy = at(EAST, NEAR);
        this.enemies(enemy);

        run(twin, revolutionOf(twin));

        int weapon = twin.damageCurrent();
        assertThat(twin.twinBeamShare()).isBetween(0f, 1f);
        assertThat(enemy.hits()).extracting(Damage::amount)
                .containsExactly(weapon, Math.round(weapon * twin.twinBeamShare()));
    }

    @Test
    void phasedArrayHoldsOnTheHealthiestAndHitsItThreeTimesARevolution() {
        SonarTower tower = this.upgradedTower("Phased Array");
        FakeEnemyMob strong = at(SOUTH, NEAR).withHealth(900);
        FakeEnemyMob weak = at(WEST, NEAR).withHealth(100);
        this.enemies(strong, weak);

        run(tower, revolutionOf(tower));

        assertThat(hitCount(strong)).isEqualTo(3);
        assertThat(weak.hits()).isEmpty();
    }

    @Test
    void resonantCrackFracturesEveryHitAndFaultLineDeepensIt() {
        SonarTower crack = this.upgradedTower("Resonant Crack");
        FakeEnemyMob cracked = at(EAST, NEAR);
        this.enemies(cracked);
        crack.doTick(1);
        SonarTower faultLine = this.upgradedTower("Fault Line");
        FakeEnemyMob faulted = at(EAST, NEAR);
        this.enemies(faulted);

        faultLine.doTick(1);

        assertThat(effectsOf(cracked, EffectKind.FRACTURED)).singleElement().extracting(Effect::faultLine)
                .isEqualTo(false);
        assertThat(effectsOf(faulted, EffectKind.FRACTURED)).singleElement().extracting(Effect::faultLine)
                .isEqualTo(true);
    }

    @Test
    void ultrasoundAddsAFifthAsMagicAndUpToHalfAgainstAnArmoredEnemy() {
        SonarTower tower = this.upgradedTower("Ultrasound");
        FakeEnemyMob plain = at(EAST, NEAR);
        FakeEnemyMob armored = at(EAST, NEAR + 30);
        armored.reportPhysicalReduction(0.9f);
        this.enemies(plain, armored);

        tower.doTick(1);

        int weapon = tower.damageCurrent();
        assertThat(plain.hits()).containsExactly(Damage.physical(weapon), Damage.magic(Math.round(weapon * 0.2f)));
        assertThat(armored.hits()).containsExactly(Damage.physical(weapon), Damage.magic(Math.round(weapon * 0.5f)));
    }

    @Test
    void ultrasoundsMagicNeverRollsACritOfItsOwnButGrowsWithTheHitsCrit() {
        SonarTower tower = this.upgradedTower("Ultrasound");
        FakeEnemyMob enemy = at(EAST, NEAR);
        enemy.landEveryHitCritical();
        this.enemies(enemy);

        tower.doTick(1);

        float critFactor = tower.stats().attack().critMultiplier();
        assertThat(enemy.attackers()).extracting(attack -> attack.delivery().name()).containsExactly("HIT", "PERIODIC");
        assertThat(enemy.hits().get(1).amount()).isEqualTo(Math.round(tower.damageCurrent() * 0.2f * critFactor));
    }

    @Test
    void twinBeamsSecondHeadIsDrawnOppositeAtItsShareOfTheDamage() {
        SonarTower twin = this.upgradedTower("Twin Beam");
        this.enemies();
        twin.doTick(1);
        TowerSpriteFrameBuilder sprites = new TowerSpriteFrameBuilder(this.context, 0, 0.0, 0.0);

        twin.accept(sprites);

        assertThat(sprites.buildHeads()).extracting(TurretHeadDraw::scale).containsExactly(1f, twin.twinBeamShare());
    }

    @Test
    void onlyATranscendedSonarWearsTheHaloOnTheBoard() {
        SonarTower transcended = this.upgradedTower("Transcendent");
        SonarTower awakened = this.upgradedTower("Awaken");
        TowerSpriteFrameBuilder sprites = new TowerSpriteFrameBuilder(this.context, 0, 0.0, 0.0);

        transcended.accept(sprites);
        awakened.accept(sprites);

        assertThat(sprites.build()).extracting(TowerSpriteDraw::transcendent).containsExactly(true, false);
    }
}
