package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;
import td.util.RandomSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Cinder only applies burns, so hits appear as burn ticks, and each case ticks long enough for the
 * wave to arrive.
 */
class CinderTowerTest {

    /** A roll no chance beats, so an ignition's size never depends on a crit. */
    private static final RandomSource NEVER_CRITS = () -> 0.999;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(NEVER_CRITS, BoardFixtures.SCALE, 20, 20);

    private CinderTower towerAt(int cellX, int cellY) {
        return new CinderTower(this.context, cellX, cellY);
    }

    private static void tickThrough(CinderTower tower, int fromInclusive, int toInclusive) {
        for (int t = fromInclusive; t <= toInclusive; t++) {
            tower.doTick(t);
        }
    }

    @Test
    void anEnemyDirectlyAheadOfTheDefaultHeadingIsBurnedOnceTheWaveReachesIt() {
        CinderTower tower = towerAt(3, 3); // centre at (112, 112)
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112); // due +X of the tower, 38px out
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ahead.appliedEffects()).hasSize(1);
        assertThat(ahead.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.BURN);
        assertThat(ahead.appliedEffects().getFirst().damagePerTick().type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void anEnemyIsNotBurnedOnTheSameTickItsWaveFires() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tower.doTick(1); // fires the wave; it hasn't travelled anywhere yet

        assertThat(ahead.appliedEffects()).isEmpty();
    }

    @Test
    void aWaveBurnsALingeringEnemyExactlyOnceAcrossSeveralTicksInsideItsExpandingBand() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob close = FakeEnemyMob.at(119, 112); // 7px out - caught almost immediately
        this.context.enemies().setEnemies(new EnemyMob[]{close});

        tower.doTick(1); // fires
        tower.doTick(2); // radius already covers 7px - first (and only) burn
        assertThat(close.appliedEffects()).hasSize(1);

        // the enemy stays inside the expanding band for the rest of the wave's travel, but the
        // wave must not re-burn it on every subsequent tick it remains caught
        tickThrough(tower, 3, 1 + CinderTower.WAVE_TRAVEL_TICKS);
        assertThat(close.appliedEffects()).hasSize(1);
    }

    @Test
    void aWaveThatHasFullyTravelledStopsBeingTracked() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void theCooldownGatesFiringASecondWaveUntilItActuallyElapses() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS); // first wave fires and lands
        assertThat(ahead.appliedEffects()).hasSize(1);

        // still well within the first shot's cooldown - no second wave fired yet
        tickThrough(tower, 2 + CinderTower.WAVE_TRAVEL_TICKS, CinderTower.COOLDOWN_MAX);
        assertThat(ahead.appliedEffects()).hasSize(1);

        // cooldown has now elapsed (fired at tick 1, resets after COOLDOWN_MAX ticks) and the
        // second wave has had time to travel back out to the same enemy
        tickThrough(tower, CinderTower.COOLDOWN_MAX + 1, CinderTower.COOLDOWN_MAX + 1 + CinderTower.WAVE_TRAVEL_TICKS);
        assertThat(ahead.appliedEffects()).hasSize(2);
    }

    @Test
    void anEnemyBehindTheTurretIsNotFiredAtUntilTheTurretHasTurnedToIt() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob behind = FakeEnemyMob.at(74, 112); // due -X, half a turn from the default heading
        this.context.enemies().setEnemies(new EnemyMob[]{behind});

        tower.doTick(1);

        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void theFirstWaveFiredAtAnEnemyBehindTheTurretHeadsAtTheEnemy() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob behind = FakeEnemyMob.at(74, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{behind});

        tickThrough(tower, 1, 40);

        assertThat(behind.appliedEffects()).isNotEmpty();
    }

    @Test
    void aGhostCaughtInAWaveFiredAtAVisibleEnemyIsBurnedToo() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob visible = FakeEnemyMob.at(150, 112);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(140, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{visible, ghost});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ghost.appliedEffects()).hasSize(1);
    }

    @Test
    void aLoneGhostIsNeitherAimedAtNorFiredAt() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(tower.getInFlightWaves()).isEmpty();
        assertThat(ghost.appliedEffects()).isEmpty();
    }

    @Test
    void anEnemyOutOfRangeIsNotBurned() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob farAway = FakeEnemyMob.at(10_000, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{farAway});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(farAway.appliedEffects()).isEmpty();
    }

    @Test
    void noEnemyInRangeAppliesNoBurnAndHoldsTheLastHeading() {
        CinderTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(tower.getTurretAim().currentRadians()).isEqualTo(0.0);
        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void wideNozzleAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.awakenVeteran(tower);
        UpgradeNode wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");

        boolean chosen = tower.buyUpgrade(wideNozzle);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(CinderTower.RANGE * BoardFixtures.SCALE);
    }

    @Test
    void whiteFlameIsNotYetChoosableBeforeAttuneIsBought() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        UpgradeNode whiteFlame = UpgradePaths.named(tower, "White Flame");

        boolean chosen = tower.buyUpgrade(whiteFlame);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void wideNozzleBumpsTheHalfWidthRadiansBeyondTheBase() {
        CinderTower tower = towerAt(3, 3);
        UpgradeNode wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");
        double halfWidthBeforeChoosing = tower.getHalfWidthRadians();

        tower.onUpgradeBought(wideNozzle);

        assertThat(tower.getHalfWidthRadians()).isGreaterThan(halfWidthBeforeChoosing);
    }

    private static final int TRAVEL = CinderTower.WAVE_TRAVEL_TICKS;
    private static final int CYCLE = CinderTower.COOLDOWN_MAX + 1;

    /** A tower with the Soulfire chain and a special, Transcendent bought, gates waived. */
    private CinderTower transcendentTower() {
        CinderTower tower = this.towerAt(3, 3);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        UpgradePaths.buy(tower, this.context, "White Flame", "White Flame II", "Soulfire", "Searing Flame",
                "Transcendent");
        return tower;
    }

    private static List<Effect> burns(FakeEnemyMob enemy) {
        return enemy.appliedEffects().stream().filter(effect -> effect.kind() == EffectKind.BURN).toList();
    }

    @Test
    void whiteFlameIiMakesTheBurnAQuarterShorter() {
        CinderTower before = towerAt(3, 3);
        FakeEnemyMob beforeTarget = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{beforeTarget});
        tickThrough(before, 1, 1 + TRAVEL);

        CinderTower after = towerAt(3, 3);
        UpgradePaths.buy(after, this.context, "White Flame", "White Flame II");
        FakeEnemyMob afterTarget = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{afterTarget});
        tickThrough(after, 1, 1 + TRAVEL);

        assertThat(afterTarget.appliedEffects().getFirst().authoredDurationTicks())
                .isEqualTo(Math.round(beforeTarget.appliedEffects().getFirst().authoredDurationTicks() * 0.75f));
    }

    @Test
    void withoutAttuneAWaveOnABurningEnemyAddsNoStoke() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 3 * CYCLE);

        assertThat(burns(target)).hasSizeGreaterThan(1);
        assertThat(burns(target)).extracting(effect -> effect.damagePerTick().amount()).containsOnly(150);
    }

    @Test
    void attunedEachWaveOnAnEnemyAlreadyBurningFromItRaisesItsBurnTenPercentUpToThreeTimes() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 6 * CYCLE);

        assertThat(burns(target).stream().limit(6).map(effect -> effect.damagePerTick().amount()).toList())
                .containsExactly(150, 165, 180, 195, 195, 195);
        assertThat(tower.stokeStepsOf(target)).isEqualTo(3);
    }

    @Test
    void whiteFlameMakesEachStokeStepFifteenPercent() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "White Flame");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 2 * CYCLE);

        float base = tower.damageCurrent();
        assertThat(burns(target).get(1).damagePerTick().amount()).isEqualTo(Math.round(base * 1.15f));
    }

    @Test
    void everyStokingWaveIsADeedAndTheFirstWaveIsNot() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, TRAVEL + 1);
        int afterTheFirst = tower.experience().deeds();
        tickThrough(tower, TRAVEL + 2, 2 * CYCLE + TRAVEL);

        assertThat(afterTheFirst).isZero();
        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    @Test
    void theStokeGoesWhenTheBurnEndsAndAFreshWaveStartsAgain() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tickThrough(tower, 1, 3 * CYCLE);
        int stoked = tower.stokeStepsOf(target);

        target.expire(EffectKind.BURN);
        tower.doTick(3 * CYCLE + 1);

        assertThat(stoked).isGreaterThan(0);
        assertThat(tower.stokeStepsOf(target)).isZero();
    }

    @Test
    void wideNozzleLetsAnEnemyThatLostItsBurnKeepItsStokeForTwoSeconds() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Wide Nozzle");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tickThrough(tower, 1, 3 * CYCLE);
        int stoked = tower.stokeStepsOf(target);

        target.expire(EffectKind.BURN);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(1000, 1000)});
        tower.doTick(3 * CYCLE + 1);
        int justAfter = tower.stokeStepsOf(target);
        tickThrough(tower, 3 * CYCLE + 2, 3 * CYCLE + 60);

        assertThat(stoked).isGreaterThan(0);
        assertThat(justAfter).isEqualTo(stoked);
        assertThat(tower.stokeStepsOf(target)).isZero();
    }

    @Test
    void wideNozzleWidensTheConeAndWideNozzleIiMore() {
        CinderTower tower = towerAt(3, 3);
        double base = tower.getHalfWidthRadians();

        UpgradePaths.buy(tower, this.context, "Wide Nozzle");
        double first = tower.getHalfWidthRadians();
        UpgradePaths.buy(tower, this.context, "Wide Nozzle II");

        assertThat(first).isCloseTo(base * 1.3, within(1e-6));
        assertThat(tower.getHalfWidthRadians()).isCloseTo(base * 1.3 * 1.2, within(1e-6));
    }

    @Test
    void bellowsWidensTheConeByTheFireRateAnAuraGivesItOnlyOnceAwakened() {
        CinderTower tower = towerAt(3, 3);
        this.context.towers().add(td.fixtures.FakeTower.offering(this.context, 3, 4, td.tower.upgrade.UpgradeTree.none())
                .giving(td.tower.buff.TowerBuff.fireRate(0.2f)));
        double base = tower.getHalfWidthRadians();

        this.context.economy().startEconomy(1_000_000, 5);
        tower.earnXp(1_000);
        tower.buyUpgrade(UpgradePaths.named(tower, "Attune"));
        double attuned = tower.getHalfWidthRadians();
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));

        assertThat(attuned).isEqualTo(base);
        assertThat(tower.getHalfWidthRadians()).isCloseTo(base * 1.2, within(1e-6));
    }

    @Test
    void longNozzleMakesTheWaveTravelTwiceAsFast() {
        CinderTower tower = this.transcendentTower();
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tower.doTick(1);
        int before = tower.getInFlightWaves().getFirst().travelTicks();

        UpgradePaths.buy(tower, this.context, "Range", "Range II", "Range III");
        CinderTower fresh = towerAt(8, 3);
        UpgradePaths.buy(fresh, this.context, "White Flame", "White Flame II", "Soulfire", "Searing Flame",
                "Transcendent", "Range", "Range II", "Range III");
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(fresh.getX() + 38, fresh.getY())});
        fresh.doTick(1);

        assertThat(before).isEqualTo(TRAVEL);
        assertThat(fresh.getInFlightWaves().getFirst().travelTicks()).isEqualTo(TRAVEL / 2);
    }

    @Test
    void soulfireIsEveryOtherWaveAndStokeCountsBothKinds() {
        CinderTower tower = this.towerAt(3, 3);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        UpgradePaths.buy(tower, this.context, "White Flame", "White Flame II", "Soulfire");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 4 * CinderTower.COOLDOWN_MAX + 4 * TRAVEL);

        assertThat(target.appliedEffects()).extracting(Effect::kind).startsWith(EffectKind.BURN, EffectKind.SOULFIRE,
                EffectKind.BURN, EffectKind.SOULFIRE);
        assertThat(tower.stokeStepsOf(target)).isEqualTo(3);
    }

    @Test
    void lingeringFlamesLeavesAPatchOfBurningGroundWhereTheTargetStandsEachWave() {
        CinderTower tower = this.towerAt(3, 3);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        UpgradePaths.buy(tower, this.context, "Wide Nozzle", "Wide Nozzle II", "Lingering Flames");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.zones().zones()).singleElement().satisfies(zone -> {
            assertThat(zone.kind()).isEqualTo(td.zone.ZoneKind.BURNING_GROUND);
            assertThat(zone.x()).isEqualTo(150.0);
            assertThat(zone.y()).isEqualTo(112.0);
        });
    }

    @Test
    void searingFlameMakesAnIgnitionVulnerableAndEveryLaterWaveOnABurningEnemyOnceASecondAtMost() {
        CinderTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Searing Flame");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, TRAVEL + 1);
        long afterTheIgnition = target.appliedEffects().stream().filter(e -> e.kind() == EffectKind.VULNERABLE).count();
        tickThrough(tower, TRAVEL + 2, 3 * CYCLE + TRAVEL);

        assertThat(afterTheIgnition).isEqualTo(1);
        assertThat(target.appliedEffects().stream().filter(e -> e.kind() == EffectKind.VULNERABLE).count())
                .isBetween(2L, 4L);
    }

    @Test
    void aCritIgnitionStartsThePoolAtTheCritMultiplier() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        target.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().damagePerTick().amount()).isEqualTo(Math.round(tower.damageCurrent() * 1.5f));
    }

    @Test
    void flashpointAddsThreeScorchedToACritIgnitionAndNothingToAPlainOne() {
        CinderTower tower = this.transcendentTower();
        UpgradePaths.buy(tower, this.context, "Flashpoint");
        FakeEnemyMob crit = FakeEnemyMob.at(150, 112);
        crit.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{crit});
        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(crit.appliedEffects()).anySatisfy(effect -> {
            assertThat(effect.kind()).isEqualTo(EffectKind.SCORCHED);
            assertThat(effect.stacks()).isEqualTo(3);
        });
    }

    @Test
    void theFuelLineTunesThePoolsItFeeds() {
        CinderTower tower = this.transcendentTower();
        UpgradePaths.buy(tower, this.context, "Kindling", "Cauterize", "Heat");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().tuning().stackRate()).isEqualTo(2);
        assertThat(burns(target).getFirst().tuning().cauterizes()).isTrue();
        assertThat(burns(target).getFirst().tuning().heats()).isTrue();
    }

    @Test
    void thermalShockAndThePyromancersMarkTuneThePoolAndShockChillsNeighboursOfAFrozenBurningEnemy() {
        CinderTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Thermal Shock");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        FakeEnemyMob neighbour = FakeEnemyMob.at(150, 130);
        this.context.enemies().setEnemies(new EnemyMob[]{target, neighbour});
        tickThrough(tower, 1, 1 + TRAVEL);
        float burstShare = burns(target).getFirst().tuning().freezeBurstShare();

        target.expire(EffectKind.BURN);
        target.reportFrozen();
        tower.doTick(2 + TRAVEL);

        assertThat(burstShare).isEqualTo(1.5f);
        assertThat(neighbour.activeEffectKinds()).contains(EffectKind.CHILL);
    }

    @Test
    void thePyromancersMarkMarksTheBurningForMagic() {
        CinderTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Pyromancer's Mark");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().tuning().marksForMagic()).isTrue();
    }

    /** A tower on the Wide Nozzle chain with a special and Transcendent, gates waived. */
    private CinderTower nozzleTower() {
        CinderTower tower = this.towerAt(3, 3);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        UpgradePaths.buy(tower, this.context, "Wide Nozzle", "Wide Nozzle II", "Lingering Flames", "Searing Flame",
                "Transcendent");
        return tower;
    }

    /** A real enemy standing where the path starts, which nothing ticks, so its pool never decays. */
    private td.enemy.EnemyMob realEnemyAt(double x, double y) {
        this.context.setPath(new td.wave.PathNormal(List.of(new td.wave.Vec2(x, y), new td.wave.Vec2(x + 900, y))));
        return td.enemy.EnemyFactory.getEnemy("c", this.context, 0, 1_000_000, 3, td.enemy.Rank.GRUNT);
    }

    @Test
    void infernoRingBurnsEnemiesOnEverySideAndCostsAQuarterOfTheRange() {
        CinderTower tower = this.nozzleTower();
        float rangeBefore = tower.getRangeReal();
        UpgradePaths.buy(tower, this.context, "Inferno Ring");
        FakeEnemyMob ahead = FakeEnemyMob.at(tower.getX() + 40, tower.getY());
        FakeEnemyMob behind = FakeEnemyMob.at(tower.getX() - 40, tower.getY());
        FakeEnemyMob above = FakeEnemyMob.at(tower.getX(), tower.getY() - 40);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead, behind, above});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(ahead)).isNotEmpty();
        assertThat(burns(behind)).isNotEmpty();
        assertThat(burns(above)).isNotEmpty();
        assertThat(tower.getRangeReal()).isLessThan(rangeBefore);
    }

    @Test
    void dragonsBreathFiresTwiceAsOftenAsThreeTimesTheRateForLessDamageAndKeepsStokeFull() {
        CinderTower tower = this.nozzleTower();
        float damageBefore = tower.damageCurrent();
        double rateBefore = tower.fireRateCurrent();
        UpgradePaths.buy(tower, this.context, "Dragon's Breath");
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + 40, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(tower.fireRateCurrent()).isGreaterThan(rateBefore * 2);
        assertThat(tower.damageCurrent()).isLessThan(Math.round(damageBefore * 0.45f));
        assertThat(tower.stokeStepsOf(target)).isEqualTo(3);
        assertThat(burns(target).getFirst().damagePerTick().amount())
                .isEqualTo(Math.round(tower.damageCurrent() * 1.3f));
    }

    @Test
    void combustionBurstsAPoolThatReachesItsCapOntoTheEnemiesWithinACellAndOnlyOnce() {
        CinderTower tower = this.transcendentTower();
        UpgradePaths.buy(tower, this.context, "Combustion");
        td.enemy.EnemyMob full = this.realEnemyAt(tower.getX() + 40, tower.getY());
        FakeEnemyMob neighbour = FakeEnemyMob.at(tower.getX() + 40, tower.getY() + 20);
        FakeEnemyMob far = FakeEnemyMob.at(tower.getX() + 40, tower.getY() + 100).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{full, neighbour, far});
        td.effect.PoolTuning deep = td.effect.PoolTuning.standard().withCapFactor(4f);
        for (int i = 0; i < 60; i++) {
            full.applyEffect(Effect.burn(td.damage.Damage.magic(300), 60, d -> {
            }).withTuning(deep));
        }

        tickThrough(tower, 1, 1 + TRAVEL);
        int hitsAfterTheFirstBurst = neighbour.hits().size();
        tickThrough(tower, 2 + TRAVEL, 2 + TRAVEL + CYCLE);

        assertThat(hitsAfterTheFirstBurst).isEqualTo(1);
        assertThat(neighbour.hits()).hasSize(1);
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void everburnTopsAPoolBackUpToAQuarterOfItsStrongestApplicationWhileTheEnemyIsInRange() {
        CinderTower tower = this.nozzleTower();
        UpgradePaths.buy(tower, this.context, "Kindling", "Cauterize", "Heat", "Everburn");
        td.enemy.EnemyMob enemy = this.realEnemyAt(tower.getX() + 40, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});
        tickThrough(tower, 1, 1 + TRAVEL);
        enemy.applyEffect(Effect.invisible(10_000, d -> {
        }));
        float peak = enemy.activeEffects().stream().filter(e -> e.kind() == EffectKind.BURN).findFirst().orElseThrow().peakL0();

        for (int t = 2 + TRAVEL; t < 2 + TRAVEL + 40; t++) {
            enemy.doTick(t);
            tower.doTick(t);
        }

        Effect pool = enemy.activeEffects().stream().filter(e -> e.kind() == EffectKind.BURN).findFirst().orElseThrow();
        assertThat(pool.fuelLevel()).isGreaterThanOrEqualTo(0.24f * peak);
    }
}
