package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.ui.TowerSpriteFrameBuilder;
import td.ui.render.Palette;
import td.ui.render.TurretHeadDraw;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SniperTowerTest {

    private static final float STEADY_AIM_CRIT_BONUS = 0.1f;

    private final GameWorld context = WorldFixtures.newWorld();
    private int clock;

    private static GameWorld boardWorld() {
        return WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
    }

    private static SniperTower awakenedSniper(GameWorld world, int killsEarned) {
        world.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(world, 3, 3);
        UpgradePaths.awakenVeteran(tower);
        for (int i = 0; i < killsEarned; i++) {
            tower.dealDamage(EnemyFactory.getEnemy("c", world, 0, 1, 1, Rank.GRUNT), Damage.physical(1_000_000));
        }
        return tower;
    }

    /** A Sniper with {@code names} bought, every XP and purpose gate waived. */
    private static SniperTower sniperWith(GameWorld world, String... names) {
        world.playtestRules().setUpgradeGatesIgnored(true);
        SniperTower tower = new SniperTower(world, 3, 3);
        UpgradePaths.buy(tower, world, names);
        return tower;
    }

    private static FakeEnemyMob targetFor(GameWorld world) {
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        world.enemies().setEnemies(new EnemyMob[]{target});
        return target;
    }

    /** One tick of the Sniper's turn; the tick it was. */
    private int tick(SniperTower tower) {
        this.clock++;
        tower.beginTick(this.clock);
        tower.doTick(this.clock);
        return this.clock;
    }

    /** The ticks the Sniper fires at {@code target} on, for its next {@code shots} shots. */
    private List<Integer> shotTicks(SniperTower tower, FakeEnemyMob target, int shots) {
        List<Integer> ticks = new ArrayList<>();
        while (ticks.size() < shots) {
            int before = target.hits().size();
            int tick = this.tick(tower);
            if (target.hits().size() > before) {
                ticks.add(tick);
            }
        }
        return ticks;
    }

    private static List<Integer> gaps(List<Integer> ticks) {
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < ticks.size(); i++) {
            gaps.add(ticks.get(i) - ticks.get(i - 1));
        }
        return gaps;
    }

    private void fireShots(SniperTower tower, FakeEnemyMob target, int shots) {
        this.shotTicks(tower, target, shots);
    }

    private static List<Float> critChances(FakeEnemyMob target) {
        return target.attackers().stream().map(AttackProfile::critChance).toList();
    }

    @Test
    void theAssassinStartsWithTheStatsOfItsDesign() {
        SniperTower tower = new SniperTower(boardWorld(), 3, 3);

        assertThat(SniperTower.PRICE).isEqualTo(15);
        assertThat(tower.damageCurrent()).isEqualTo(DamageUnits.ofPoints(40f));
        assertThat(tower.getRange()).isEqualTo(4.0f);
        assertThat(tower.coolDownMax).isEqualTo(49);
        assertThat(tower.fireRateCurrent()).isEqualTo(1.0);
        assertThat(tower.critChance()).isEqualTo(0.05f);
        assertThat(tower.stats().attack().critMultiplier()).isEqualTo(2.0f);
    }

    @Test
    void focusedOpticsTwoAddsFortyPercentDamage() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradePaths.awakenVeteran(tower);
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));

        boolean bought = tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics II"));

        assertThat(bought).isTrue();
        assertThat(tower.damageCurrent()).isEqualTo(Math.round(tower.damageBase * 1.4f));
    }

    @Test
    void marksmansEyeTwoWaitsForFiftyXpFromTheGateTable() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Attune"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));
        tower.earnXp(49);
        boolean atFortyNine = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye II"));
        tower.earnXp(1);

        boolean atFifty = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye II"));

        assertThat(atFortyNine).isFalse();
        assertThat(atFifty).isTrue();
    }

    @Test
    void marksmansEyeLetsSteadyAimBuildToThreeStacks() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Marksman's Eye");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 6);

        assertThat(critChances(target).get(4)).isCloseTo(SniperTower.CRIT_CHANCE + 3 * STEADY_AIM_CRIT_BONUS, within(1e-6f));
        assertThat(critChances(target).get(5)).isCloseTo(SniperTower.CRIT_CHANCE + 3 * STEADY_AIM_CRIT_BONUS, within(1e-6f));
    }

    @Test
    void marksmansEyeTwoShootsFasterAndIgnoresThirtyArmor() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Marksman's Eye", "Marksman's Eye II");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 1);

        assertThat(tower.fireRateCurrent()).isCloseTo(1.0 / 0.75, within(1e-9));
        assertThat(target.attackers().getFirst().armorPenetrationFlat()).isEqualTo(30f);
    }

    @Test
    void cleanShotMakesTheShotsCritsPierceShields() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Marksman's Eye", "Marksman's Eye II", "Marksman's Eye III");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 1);

        assertThat(target.attackers().getFirst().critsPierceShields()).isTrue();
    }

    @Test
    void unbrokenAimBuildsToFiveStacksAndSurvivesAKill() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Marksman's Eye", "Marksman's Eye II", "Marksman's Eye III",
                "Fifth Shot", "Transcendent", "Unbroken Aim");
        FakeEnemyMob first = targetFor(world);
        this.fireShots(tower, first, 5);
        first.invalidate();
        FakeEnemyMob next = targetFor(world);

        this.fireShots(tower, next, 1);

        assertThat(critChances(next).getFirst()).isCloseTo(SniperTower.CRIT_CHANCE + 5 * STEADY_AIM_CRIT_BONUS, within(1e-6f));
    }

    @Test
    void sunderRoundsSundersAnEnemyOnlyWhenTheShotCrits() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Marksman's Eye", "Marksman's Eye II", "Marksman's Eye III",
                "Fifth Shot", "Transcendent", "Sunder Rounds");
        FakeEnemyMob target = targetFor(world);
        this.fireShots(tower, target, 1);
        assertThat(target.appliedEffects()).isEmpty();

        target.landEveryHitCritical();
        this.fireShots(tower, target, 1);

        assertThat(target.appliedEffects()).extracting(Effect::kind).containsExactly(EffectKind.SUNDERED);
    }

    @Test
    void buyingAnyHeadRootForeclosesTheOtherHeadRootForever() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(this.context, 0, 0);
        UpgradePaths.awakenVeteran(tower);
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));

        boolean chosenMarksmansEye = tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));

        assertThat(chosenMarksmansEye).isFalse();
    }

    @Test
    void doTickRecordsWhetherTheShotThatJustFiredWasACriticalHit() {
        GameWorld alwaysCrits = new GameWorld(new RecordingGameHost(), () -> 0.0);
        alwaysCrits.setBoard(BoardGeometry.of(BoardFixtures.SCALE, 20, 20));
        SniperTower tower = new SniperTower(alwaysCrits, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        target.landEveryHitCritical();
        alwaysCrits.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(tower.wasLastShotCritical()).isTrue();
        assertThat(target.attackers().getFirst().critChance()).isEqualTo(tower.critChance());
    }

    @Test
    void steadyAimAddsCritChanceToEveryShotAfterTheFirstAtOneTarget() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 0);
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 3);

        assertThat(critChances(target)).hasSize(3);
        assertThat(critChances(target).get(0)).isEqualTo(SniperTower.CRIT_CHANCE);
        assertThat(critChances(target).get(1)).isCloseTo(SniperTower.CRIT_CHANCE + STEADY_AIM_CRIT_BONUS, within(1e-6f));
        assertThat(critChances(target).get(2)).isCloseTo(SniperTower.CRIT_CHANCE + STEADY_AIM_CRIT_BONUS, within(1e-6f));
    }

    @Test
    void steadyAimWaitsForAttune() {
        GameWorld world = boardWorld();
        world.economy().startEconomy(1000, 5);
        SniperTower tower = new SniperTower(world, 3, 3);
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 2);

        assertThat(critChances(target)).containsOnly(SniperTower.CRIT_CHANCE);
    }

    @Test
    void aNewTargetStartsSteadyAimOver() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 0);
        FakeEnemyMob first = targetFor(world);
        this.fireShots(tower, first, 2);
        first.invalidate();
        FakeEnemyMob second = targetFor(world);

        this.fireShots(tower, second, 1);

        assertThat(critChances(second)).containsExactly(SniperTower.CRIT_CHANCE);
    }

    @Test
    void steadyAimShotsCountAsTheDeedOfTheTowerOncePerShot() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 0);
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 4);

        assertThat(tower.experience().deeds()).isEqualTo(3);
    }

    @Test
    void focusedOpticsThreeWaitsForTwentyShotsUnderSteadyAim() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 0);
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics II"));
        UpgradeNode three = UpgradePaths.named(tower, "Focused Optics III");
        FakeEnemyMob target = targetFor(world);
        this.fireShots(tower, target, 20);
        boolean atNineteen = tower.buyUpgrade(three);

        this.fireShots(tower, target, 1);
        boolean atTwenty = tower.buyUpgrade(three);

        assertThat(atNineteen).isFalse();
        assertThat(atTwenty).isTrue();
    }

    @Test
    void quickScopeHelpsTheFirstShotAndSteadyAimThenSpeedsUpTheWaitBetweenShots() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics");
        FakeEnemyMob target = targetFor(world);

        List<Integer> ticks = this.shotTicks(tower, target, 3);

        assertThat(critChances(target).get(0)).isCloseTo(SniperTower.CRIT_CHANCE + 0.5f, within(1e-6f));
        assertThat(critChances(target).get(1)).isCloseTo(SniperTower.CRIT_CHANCE + STEADY_AIM_CRIT_BONUS, within(1e-6f));
        assertThat(gaps(ticks).get(0)).isEqualTo(waitTicks(tower, 1.0));
        assertThat(gaps(ticks).get(1)).isLessThan(waitTicks(tower, 1.0));
    }

    /** How many ticks a wait of {@code periods} base periods takes at the tower's fire rate. */
    private static int waitTicks(SniperTower tower, double periods) {
        return (int) Math.round((tower.coolDownMax + 1) * periods / tower.fireRateCurrent());
    }

    @Test
    void aCritStartsAFrenzyThatMakesTheNextShotsComeFasterThanSteadyAimAlone() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II");
        FakeEnemyMob steadyOnly = targetFor(world);
        List<Integer> withoutCrits = this.shotTicks(tower, steadyOnly, 3);
        GameWorld critWorld = boardWorld();
        SniperTower frenzied = sniperWith(critWorld, "Focused Optics", "Focused Optics II");
        FakeEnemyMob critical = targetFor(critWorld);
        critical.landEveryHitCritical();

        List<Integer> withCrits = this.shotTicks(frenzied, critical, 3);

        assertThat(gaps(withCrits).get(1)).isLessThan(gaps(withoutCrits).get(1));
    }

    @Test
    void weakSpotMakesAShotThatDoesNotCritIgnoreFiftyArmorAndAllPlating() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 1);

        AttackProfile attack = target.attackers().getFirst();
        assertThat(attack.penetrate(DamageType.PHYSICAL, 80f, false)).isEqualTo(30f);
        assertThat(attack.penetratePlating(500f, false)).isZero();
        assertThat(attack.penetratePlating(500f, true)).isEqualTo(500f);
    }

    @Test
    void overwatchStopsTheTowerShootingWhatIsWithinTwoCellsHoweverFarItsRangeGrows() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III", "Momentum",
                "Transcendent", "Range", "Range II", "Range III");
        FakeEnemyMob close = FakeEnemyMob.at(tower.getX() + 40, tower.getY()).withProgression(50);
        FakeEnemyMob distant = FakeEnemyMob.at(tower.getX() + 100, tower.getY()).withProgression(1);
        world.enemies().setEnemies(new EnemyMob[]{close, distant});

        tower.doTick(1);

        assertThat(close.hits()).isEmpty();
        assertThat(distant.hits()).hasSize(1);
    }

    @Test
    void withoutOverwatchTheTowerShootsWhatIsClose() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Range");
        FakeEnemyMob close = FakeEnemyMob.at(tower.getX() + 40, tower.getY()).withProgression(50);
        world.enemies().setEnemies(new EnemyMob[]{close});

        tower.doTick(1);

        assertThat(close.hits()).hasSize(1);
    }

    @Test
    void railgunPiercesEveryEnemyOnTheLineEvenPastRangeAndEachAfterTheFirstTakesAQuarterLess() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III", "Momentum",
                "Transcendent", "Railgun");
        float scale = BoardFixtures.SCALE;
        FakeEnemyMob first = FakeEnemyMob.at(tower.getX() + 60, tower.getY());
        FakeEnemyMob aimed = FakeEnemyMob.at(tower.getX() + 100, tower.getY()).withProgression(10);
        FakeEnemyMob pastRange = FakeEnemyMob.at(tower.getX() + tower.getRangeReal() + scale, tower.getY());
        FakeEnemyMob beside = FakeEnemyMob.at(tower.getX() + 100, tower.getY() + 2 * scale);
        world.enemies().setEnemies(new EnemyMob[]{first, aimed, pastRange, beside});

        tower.doTick(1);

        int full = tower.damageCurrent();
        assertThat(first.onlyHitAmount()).isEqualTo(full);
        assertThat(aimed.onlyHitAmount()).isEqualTo(Math.round(full * 0.75f));
        assertThat(pastRange.onlyHitAmount()).isEqualTo(Math.round(full * 0.75f * 0.75f));
        assertThat(beside.hits()).isEmpty();
    }

    @Test
    void aRailgunShotsTraceRunsTwoCellsPastRangeWhereAPlainShotsEndsAtItsTarget() {
        GameWorld world = boardWorld();
        SniperTower railgun = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III", "Momentum",
                "Transcendent", "Railgun");
        SniperTower plain = sniperWith(world);
        FakeEnemyMob target = FakeEnemyMob.at(railgun.getX() + 60, railgun.getY());
        world.enemies().setEnemies(new EnemyMob[]{target});

        railgun.doTick(1);
        plain.doTick(1);

        float reach = railgun.getRangeReal() + 2 * BoardFixtures.SCALE;
        assertThat(railgun.lastShot()).hasValueSatisfying(trace -> {
            assertThat(trace.toX()).isCloseTo(railgun.getX() + reach, within(0.5f));
            assertThat(trace.toY()).isCloseTo(railgun.getY(), within(0.5f));
        });
        assertThat(plain.lastShot()).hasValueSatisfying(trace -> assertThat(trace.toX()).isEqualTo((float) target.getX()));
    }

    @Test
    void aTranscendedSnipersBarrelTurnsGold() {
        GameWorld world = boardWorld();
        SniperTower transcended = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III",
                "Momentum", "Transcendent");
        SniperTower awakened = sniperWith(world, "Focused Optics");
        TowerSpriteFrameBuilder sprites = new TowerSpriteFrameBuilder(world, 0, 0.0, 0.0);

        transcended.accept(sprites);
        awakened.accept(sprites);

        assertThat(sprites.buildHeads()).extracting(TurretHeadDraw::palette)
                .containsExactly(Palette.TOWER_SNIPER_GOLD_BARREL, Palette.TOWER_SNIPER_BODY);
    }

    @Test
    void executionerKillsAnEnemyAShotLeavesUnderFifteenPercentHealthAndThatCountsAsACrit() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III", "Momentum",
                "Transcendent", "Executioner");
        FakeEnemyMob target = targetFor(world);
        target.atHealthFraction(0.1f);

        tower.doTick(1);

        assertThat(target.hits()).hasSize(2);
        assertThat(target.hits().get(1).amount()).isGreaterThan(target.hits().get(0).amount());
        assertThat(target.attackers().get(1).delivery()).isEqualTo(Delivery.PERIODIC);
        assertThat(tower.wasLastShotCritical()).isTrue();
    }

    @Test
    void executionerLeavesAHealthyEnemyAlone() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Focused Optics II", "Focused Optics III", "Momentum",
                "Transcendent", "Executioner");
        FakeEnemyMob target = targetFor(world);

        tower.doTick(1);

        assertThat(target.hits()).hasSize(1);
        assertThat(tower.wasLastShotCritical()).isFalse();
    }

    @Test
    void fifthShotMakesEveryFifthShotAGuaranteedCritThatHitsHarder() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 15);
        tower.buyUpgrade(UpgradePaths.named(tower, "Marksman's Eye"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Fifth Shot"));
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 5);

        assertThat(target.attackers()).extracting(AttackProfile::guaranteedCrit)
                .containsExactly(false, false, false, false, true);
        assertThat(target.attackers()).extracting(AttackProfile::critMultiplier)
                .containsExactly(2.0f, 2.0f, 2.0f, 2.0f, 2.5f);
    }

    @Test
    void momentumTurnsTheShotAfterACritIntoAFivefoldShotThatIgnoresArmorAndPlating() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 20);
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Momentum"));
        FakeEnemyMob target = targetFor(world);
        target.landEveryHitCritical();

        this.fireShots(tower, target, 2);

        assertThat(target.hits().get(1).amount()).isEqualTo(5 * target.hits().get(0).amount());
        assertThat(target.attackers().get(0).armorPenetration()).isZero();
        assertThat(target.attackers().get(1).armorPenetration()).isEqualTo(1f);
        assertThat(target.attackers().get(1).platingPenetration()).isEqualTo(1f);
    }

    @Test
    void momentumHalvesTheWaitForFiveSecondsAfterAKillThenItEnds() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 20);
        tower.buyUpgrade(UpgradePaths.named(tower, "Focused Optics"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Momentum"));
        FakeEnemyMob killed = targetFor(world);
        killed.dieOnAnyHit();
        int killTick = this.tick(tower);
        FakeEnemyMob next = targetFor(world);

        List<Integer> ticks = this.shotTicks(tower, next, 1);

        assertThat(ticks.getFirst() - killTick).isEqualTo(waitTicks(tower, 0.5));
    }

    @Test
    void withoutMomentumAKillLeavesTheNextWaitUnchanged() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 10);
        FakeEnemyMob killed = targetFor(world);
        killed.dieOnAnyHit();
        int killTick = this.tick(tower);
        FakeEnemyMob next = targetFor(world);

        List<Integer> ticks = this.shotTicks(tower, next, 1);

        assertThat(ticks.getFirst() - killTick).isEqualTo(waitTicks(tower, 1.0));
    }

    @Test
    void hollowPointAppliesAVulnerabilityStackOnlyWhenTheShotCrits() {
        GameWorld world = boardWorld();
        SniperTower tower = new SniperTower(world, 0, 0);
        UpgradePaths.buy(tower, world, "Marksman's Eye", "Hollow Point");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 1);
        assertThat(target.appliedEffects()).isEmpty();

        target.landEveryHitCritical();
        this.fireShots(tower, target, 1);

        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.VULNERABLE);
        assertThat(target.appliedEffects().getFirst().stacks()).isEqualTo(1);
    }

    @Test
    void anEnemyUnderPriorityIsShotBeforeOneFurtherAlongThePath() {
        GameWorld world = boardWorld();
        SniperTower tower = awakenedSniper(world, 0);
        FakeEnemyMob furthest = FakeEnemyMob.at(100, 100).withProgression(900);
        FakeEnemyMob prioritised = FakeEnemyMob.at(100, 110).withProgression(10);
        prioritised.applyEffect(Effect.priority(100, d -> {
        }));
        world.enemies().setEnemies(new EnemyMob[]{furthest, prioritised});

        tower.doTick(1);

        assertThat(prioritised.hits()).hasSize(1);
        assertThat(furthest.hits()).isEmpty();
    }

    @Test
    void theFirstSpecialBoughtDecidesWhoTheSniperAimsAt() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Headhunter", "Focused Optics II", "Focused Optics III",
                "Transcendent", "Ricochet");
        FakeEnemyMob grunt = FakeEnemyMob.at(100, 100).withProgression(900);
        FakeEnemyMob boss = FakeEnemyMob.at(100, 110).ranked(Rank.BOSS).withProgression(1);
        world.enemies().setEnemies(new EnemyMob[]{grunt, boss});

        tower.doTick(1);

        assertThat(boss.hits()).hasSize(1);
        assertThat(grunt.hits()).isEmpty();
    }

    @Test
    void headhunterHitsAnEliteForFortyPercentMore() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Headhunter");
        FakeEnemyMob elite = FakeEnemyMob.at(100, 100).ranked(Rank.ELITE);
        world.enemies().setEnemies(new EnemyMob[]{elite});

        tower.doTick(1);

        assertThat(elite.onlyHitAmount()).isEqualTo(Math.round(tower.damageCurrent() * 1.4f));
    }

    @Test
    void aCritRicochetsToTheNearestEnemiesBesideTheTargetForSixtyPercentEach() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Ricochet");
        // only the first is within range; the rest are a chain of neighbours leading away from the tower
        float edge = tower.getRangeReal() * 0.85f;
        FakeEnemyMob aimed = FakeEnemyMob.at(tower.getX() + edge, tower.getY());
        aimed.landEveryHitCritical();
        FakeEnemyMob first = FakeEnemyMob.at(aimed.getX() + 40, aimed.getY());
        FakeEnemyMob second = FakeEnemyMob.at(first.getX() + 40, first.getY());
        FakeEnemyMob third = FakeEnemyMob.at(second.getX() + 40, second.getY());
        FakeEnemyMob fourth = FakeEnemyMob.at(third.getX() + 40, third.getY());
        FakeEnemyMob faraway = FakeEnemyMob.at(aimed.getX(), aimed.getY() + 300);
        world.enemies().setEnemies(new EnemyMob[]{aimed, first, second, third, fourth, faraway});

        tower.doTick(1);

        int bounce = Math.round(tower.damageCurrent() * 0.6f);
        assertThat(aimed.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(first.onlyHitAmount()).isEqualTo(bounce);
        assertThat(second.onlyHitAmount()).isEqualTo(bounce);
        assertThat(third.onlyHitAmount()).isEqualTo(bounce);
        assertThat(fourth.hits()).isEmpty();
        assertThat(faraway.hits()).isEmpty();
    }

    @Test
    void aShotThatDoesNotCritDoesNotRicochet() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Focused Optics", "Ricochet");
        FakeEnemyMob aimed = FakeEnemyMob.at(100, 100);
        FakeEnemyMob beside = FakeEnemyMob.at(120, 100);
        world.enemies().setEnemies(new EnemyMob[]{aimed, beside});

        tower.doTick(1);

        assertThat(aimed.hits().size() + beside.hits().size()).isEqualTo(1);
    }

    @Test
    void silverRoundsMakeTheThirdShotMagic() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Tradecraft", "Tradecraft II", "Silver Rounds");
        FakeEnemyMob target = targetFor(world);

        this.fireShots(tower, target, 3);

        assertThat(target.hits()).extracting(Damage::type)
                .containsExactly(DamageType.PHYSICAL, DamageType.PHYSICAL, DamageType.MAGIC);
    }

    @Test
    void tradecraftMakesACritFollowingACritHitHarder() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Tradecraft");
        FakeEnemyMob target = targetFor(world);
        target.landEveryHitCritical();

        this.fireShots(tower, target, 3);

        assertThat(target.attackers()).extracting(AttackProfile::critMultiplier)
                .containsExactly(2.0f, 2.25f, 2.5f);
    }

    @Test
    void tradecraftTwoHitsTargetsPastTwoThirdsOfTheRangeHarder() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Tradecraft", "Tradecraft II");
        float edge = tower.getRangeReal() * 0.9f;
        FakeEnemyMob far = FakeEnemyMob.at(tower.getX() + edge, tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{far});

        tower.doTick(1);

        assertThat(far.onlyHitAmount()).isEqualTo(Math.round(tower.damageCurrent() * 1.25f));
    }

    @Test
    void spotterUplinkLetsTheSniperShootAMarkedEnemyFromTwiceItsRange() {
        GameWorld world = boardWorld();
        SniperTower tower = sniperWith(world, "Tradecraft", "Tradecraft II", "Silver Rounds", "Focused Optics",
                "Momentum", "Transcendent", "Spotter Uplink");
        float far = tower.getRangeReal() * 1.5f;
        FakeEnemyMob unmarked = FakeEnemyMob.at(tower.getX() + far, tower.getY());
        FakeEnemyMob marked = FakeEnemyMob.at(tower.getX(), tower.getY() + far);
        marked.applyEffect(Effect.marked(100, d -> {
        }));
        world.enemies().setEnemies(new EnemyMob[]{unmarked, marked});

        tower.doTick(1);

        assertThat(marked.hits()).hasSize(1);
        assertThat(unmarked.hits()).isEmpty();
    }
}
