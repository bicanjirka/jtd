package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.PoolTuning;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;
import td.util.RandomSource;
import td.wave.PathNormal;
import td.wave.Vec2;
import td.zone.ZoneKind;

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

    private static final int TRAVEL = CinderTower.WAVE_TRAVEL_TICKS;
    private static final int CYCLE = CinderTower.COOLDOWN_MAX + 1;

    private CinderTower towerWith(String... nodes) {
        CinderTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private static List<Effect> burns(FakeEnemyMob enemy) {
        return enemy.appliedEffects().stream().filter(effect -> effect.kind() == EffectKind.BURN).toList();
    }

    @Test
    void withoutAttuneAWaveOnABurningEnemyAddsNoStoke() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 3 * CYCLE);

        assertThat(burns(target)).hasSizeGreaterThan(1);
        assertThat(burns(target)).extracting(effect -> effect.damagePerTick().amount()).containsOnly(tower.damageCurrent());
    }

    @Test
    void attunedEachWaveOnAnEnemyAlreadyBurningFromItBurnsHotterUntilTheStokeIsFull() {
        CinderTower tower = this.towerWith("Attune");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 6 * CYCLE);

        List<Integer> burnsBySize = burns(target).stream().map(effect -> effect.damagePerTick().amount()).toList();
        assertThat(burnsBySize.get(1)).isGreaterThan(burnsBySize.get(0));
        assertThat(burnsBySize.get(2)).isGreaterThan(burnsBySize.get(1));
        assertThat(burnsBySize.get(5)).isEqualTo(burnsBySize.get(4));
    }

    @Test
    void aWaveCarriesTheStokeStepsTheEnemiesInItsPathAreAboutToReach() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Attune");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        int first = tower.getInFlightWaves().getLast().stokeSteps();
        int t = 2;
        while (tower.getInFlightWaves().stream().noneMatch(wave -> wave.firedAtTick() > 1)) {
            tower.doTick(t++);
        }
        int second = tower.getInFlightWaves().getLast().stokeSteps();

        assertThat(first).isZero();
        assertThat(second).isEqualTo(1);
    }

    @Test
    void aWaveWithoutStokeCarriesNoSteps() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + CYCLE);

        assertThat(tower.getInFlightWaves().getLast().stokeSteps()).isZero();
    }

    @Test
    void whiteFlameMakesAStokeStepHotter() {
        assertThat(this.secondBurnOf(this.towerWith("White Flame"))).isGreaterThan(this.secondBurnOf(this.towerWith("Attune")));
    }

    /** How hot the second wave {@code tower} lands on one enemy burns it. */
    private int secondBurnOf(CinderTower tower) {
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tickThrough(tower, 1, 2 * CYCLE);
        return burns(target).get(1).damagePerTick().amount();
    }

    @Test
    void everyStokingWaveIsADeedAndTheFirstWaveIsNot() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Attune");
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
        UpgradePaths.buy(tower, this.context, "Attune");
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
    void aStokeGraceLetsAnEnemyThatLostItsBurnKeepItsStokeForThatLong() {
        CinderTower tower = TowerFixtures.buffed(this.towerWith("Attune"), this.context, TowerModifiers.stokeGrace(40));
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
    void bellowsWidensTheConeByTheFireRateAnAuraGivesItOnlyOnceAwakened() {
        CinderTower tower = TowerFixtures.buffed(towerAt(3, 3), this.context, TowerModifiers.attackSpeed(0.2f));
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
        CinderTower tower = this.towerWith("Transcendent");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tower.doTick(1);
        int before = tower.getInFlightWaves().getFirst().travelTicks();

        CinderTower fresh = towerAt(8, 3);
        UpgradePaths.buy(fresh, this.context, "Range III");
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(fresh.getX() + 38, fresh.getY())});
        fresh.doTick(1);

        assertThat(before).isEqualTo(TRAVEL);
        assertThat(fresh.getInFlightWaves().getFirst().travelTicks()).isEqualTo(TRAVEL / 2);
    }

    @Test
    void soulfireBurnsEveryWaveAlongsideTheOrdinaryBurnAndStokeCountsBothKinds() {
        CinderTower tower = this.towerWith("Soulfire");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 4 * CinderTower.COOLDOWN_MAX + 4 * TRAVEL);

        assertThat(target.appliedEffects()).extracting(Effect::kind).startsWith(EffectKind.BURN, EffectKind.SOULFIRE,
                EffectKind.BURN, EffectKind.SOULFIRE);
        assertThat(tower.stokeStepsOf(target)).isPositive();
    }

    @Test
    void lingeringFlamesLeavesAPatchOfBurningGroundWhereTheTargetStandsEachWave() {
        CinderTower tower = this.towerWith("Lingering Flames");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.zones().zones()).singleElement().satisfies(zone -> {
            assertThat(zone.kind()).isEqualTo(ZoneKind.BURNING_GROUND);
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
    void aWiderConeStatWidensTheCone() {
        CinderTower plain = towerAt(3, 3);

        CinderTower wide = TowerFixtures.buffed(towerAt(8, 3), this.context, TowerModifiers.coneWidth(0.3f));

        assertThat(wide.getHalfWidthRadians()).isGreaterThan(plain.getHalfWidthRadians());
    }

    @Test
    void anIgnitionBurnsForTheTowersBurnDurationAndFeedsAPoolTunedByItsStats() {
        CinderTower tower = TowerFixtures.buffed(towerAt(3, 3), this.context, TowerModifiers.burnDuration(20)
                .plus(TowerModifiers.burnPoolCap(1f)).plus(TowerModifiers.burnMarkRate(1f)));
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        Effect burn = burns(target).getFirst();
        assertThat(burn.authoredDurationTicks()).isEqualTo(Math.round(tower.stats().value(TowerStat.BURN_DURATION)));
        assertThat(burn.tuning().capFactor()).isEqualTo(tower.stats().value(TowerStat.BURN_POOL_CAP));
        assertThat(burn.tuning().stackRate()).isEqualTo(Math.round(tower.stats().value(TowerStat.BURN_MARK_RATE)));
        assertThat(burn.tuning().capFactor()).isGreaterThan(PoolTuning.standard().capFactor());
    }

    @Test
    void aCritIgnitionStartsThePoolAtTheCritMultiplier() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        target.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().damagePerTick().amount())
                .isEqualTo(Math.round(tower.damageCurrent() * tower.stats().attack().critMultiplier()));
    }

    @Test
    void flashpointAddsEightScorchedToACritIgnition() {
        CinderTower tower = this.towerWith("Flashpoint");
        FakeEnemyMob crit = FakeEnemyMob.at(150, 112);
        crit.landEveryHitCritical();
        this.context.enemies().setEnemies(new EnemyMob[]{crit});
        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(crit.appliedEffects()).anySatisfy(effect -> {
            assertThat(effect.kind()).isEqualTo(EffectKind.SCORCHED);
            assertThat(effect.stacks()).isEqualTo(8);
        });
    }

    @Test
    void cauterizeAndHeatTuneThePoolsTheyFeedToCutHealingAndRaiseWhatTicks() {
        CinderTower tower = this.towerWith("Cauterize", "Heat");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().tuning().carried().flat(EnemyStat.HEALING_RECEIVED)).isNegative();
        assertThat(burns(target).getFirst().tuning().carried().flat(EnemyStat.PERIODIC_DAMAGE_TAKEN)).isPositive();
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

        assertThat(burstShare).isGreaterThan(PoolTuning.standard().freezeBurstShare());
        assertThat(neighbour.activeEffectKinds()).contains(EffectKind.CHILL);
    }

    @Test
    void thePyromancersMarkMarksTheBurningForMagic() {
        CinderTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Pyromancer's Mark");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(burns(target).getFirst().tuning().carried().flat(EnemyStat.MAGIC_DAMAGE_TAKEN)).isPositive();
    }

    /** A real enemy standing where the path starts, which nothing ticks, so its pool never decays. */
    private EnemyMob realEnemyAt(double x, double y) {
        this.context.setPath(new PathNormal(List.of(new Vec2(x, y), new Vec2(x + 900, y))));
        return EnemyFixtures.plainMob(this.context, 1_000_000, 3);
    }

    @Test
    void infernoRingBurnsEnemiesOnEverySideAndKeepsTheRange() {
        CinderTower tower = this.towerWith("Lingering Flames", "Transcendent");
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
        assertThat(tower.getRangeReal()).isEqualTo(rangeBefore);
    }

    @Test
    void dragonsBreathFiresMoreThanTwiceAsOftenForLessDamageAndKeepsStokeFull() {
        CinderTower tower = this.towerWith("Lingering Flames", "Transcendent");
        float damageBefore = tower.damageCurrent();
        double rateBefore = tower.attackRate();
        UpgradePaths.buy(tower, this.context, "Dragon's Breath");
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + 40, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tickThrough(tower, 1, 1 + TRAVEL);

        assertThat(tower.attackRate()).isGreaterThan(rateBefore * 2);
        assertThat(tower.damageCurrent()).isLessThan(Math.round(damageBefore));
        assertThat(tower.stokeStepsOf(target)).isEqualTo(3);
        assertThat(burns(target).getFirst().damagePerTick().amount()).isGreaterThan(tower.damageCurrent());
    }

    @Test
    void combustionBurstsAnEnemyWhoseFireIsHotterThanAWaveCouldMakeItOntoTheEnemiesWithinACellAndOnlyOnce() {
        CinderTower tower = this.towerWith("Combustion");
        EnemyMob full = this.realEnemyAt(tower.getX() + 40, tower.getY());
        FakeEnemyMob neighbour = FakeEnemyMob.at(tower.getX() + 40, tower.getY() + 20);
        FakeEnemyMob far = FakeEnemyMob.at(tower.getX() + 40, tower.getY() + 100).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{full, neighbour, far});
        PoolTuning deep = PoolTuning.standard().withCapFactor(4f);
        for (int i = 0; i < 60; i++) {
            full.applyEffect(Effect.burn(Damage.magic(300), 60, DamageSink.none()).withTuning(deep));
        }

        tickThrough(tower, 1, 1 + TRAVEL);
        int hitsAfterTheFirstBurst = neighbour.hits().size();
        tickThrough(tower, 2 + TRAVEL, 2 + TRAVEL + CYCLE);

        assertThat(hitsAfterTheFirstBurst).isEqualTo(1);
        assertThat(neighbour.hits()).hasSize(1);
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void everburnTopsAPoolBackUpToTwoFifthsOfItsStrongestApplicationWhileTheEnemyIsInRange() {
        CinderTower tower = this.towerWith("Everburn");
        EnemyMob enemy = this.realEnemyAt(tower.getX() + 40, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});
        tickThrough(tower, 1, 1 + TRAVEL);
        enemy.applyEffect(Effect.invisible(10_000, DamageSink.none()));
        float peak = enemy.activeEffects().stream().filter(e -> e.kind() == EffectKind.BURN).findFirst().orElseThrow().peakL0();

        for (int t = 2 + TRAVEL; t < 2 + TRAVEL + 40; t++) {
            enemy.doTick(t);
            tower.doTick(t);
        }

        Effect pool = enemy.activeEffects().stream().filter(e -> e.kind() == EffectKind.BURN).findFirst().orElseThrow();
        assertThat(pool.fuelLevel()).isGreaterThanOrEqualTo(0.39f * peak);
    }

    @Test
    void aStokedEnemyCarriesTheScorchersStokeAsAMark() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Attune");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int t = 1; t < 400 && tower.marksOn(target).isEmpty(); t++) {
            tower.doTick(t);
        }

        assertThat(tower.marksOn(target)).singleElement().satisfies(mark -> {
            assertThat(mark.term()).isEqualTo(TowerTerm.STOKE);
            assertThat(mark.steps()).isPositive();
        });
    }

    @Test
    void appliesListsEachEffectOnceWithEverySourceOnTheTower() {
        CinderTower tower = this.towerWith("Soulfire");

        assertThat(tower.applies()).extracting(AppliedEffect::term).containsExactly(EffectKind.BURN, EffectKind.SOULFIRE);
        assertThat(tower.applies().getFirst().sources()).hasSize(1);
        assertThat(tower.inspect().targeting()).isEqualTo("nearest");
    }
}
