package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.ui.TowerSpriteFrameBuilder;
import td.ui.render.TowerSpriteDraw;
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
    private static final int SPUN_UP_REVOLUTION = Math.round(2f * TickRate.TICKS_PER_SECOND);
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
    private static final String[] TRANSCENDED = {"Rapid Array", "Rapid Array II", "Spin-Up", "Mark on Sweep",
        "Transcendent"};

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

    /** A Sonar past Transcendent on the Rapid Array chain, with {@code more} bought after. */
    private SonarTower transcendedTower(String... more) {
        String[] path = new String[TRANSCENDED.length + more.length];
        System.arraycopy(TRANSCENDED, 0, path, 0, TRANSCENDED.length);
        System.arraycopy(more, 0, path, TRANSCENDED.length, more.length);
        return this.upgradedTower(path);
    }

    private SonarTower upgradedTower(String... nodes) {
        this.context.playtestRules().setUpgradeGatesIgnored(true);
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
    void itsCadenceIsARotationThatSpinUpShortens() {
        SonarTower plain = tower();
        SonarTower spunUp = this.upgradedTower("Rapid Array", "Rapid Array II", "Spin-Up");

        assertThat(plain.inspect().stats()).extracting(TowerStatLine::stat)
                .contains(TowerStat.ROTATION).doesNotContain(TowerStat.FIRE_RATE);
        assertThat(plain.cadence()).map(TowerStatLine::current).contains(3f);
        assertThat(spunUp.cadence()).map(TowerStatLine::current).contains(2f);
    }

    @Test
    void spinUpCarriesTheBeamOnFromWhereItWas() {
        SonarTower tower = this.upgradedTower("Rapid Array", "Rapid Array II");
        this.enemies();
        run(tower, QUARTER_TURN);
        double before = tower.sweepRadiansAt(1);

        tower.buyUpgrade(UpgradePaths.named(tower, "Spin-Up"));
        tower.doTick(QUARTER_TURN + 1);

        assertThat(before - tower.sweepRadiansAt(0)).isCloseTo(0, within(1e-9));
        assertThat(tower.sweepRadiansAt(0) - tower.sweepRadiansAt(1))
                .isCloseTo(Math.PI * 2 / SPUN_UP_REVOLUTION, within(1e-9));
    }

    @Test
    void aPingExposesTheHealthiestEnemyTheBeamPassedUntilTheNextPassAndCountsADeed() {
        SonarTower tower = this.upgradedTower();
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
        SonarTower tower = this.upgradedTower();
        this.enemies();

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(tower.experience().deeds()).isZero();
    }

    @Test
    void rapidArrayPingsTheTwoHealthiest() {
        SonarTower tower = this.upgradedTower("Rapid Array");
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
        SonarTower tower = this.upgradedTower("Rapid Array", "Rapid Array II");
        FakeEnemyMob exposed = at(EAST, NEAR);
        exposed.applyEffect(Effect.exposed(5, d -> {
        }));
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
    void wideBandHitsAHiddenEnemyRevealsItAndExposesItForASecond() {
        this.context.towers().add(new SniperTower(this.context, 3, 3));
        this.context.towers().add(new SniperTower(this.context, 3, 3));
        SonarTower tower = this.upgradedTower("Wide Band");
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(TOWER_X + Math.cos(EAST) * NEAR, TOWER_Y + Math.sin(EAST) * NEAR);
        this.enemies(ghost);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(hitCount(ghost)).isEqualTo(1);
        assertThat(effectsOf(ghost, EffectKind.REVEALED)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(Math.round(3 * TickRate.TICKS_PER_SECOND));
        assertThat(effectsOf(ghost, EffectKind.EXPOSED)).first()
                .extracting(Effect::remainingTicks).isEqualTo(Math.round(TickRate.TICKS_PER_SECOND));
    }

    @Test
    void deepScanRevealsHiddenEnemiesInTheOuterQuarterOnlyOncePerRevolution() {
        SonarTower tower = this.upgradedTower("Range", "Range II", "Rapid Array", "Rapid Array II", "Spin-Up",
                "Mark on Sweep", "Transcendent", "Range III");
        float range = tower.getRangeReal();
        FakeEnemyMob outer = FakeEnemyMob.ghostAt(TOWER_X + range * 0.9f, TOWER_Y);
        FakeEnemyMob inner = FakeEnemyMob.ghostAt(TOWER_X + range * 0.5f, TOWER_Y);
        this.enemies(outer, inner);

        run(tower, SPUN_UP_REVOLUTION);

        assertThat(effectsOf(outer, EffectKind.REVEALED)).singleElement()
                .extracting(Effect::remainingTicks).isEqualTo(Math.round(TickRate.TICKS_PER_SECOND));
        assertThat(effectsOf(inner, EffectKind.REVEALED)).isEmpty();
    }

    @Test
    void twinBeamSweepsASecondBeamOppositeAtSeventyPercent() {
        SonarTower twin = this.transcendedTower("Twin Beam");
        FakeEnemyMob enemy = at(EAST, NEAR);
        this.enemies(enemy);

        run(twin, SPUN_UP_REVOLUTION);

        int weapon = twin.damageCurrent();
        assertThat(enemy.hits()).extracting(Damage::amount).containsExactly(weapon, Math.round(weapon * 0.7f));
        assertThat(twin.hasTwinBeam()).isTrue();
    }

    @Test
    void phasedArrayHoldsOnTheHealthiestAndHitsItThreeTimesARevolution() {
        SonarTower tower = this.transcendedTower("Phased Array");
        FakeEnemyMob strong = at(SOUTH, NEAR).withHealth(900);
        FakeEnemyMob weak = at(WEST, NEAR).withHealth(100);
        this.enemies(strong, weak);

        run(tower, SPUN_UP_REVOLUTION);

        assertThat(hitCount(strong)).isEqualTo(3);
        assertThat(weak.hits()).isEmpty();
    }

    @Test
    void longReachTwoScalesADistantHitUpToDoubleAndLeavesAPointBlankHitAlone() {
        SonarTower tower = this.upgradedTower("Long Reach", "Long Reach II");
        float range = tower.getRangeReal();
        FakeEnemyMob close = at(EAST, 1);
        FakeEnemyMob far = at(NORTH, range - 1);
        this.enemies(close, far);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(close.onlyHitAmount()).isCloseTo(tower.damageCurrent(), within(tower.damageCurrent() / 50));
        assertThat(far.onlyHitAmount()).isCloseTo(2 * tower.damageCurrent(), within(tower.damageCurrent() / 20));
    }

    @Test
    void horizonRaisesTheFarBonusButDealsNothingWithinOneAndAHalfCells() {
        SonarTower tower = this.upgradedTower("Long Reach", "Long Reach II", "Resonant Crack", "Mark on Sweep", "Transcendent", "Horizon");
        float range = tower.getRangeReal();
        FakeEnemyMob close = at(EAST, BoardFixtures.SCALE);
        FakeEnemyMob far = at(NORTH, range - 1);
        this.enemies(close, far);

        run(tower, TICKS_PER_REVOLUTION);

        assertThat(close.hits()).isEmpty();
        assertThat(far.onlyHitAmount()).isCloseTo(Math.round(2.5f * tower.damageCurrent()),
                within(tower.damageCurrent() / 20));
    }

    @Test
    void resonantCrackFracturesEveryHitAndFaultLineDeepensIt() {
        SonarTower crack = this.upgradedTower("Long Reach", "Long Reach II", "Resonant Crack");
        FakeEnemyMob cracked = at(EAST, NEAR);
        this.enemies(cracked);
        crack.doTick(1);
        SonarTower faultLine = this.upgradedTower("Long Reach", "Long Reach II", "Resonant Crack", "Mark on Sweep", "Transcendent",
                "Fault Line");
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
    void harmonicsMakesEveryHitApplyResonating() {
        SonarTower tower = this.upgradedTower("Ultrasound", "Harmonics");
        FakeEnemyMob enemy = at(EAST, NEAR);
        this.enemies(enemy);

        tower.doTick(1);

        assertThat(effectsOf(enemy, EffectKind.RESONATING)).singleElement().extracting(Effect::stacks).isEqualTo(1);
    }

    @Test
    void pureToneTurnsTheBeamToMagicWithPenetrationAndNoAddedShare() {
        SonarTower tower = this.upgradedTower("Ultrasound", "Harmonics", "Pure Tone");
        FakeEnemyMob enemy = at(EAST, NEAR);
        this.enemies(enemy);

        tower.doTick(1);

        assertThat(enemy.hits()).extracting(Damage::type).containsExactly(DamageType.MAGIC);
        assertThat(enemy.attackers().getFirst().magicPenetration()).isCloseTo(0.15f, within(1e-6f));
    }

    @Test
    void shatterToneBreaksAQuarterOfAShieldAndLeavesTheUnshieldedAlone() {
        SonarTower tower = this.transcendedTower("Ultrasound", "Harmonics", "Pure Tone", "Shatter Tone");
        FakeEnemyMob shielded = at(EAST, NEAR);
        shielded.applyEffect(Effect.shield(0.5f, 100, d -> {
        }));
        FakeEnemyMob bare = at(EAST, NEAR + 30);
        this.enemies(shielded, bare);

        tower.doTick(1);

        assertThat(shielded.shieldBreaks()).containsExactly(0.25f);
        assertThat(bare.shieldBreaks()).isEmpty();
    }

    @Test
    void aSonarWithItsLevelThreeAndASpecialTranscendsAndPicksASecondSpecial() {
        SonarTower tower = this.upgradedTower("Rapid Array", "Rapid Array II", "Spin-Up", "Mark on Sweep",
                "Transcendent", "Command Ping");

        assertThat(tower.upgrades().inSlot(UpgradeSlot.SPECIAL)).extracting(UpgradeNode::displayName)
                .containsExactly("Mark on Sweep", "Command Ping");
    }

    @Test
    void onlyATranscendedSonarWearsTheHaloOnTheBoard() {
        SonarTower transcended = this.upgradedTower("Rapid Array", "Rapid Array II", "Spin-Up", "Mark on Sweep",
                "Transcendent");
        SonarTower awakened = this.upgradedTower("Rapid Array", "Rapid Array II", "Spin-Up", "Mark on Sweep");
        TowerSpriteFrameBuilder sprites = new TowerSpriteFrameBuilder(this.context, 0, 0.0, 0.0);

        transcended.accept(sprites);
        awakened.accept(sprites);

        assertThat(sprites.build()).extracting(TowerSpriteDraw::transcendent).containsExactly(true, false);
    }
}
