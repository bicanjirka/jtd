package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scale 32, so the blast's radius is 48 px and a cell 32. Exactly one visible mob is in range unless
 * a test says otherwise, so it is the primary; the others are ghosts, which the blast and arcs reach
 * but aiming does not.
 */
class SplashTowerTest {

    private static final float RADIUS = SplashTower.BLAST_RADIUS_CELLS * BoardFixtures.SCALE;
    private static final int FULL = DamageUnits.ofPoints(SplashTower.DAMAGE_POINTS);

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private SplashTower tower() {
        return new SplashTower(this.context, 3, 3);
    }

    private SplashTower upgradedTower(String... nodes) {
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        SplashTower tower = this.tower();
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private void enemies(EnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    private static List<Effect> effectsOf(FakeEnemyMob mob, EffectKind kind) {
        return mob.appliedEffects().stream().filter(effect -> effect.kind() == kind).toList();
    }

    private static void saturate(FakeEnemyMob mob, int stacks) {
        mob.applyEffect(Effect.saturated(stacks, 30, d -> {
        }));
    }

    @Test
    void theBlastCentreTakesFullDamageAndTheRestFallsOffWithTheSquareOfTheDistance() {
        SplashTower tower = this.tower();
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob halfOut = FakeEnemyMob.ghostAt(100, 100 + RADIUS / 2);
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(100, 100 + RADIUS + 1);
        this.enemies(centre, halfOut, outside);

        tower.doTick(0);

        assertThat(centre.onlyHitAmount()).isEqualTo(FULL);
        assertThat(halfOut.onlyHitAmount()).isEqualTo(Math.round(FULL * 0.75f));
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void unattunedItAimsAtRandomAndBuildsNoSaturation() {
        SplashTower tower = this.tower();
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);

        tower.doTick(0);

        assertThat(effectsOf(enemy, EffectKind.SATURATED)).isEmpty();
        assertThat(tower.inspect().behaviours()).extracting(BehaviourLine::value).contains("random");
    }

    @Test
    void fireControlLandsTheBlastOnTheEnemyWithMostNeighbours() {
        SplashTower tower = this.upgradedTower();
        FakeEnemyMob alone = FakeEnemyMob.at(70, 140).withProgression(50);
        FakeEnemyMob crowded = FakeEnemyMob.at(130, 100);
        FakeEnemyMob neighbour = FakeEnemyMob.at(140, 105);
        this.enemies(alone, crowded, neighbour);

        tower.doTick(0);

        assertThat(tower.getPrimaryTarget()).isIn(crowded, neighbour);
        assertThat(alone.hits()).isEmpty();
    }

    @Test
    void anAttunedBlastSaturatesWhatItCatchesForOneAndAHalfSeconds() {
        SplashTower tower = this.upgradedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);

        tower.doTick(0);

        assertThat(effectsOf(enemy, EffectKind.SATURATED)).singleElement().satisfies(effect -> {
            assertThat(effect.stacks()).isEqualTo(1);
            assertThat(effect.effectiveStackCap()).isEqualTo(3);
            assertThat(effect.remainingTicks()).isEqualTo(Math.round(1.5f * TickRate.TICKS_PER_SECOND));
        });
    }

    @Test
    void eachSaturationStackMakesTheBlastHitFivePercentHarderAndAPrimaryAlreadySaturatedIsADeed() {
        SplashTower tower = this.upgradedTower();
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        saturate(enemy, 2);
        this.enemies(enemy);

        tower.doTick(0);

        assertThat(enemy.onlyHitAmount()).isEqualTo(Math.round(FULL * 1.1f));
        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    @Test
    void wideChargeWidensTheBlastAndItsEdgeDealsAQuarter() {
        SplashTower tower = this.upgradedTower("Wide Charge");
        float radius = RADIUS * 1.25f;
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob edge = FakeEnemyMob.ghostAt(100, 100 + radius - 1);
        this.enemies(centre, edge);

        tower.doTick(0);

        assertThat(edge.onlyHitAmount()).isEqualTo(Math.round(FULL * 0.25f));
        assertThat(tower.inspect().stats()).filteredOn(line -> line.stat() == TowerStat.SPLASH_RADIUS)
                .singleElement().extracting(TowerStatLine::current).isEqualTo(SplashTower.BLAST_RADIUS_CELLS * 1.25f);
    }

    @Test
    void shapedChargeGivesTheInnerHalfTwoStacksAndRaisesTheCapToFour() {
        SplashTower tower = this.upgradedTower("Wide Charge", "Shaped Charge");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob outer = FakeEnemyMob.ghostAt(100, 100 + RADIUS);
        this.enemies(centre, outer);

        tower.doTick(0);

        assertThat(effectsOf(centre, EffectKind.SATURATED)).singleElement().satisfies(effect -> {
            assertThat(effect.stacks()).isEqualTo(2);
            assertThat(effect.effectiveStackCap()).isEqualTo(4);
        });
        assertThat(effectsOf(outer, EffectKind.SATURATED)).singleElement().extracting(Effect::stacks).isEqualTo(1);
    }

    @Test
    void arcJumpsFromTheOutermostCaughtEnemyToTheNearestOneBeyondAtHalfTheBlastAsMagic() {
        SplashTower tower = this.upgradedTower("Arc");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob outermost = FakeEnemyMob.ghostAt(100, 100 + RADIUS - 2);
        FakeEnemyMob beyond = FakeEnemyMob.ghostAt(100, 100 + RADIUS + 30);
        FakeEnemyMob farther = FakeEnemyMob.ghostAt(100, 100 + RADIUS + 60);
        FakeEnemyMob tooFar = FakeEnemyMob.ghostAt(100, 100 + RADIUS + 200);
        this.enemies(centre, outermost, beyond, farther, tooFar);

        tower.doTick(0);

        assertThat(beyond.hits()).containsExactly(Damage.magic(Math.round(FULL * 0.5f)));
        assertThat(farther.hits()).containsExactly(Damage.magic(Math.round(FULL * 0.5f)));
        assertThat(tooFar.hits()).isEmpty();
        assertThat(beyond.attackers()).extracting(AttackProfile::critChance).containsExactly(0f);
        assertThat(tower.getArcs()).hasSize(2);
    }

    @Test
    void anArcCarriesHalfACellFurtherFromAnEnemyForEachSaturationStack() {
        SplashTower plain = this.upgradedTower("Arc");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob gap = FakeEnemyMob.ghostAt(100, 100 + 2.5 * BoardFixtures.SCALE);
        this.enemies(centre, gap);
        plain.doTick(0);
        SplashTower saturated = this.upgradedTower("Arc");
        FakeEnemyMob soaked = FakeEnemyMob.at(100, 100);
        saturate(soaked, 2);
        FakeEnemyMob reached = FakeEnemyMob.ghostAt(100, 100 + 2.5 * BoardFixtures.SCALE);
        this.enemies(soaked, reached);

        saturated.doTick(0);

        assertThat(gap.hits()).isEmpty();
        assertThat(reached.hits()).extracting(Damage::type).containsExactly(DamageType.MAGIC);
    }

    @Test
    void conductorLetsArcsCritAndJumpOnceMore() {
        SplashTower tower = this.upgradedTower("Arc", "Conductor");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        List<FakeEnemyMob> line = List.of(FakeEnemyMob.ghostAt(100, 160), FakeEnemyMob.ghostAt(100, 190),
                FakeEnemyMob.ghostAt(100, 220), FakeEnemyMob.ghostAt(100, 250));
        this.enemies(centre, line.get(0), line.get(1), line.get(2), line.get(3));

        tower.doTick(0);

        assertThat(tower.getArcs()).hasSize(3);
        assertThat(line.get(0).attackers()).extracting(AttackProfile::critChance).containsExactly(0.1f);
    }

    @Test
    void overloadDazesWhatAnArcCrits() {
        SplashTower tower = this.upgradedTower("Arc", "Conductor", "Overload");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob struck = FakeEnemyMob.ghostAt(100, 160);
        struck.landEveryHitCritical();
        this.enemies(centre, struck);

        tower.doTick(0);

        assertThat(effectsOf(struck, EffectKind.DAZED)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(Math.round(0.5f * TickRate.TICKS_PER_SECOND));
    }

    @Test
    void chainLightningArcsAtFullDamageAndItsThirdJumpForks() {
        SplashTower tower = this.upgradedTower("Arc", "Conductor", "Overload", "Transcendent", "Chain Lightning");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob first = FakeEnemyMob.ghostAt(100, 160);
        FakeEnemyMob second = FakeEnemyMob.ghostAt(100, 190);
        FakeEnemyMob forkA = FakeEnemyMob.ghostAt(85, 215);
        FakeEnemyMob forkB = FakeEnemyMob.ghostAt(115, 215);
        this.enemies(centre, first, second, forkA, forkB);

        tower.doTick(0);

        assertThat(first.hits()).containsExactly(Damage.magic(FULL));
        assertThat(forkA.hits()).hasSize(1);
        assertThat(forkB.hits()).hasSize(1);
    }

    @Test
    void lightningRodSendsArcsWithNowhereToGoBackIntoThePrimary() {
        SplashTower tower = this.upgradedTower("Arc", "Conductor", "Overload", "Transcendent", "Lightning Rod");
        FakeEnemyMob boss = FakeEnemyMob.at(100, 100);
        FakeEnemyMob escortA = FakeEnemyMob.ghostAt(90, 100);
        FakeEnemyMob escortB = FakeEnemyMob.ghostAt(110, 100);
        this.enemies(boss, escortA, escortB);

        tower.doTick(0);

        assertThat(boss.hits()).hasSize(4);
        assertThat(boss.hits().subList(1, 4)).allMatch(hit -> hit.equals(Damage.magic(FULL)));
    }

    @Test
    void potencyAddsAJumpAndRaisesTheArcShare() {
        SplashTower tower = this.upgradedTower("Arc", "Wide Charge", "Shaped Charge", "Potency");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        List<FakeEnemyMob> line = List.of(FakeEnemyMob.ghostAt(100, 170), FakeEnemyMob.ghostAt(100, 200),
                FakeEnemyMob.ghostAt(100, 230), FakeEnemyMob.ghostAt(100, 260));
        this.enemies(centre, line.get(0), line.get(1), line.get(2), line.get(3));

        tower.doTick(0);

        assertThat(tower.getArcs()).hasSize(3);
        assertThat(line.get(0).hits()).containsExactly(Damage.magic(Math.round(FULL * 0.65f)));
    }

    private static final String[] STORMCALLER = {"Arc", "Conductor", "Overload"};

    private SplashTower stormcallerWith(String special) {
        return this.upgradedTower(STORMCALLER[0], STORMCALLER[1], STORMCALLER[2], special);
    }

    @Test
    void thunderclapDischargesIntoEveryEnemyInRangeOnTheShotAfterAnArcCritButItsOwnCritsDoNotArmItAgain() {
        SplashTower tower = this.stormcallerWith("Thunderclap");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob arcTarget = FakeEnemyMob.ghostAt(100, 160);
        arcTarget.landEveryHitCritical();
        this.enemies(centre, arcTarget);
        tickThroughCooldown(tower, 1);
        this.enemies(centre);
        centre.landEveryHitCritical();

        tickThroughCooldown(tower, 1);
        List<Damage> secondShot = List.copyOf(centre.hits().subList(1, centre.hits().size()));
        tickThroughCooldown(tower, 1);
        List<Damage> thirdShot = centre.hits().subList(1 + secondShot.size(), centre.hits().size());

        assertThat(secondShot).extracting(Damage::type).containsExactly(DamageType.PHYSICAL, DamageType.MAGIC);
        assertThat(secondShot.get(1).amount()).isEqualTo(Math.round(FULL * 0.5f));
        assertThat(effectsOf(centre, EffectKind.DAZED)).isNotEmpty();
        assertThat(thirdShot).extracting(Damage::type).containsExactly(DamageType.PHYSICAL);
    }

    @Test
    void staticChargeChargesWhatTheArcsStrike() {
        SplashTower tower = this.stormcallerWith("Static Charge");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100);
        FakeEnemyMob arcTarget = FakeEnemyMob.ghostAt(100, 160);
        this.enemies(centre, arcTarget);

        tower.doTick(0);

        assertThat(effectsOf(arcTarget, EffectKind.CHARGED)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(Math.round(3f * TickRate.TICKS_PER_SECOND));
        assertThat(effectsOf(centre, EffectKind.CHARGED)).isEmpty();
    }

    @Test
    void everySixthShotThunderstrikesTheHealthiestForFourTimesTheBlastAndDazesIt() {
        SplashTower tower = this.stormcallerWith("Thunderstrike");
        FakeEnemyMob centre = FakeEnemyMob.at(100, 100).withHealth(100);
        FakeEnemyMob healthiest = FakeEnemyMob.at(140, 140).withHealth(900);
        this.enemies(centre, healthiest);

        tickThroughCooldown(tower, 6);

        assertThat(healthiest.hits()).contains(Damage.magic(FULL * 4));
        assertThat(effectsOf(healthiest, EffectKind.DAZED)).isNotEmpty();
        assertThat(healthiest.hits().stream().filter(hit -> hit.equals(Damage.magic(FULL * 4)))).hasSize(1);
    }

    /** Ticks until {@code tower} has fired {@code shots} more shots. */
    private static void tickThroughCooldown(SplashTower tower, int shots) {
        int tick = 0;
        for (int fired = 0; fired < shots; tick++) {
            if (tower.getCoolDownFraction() == 0f) {
                fired++;
            }
            tower.doTick(tick);
        }
    }

    @Test
    void aHexerCastsDoomOnTheHealthiestEnemyEveryFourthShotInsteadOfBlasting() {
        SplashTower tower = this.upgradedTower("Hex");
        FakeEnemyMob weak = FakeEnemyMob.at(100, 100).withHealth(100);
        FakeEnemyMob strong = FakeEnemyMob.at(70, 130).withHealth(900);
        this.enemies(weak, strong);

        tickThroughCooldown(tower, 3);
        int hitsBefore = strong.hits().size() + weak.hits().size();
        tickThroughCooldown(tower, 1);

        assertThat(effectsOf(strong, EffectKind.DOOM)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(Math.round(4f * TickRate.TICKS_PER_SECOND)
                        + Math.round(strong.effectStacks(EffectKind.SATURATED) * TickRate.TICKS_PER_SECOND));
        assertThat(strong.hits().size() + weak.hits().size()).isEqualTo(hitsBefore);
        assertThat(tower.getCasts()).hasSize(1);
    }

    @Test
    void whenEveryEnemyAlreadyCarriesDoomTheCastIsAPlainBlast() {
        SplashTower tower = this.upgradedTower("Hex");
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        enemy.applyEffect(Effect.hex(EffectKind.DOOM, 1000, d -> {
        }));
        this.enemies(enemy);

        tickThroughCooldown(tower, 4);

        assertThat(enemy.hits()).hasSize(4);
        assertThat(tower.getCasts()).isEmpty();
    }

    @Test
    void whenDoomRunsOutTheEnemyTakesThirtyPercentOfWhatItTookUnderItAsPeriodicMagic() {
        SplashTower tower = this.upgradedTower("Hex");
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);
        tickThroughCooldown(tower, 4);
        enemy.doDamage(Damage.physical(10_000), AttackProfile.none());
        int hitsBeforeEnd = enemy.hits().size();

        enemy.expire(EffectKind.DOOM);
        tower.doTick(1000);

        Damage payout = enemy.hits().get(hitsBeforeEnd);
        assertThat(payout).isEqualTo(Damage.magic(3_000));
        assertThat(enemy.attackers().get(hitsBeforeEnd).delivery().name()).isEqualTo("PERIODIC");
    }

    @Test
    void withWitchsBrewACastCursesItsTargetAndTheTwoMostSaturatedAroundIt() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        List<FakeEnemyMob> around = List.of(FakeEnemyMob.ghostAt(100, 110), FakeEnemyMob.ghostAt(110, 100),
                FakeEnemyMob.ghostAt(90, 100));
        this.enemies(target, around.get(0), around.get(1), around.get(2));

        tickThroughCooldown(tower, 4);

        long doomed = around.stream().filter(enemy -> enemy.hasEffect(EffectKind.DOOM)).count();
        assertThat(target.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(doomed).isEqualTo(2);
    }

    @Test
    void hexOfBlightPoisonsForTheHexsLength() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew");
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);

        tickThroughCooldown(tower, 8);

        int hexTicks = Math.round(6f * TickRate.TICKS_PER_SECOND);
        assertThat(effectsOf(enemy, EffectKind.BLIGHT)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(hexTicks);
        assertThat(effectsOf(enemy, EffectKind.POISON)).singleElement().satisfies(poison -> {
            assertThat(poison.damagePerTick()).isEqualTo(Damage.magic(Math.round(FULL * 0.04f)));
            assertThat(poison.remainingTicks()).isEqualTo(hexTicks);
        });
    }

    @Test
    void whenAContagionCarrierDiesItsCursesAndDebuffsJumpToTheNearestUnhexedEnemiesTwiceAtMost() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew", "Spreading Curse");
        FakeEnemyMob weak = FakeEnemyMob.at(100, 100).withHealth(100);
        FakeEnemyMob strong = FakeEnemyMob.at(130, 100).withHealth(900);
        this.enemies(weak, strong);
        tickThroughCooldown(tower, 12);
        FakeEnemyMob carrier = weak.hasEffect(EffectKind.CONTAGION) ? weak : strong;
        carrier.applyEffect(Effect.vulnerable(2, 80, d -> {
        }));
        FakeEnemyMob first = FakeEnemyMob.ghostAt(carrier.getX(), carrier.getY() + 20);
        FakeEnemyMob tooFar = FakeEnemyMob.ghostAt(carrier.getX(), carrier.getY() + 300);

        kill(carrier);
        this.enemies(weak, strong, first, tooFar);
        tower.doTick(2000);
        FakeEnemyMob second = FakeEnemyMob.ghostAt(first.getX(), first.getY() + 20);
        kill(first);
        this.enemies(weak, strong, first, second, tooFar);
        tower.doTick(2001);
        FakeEnemyMob third = FakeEnemyMob.ghostAt(second.getX(), second.getY() + 20);
        kill(second);
        this.enemies(weak, strong, first, second, third, tooFar);
        tower.doTick(2002);

        assertThat(first.activeEffectKinds()).contains(EffectKind.CONTAGION, EffectKind.VULNERABLE);
        assertThat(second.activeEffectKinds()).contains(EffectKind.CONTAGION, EffectKind.VULNERABLE);
        assertThat(third.activeEffectKinds()).isEmpty();
        assertThat(tooFar.activeEffectKinds()).isEmpty();
    }

    private static void kill(FakeEnemyMob enemy) {
        enemy.dieOnAnyHit();
        enemy.doDamage(Damage.physical(1), AttackProfile.none());
    }

    @Test
    void onAHexerPotencyRaisesDoomsPayoutToFortyFivePercent() {
        SplashTower tower = this.upgradedTower("Hex", "Wide Charge", "Shaped Charge", "Potency");
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);
        tickThroughCooldown(tower, 4);
        enemy.doDamage(Damage.physical(10_000), AttackProfile.none());
        int hitsBeforeEnd = enemy.hits().size();

        enemy.expire(EffectKind.DOOM);
        tower.doTick(1000);

        assertThat(enemy.hits().get(hitsBeforeEnd)).isEqualTo(Damage.magic(4_500));
    }

    @Test
    void rimeCovenAddsHexOfRimeWhichChillsThirtyPercentAsItIsCast() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew", "Spreading Curse", "Transcendent", "Rime Coven");
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.enemies(enemy);

        tickThroughCooldown(tower, 16);

        assertThat(effectsOf(enemy, EffectKind.RIME)).hasSize(1);
        assertThat(effectsOf(enemy, EffectKind.CHILL)).singleElement().extracting(Effect::fuelLevel).isEqualTo(0.3f);
    }

    private SplashTower hexerWith(String special) {
        return this.upgradedTower("Hex", special);
    }

    @Test
    void hexOfInversionTargetsAnEnemyThatHealsShieldsOrVanishesAndNoOtherEnemy() {
        SplashTower tower = this.hexerWith("Hex of Inversion");
        FakeEnemyMob plain = FakeEnemyMob.at(100, 100).withHealth(900);
        FakeEnemyMob mender = FakeEnemyMob.at(100, 130).withHealth(100).thatAppliesEffects();
        this.enemies(plain, mender);

        tickThroughCooldown(tower, 8);

        assertThat(plain.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(plain.hasEffect(EffectKind.INVERSION)).isFalse();
        assertThat(effectsOf(mender, EffectKind.INVERSION)).singleElement().extracting(Effect::remainingTicks)
                .isEqualTo(Math.round(6f * TickRate.TICKS_PER_SECOND));
    }

    @Test
    void withNoEnemyThatAppliesEffectsHexOfInversionIsSkipped() {
        SplashTower tower = this.hexerWith("Hex of Inversion");
        FakeEnemyMob plain = FakeEnemyMob.at(100, 100);
        this.enemies(plain);

        tickThroughCooldown(tower, 12);

        assertThat(plain.hasEffect(EffectKind.INVERSION)).isFalse();
    }

    @Test
    void hexOfSympathyCopiesTheCarriersDebuffsToTheOtherHexedEnemiesNearbyOnceASecond() {
        SplashTower tower = this.hexerWith("Hex of Sympathy");
        FakeEnemyMob strong = FakeEnemyMob.at(100, 100).withHealth(900);
        FakeEnemyMob carrier = FakeEnemyMob.at(100, 140).withHealth(100).withProgression(5);
        FakeEnemyMob unhexed = FakeEnemyMob.ghostAt(100, 120);
        this.enemies(strong, carrier);
        tickThroughCooldown(tower, 8);
        this.enemies(strong, carrier, unhexed);
        carrier.applyEffect(Effect.sundered(2, 100, d -> {
        }));

        tower.doTick(2001);
        int beforeTheSecond = strong.effectStacks(EffectKind.SUNDERED);
        tower.doTick(2000);

        assertThat(carrier.hasEffect(EffectKind.SYMPATHY)).isTrue();
        assertThat(beforeTheSecond).isZero();
        assertThat(strong.effectStacks(EffectKind.SUNDERED)).isEqualTo(2);
        assertThat(unhexed.hasEffect(EffectKind.SUNDERED)).isFalse();
    }

    @Test
    void hexOfSympathyDoesNotAddAgainWhatTheOtherAlreadyHas() {
        SplashTower tower = this.hexerWith("Hex of Sympathy");
        FakeEnemyMob strong = FakeEnemyMob.at(100, 100).withHealth(900);
        FakeEnemyMob carrier = FakeEnemyMob.at(100, 140).withHealth(100).withProgression(5);
        this.enemies(strong, carrier);
        tickThroughCooldown(tower, 8);
        carrier.applyEffect(Effect.sundered(2, 100, d -> {
        }));

        tower.doTick(2000);
        tower.doTick(2020);
        tower.doTick(2040);

        assertThat(strong.effectStacks(EffectKind.SUNDERED)).isEqualTo(2);
        assertThat(carrier.effectStacks(EffectKind.SUNDERED)).isEqualTo(2);
    }

    @Test
    void whenAReckoningCarrierDiesTheDoomsNearItPayOutAtOnceAndStartAgain() {
        SplashTower tower = this.hexerWith("Hex of Reckoning");
        FakeEnemyMob doomed = FakeEnemyMob.at(100, 100).withHealth(900);
        FakeEnemyMob carrier = FakeEnemyMob.at(100, 140).withHealth(100).withProgression(5);
        this.enemies(doomed, carrier);
        tickThroughCooldown(tower, 8);
        doomed.doDamage(Damage.physical(10_000), AttackProfile.none());
        int hitsBefore = doomed.hits().size();

        kill(carrier);
        tower.doTick(2000);
        tower.doTick(2001);

        assertThat(carrier.hasEffect(EffectKind.RECKONING)).isTrue();
        List<Damage> payouts = doomed.hits().subList(hitsBefore, doomed.hits().size());
        assertThat(payouts).singleElement().satisfies(payout -> {
            assertThat(payout.type()).isEqualTo(DamageType.MAGIC);
            assertThat(payout.amount()).isGreaterThanOrEqualTo(3_000);
        });
        assertThat(effectsOf(doomed, EffectKind.DOOM)).hasSize(2);
    }

    @Test
    void aKillByReckoningsPayoutChainsToTheNextDoom() {
        SplashTower tower = this.hexerWith("Hex of Reckoning");
        FakeEnemyMob first = FakeEnemyMob.at(100, 100).withHealth(900);
        FakeEnemyMob second = FakeEnemyMob.at(100, 130).withHealth(500).withProgression(1);
        FakeEnemyMob third = FakeEnemyMob.at(100, 160).withHealth(100).withProgression(2);
        this.enemies(first, second, third);
        tickThroughCooldown(tower, 16);
        assertThat(first.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(second.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(third.hasEffect(EffectKind.RECKONING)).isTrue();
        FakeEnemyMob link = second.hasEffect(EffectKind.RECKONING) ? second : first;
        FakeEnemyMob end = link == second ? first : second;
        link.dieOnAnyHit();

        kill(third);
        tower.doTick(2000);
        tower.doTick(2001);

        assertThat(link.isDead()).isTrue();
        assertThat(end.hits().stream().filter(hit -> hit.type() == DamageType.MAGIC)).hasSize(2);
    }

    @Test
    void potencyAndMasteryWaitForAChainRoot() {
        SplashTower tower = this.upgradedTower("Wide Charge", "Shaped Charge");

        assertThat(tower.offeredUpgrades(this.context)).extracting(node -> node.displayName())
                .doesNotContain("Potency");
    }

    @Test
    void theForkNamesTheTower() {
        SplashTower plain = this.tower();
        SplashTower stormcaller = this.upgradedTower("Arc");
        SplashTower hexer = this.upgradedTower("Hex");

        assertThat(plain.inspect().name()).isEqualTo("Burst");
        assertThat(stormcaller.inspect().name()).isEqualTo("Stormcaller");
        assertThat(hexer.inspect().name()).isEqualTo("Hexer");
    }

    private SplashTower mineLayer() {
        return this.upgradedTower("Arc", "Conductor", "Overload", "Transcendent", "Range", "Range II", "Range III");
    }

    private List<td.zone.Zone> mines() {
        return this.context.zones().zones().stream().filter(zone -> zone.kind() == td.zone.ZoneKind.MINE).toList();
    }

    @Test
    void everyFourthBlastLeavesAMineOnThePathWhereItLandedOnceRangeThreeIsOwned() {
        SplashTower tower = this.mineLayer();
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.enemies(target);

        tickThroughCooldown(tower, 3);
        int afterThree = this.mines().size();
        tickThroughCooldown(tower, 1);

        assertThat(afterThree).isZero();
        assertThat(this.mines()).singleElement().satisfies(mine -> {
            assertThat(mine.x()).isEqualTo(100.0);
            assertThat(mine.y()).isEqualTo(100.0);
        });
    }

    @Test
    void withoutRangeThreeABlastLeavesNoMine() {
        SplashTower tower = this.upgradedTower("Arc", "Conductor", "Overload", "Transcendent");
        this.enemies(FakeEnemyMob.at(100, 100));

        tickThroughCooldown(tower, 8);

        assertThat(this.mines()).isEmpty();
    }

    @Test
    void theNextEnemyToStepOnAMineSetsItOffAsOneBlastAndTheMineIsGone() {
        SplashTower tower = this.mineLayer();
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.enemies(target);
        tickThroughCooldown(tower, 4);
        target.moveTo(2000, 2000);
        FakeEnemyMob walker = FakeEnemyMob.ghostAt(100, 105);
        FakeEnemyMob bystander = FakeEnemyMob.ghostAt(100, 130);
        this.enemies(target, walker, bystander);

        this.context.zones().doTick(1);

        assertThat(walker.hits()).isNotEmpty();
        assertThat(bystander.hits()).isNotEmpty();
        assertThat(this.mines()).isEmpty();
    }

    @Test
    void aMineNobodyStepsOnEndsAfterTenSeconds() {
        SplashTower tower = this.mineLayer();
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.enemies(target);
        tickThroughCooldown(tower, 4);
        target.moveTo(2000, 2000);

        for (int t = 1; t < 199; t++) {
            this.context.zones().doTick(t);
        }
        int stillThere = this.mines().size();
        this.context.zones().doTick(199);
        this.context.zones().doTick(200);

        assertThat(stillThere).isEqualTo(1);
        assertThat(this.mines()).isEmpty();
    }

    @Test
    void aSplashKeepsAtMostThreeMinesAndAFourthReplacesTheOldest() {
        SplashTower tower = this.mineLayer();
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.enemies(target);

        for (int laid = 0; laid < 4; laid++) {
            target.moveTo(70 + 40 * laid, 100);
            tickThroughCooldown(tower, 4);
        }

        assertThat(this.mines()).extracting(mine -> mine.x()).containsExactly(110.0, 150.0, 190.0);
    }

    @Test
    void aSoldSplashTakesItsMinesWithIt() {
        SplashTower tower = this.mineLayer();
        this.enemies(FakeEnemyMob.at(100, 100));
        tickThroughCooldown(tower, 4);

        tower.doCleanup();

        assertThat(this.mines()).isEmpty();
    }

    @Test
    void masteryMakesAHexedEnemyThatDiesLeaveACloudThatCursesWhatStandsInIt() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew", "Spreading Curse", "Transcendent", "Wide Charge",
                "Shaped Charge", "Potency", "Mastery");
        FakeEnemyMob carrier = FakeEnemyMob.at(100, 100);
        this.enemies(carrier);
        tickThroughCooldown(tower, 4);
        carrier.applyEffect(Effect.vulnerable(2, 80, d -> {
        }));
        FakeEnemyMob bystander = FakeEnemyMob.ghostAt(100, 120);

        kill(carrier);
        this.enemies(carrier, bystander);
        tower.doTick(3000);
        this.context.zones().doTick(1);

        assertThat(this.context.zones().zones()).extracting(td.zone.Zone::kind).contains(td.zone.ZoneKind.CURSED_CLOUD);
        assertThat(bystander.activeEffectKinds()).contains(EffectKind.VULNERABLE);
    }

    @Test
    void withoutMasteryAHexedEnemyThatDiesLeavesNoCloud() {
        SplashTower tower = this.upgradedTower("Hex", "Witch's Brew", "Spreading Curse");
        FakeEnemyMob carrier = FakeEnemyMob.at(100, 100);
        this.enemies(carrier);
        tickThroughCooldown(tower, 4);
        carrier.applyEffect(Effect.vulnerable(2, 80, d -> {
        }));

        kill(carrier);
        tower.doTick(3000);

        assertThat(this.context.zones().zones()).isEmpty();
    }
}
