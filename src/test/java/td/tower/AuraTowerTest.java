package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.stat.StatModifiers;
import td.tower.aura.BreakthroughMechanic;
import td.tower.aura.KinshipMechanic;
import td.tower.aura.MentorMechanic;
import td.tower.aura.PrismMark;
import td.tower.splash.StaticMechanic;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AuraTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    private AuraTower beaconWith(String... nodes) {
        AuraTower beacon = new AuraTower(this.context, 0, 0);
        this.context.towers().add(beacon);
        if (nodes.length > 0) {
            UpgradePaths.buy(beacon, this.context, nodes);
        }
        return beacon;
    }

    private <T extends Tower> T place(T tower) {
        this.context.towers().add(tower);
        return tower;
    }

    private float outside() {
        return 3 * AuraTower.RANGE * this.context.getBoard().scale();
    }

    private void enemies(EnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    @Test
    void aTowerInTheLightTakesLessJamThanOneOutsideIt() {
        this.beaconWith();
        SniperTower lit = this.place(new SniperTower(this.context, 0, 0));
        SniperTower dark = this.place(new SniperTower(this.context, (int) this.outside(), 0));
        DisruptionAura jammer = new DisruptionAura(20f, 0.4f, 0.2f);
        this.context.disruptions().add(lit.getX(), lit.getY(), jammer);
        this.context.disruptions().add(dark.getX(), dark.getY(), jammer);

        lit.beginTick(1);
        dark.beginTick(1);

        assertThat(dark.attackRate()).isLessThan(1.0);
        assertThat(lit.attackRate()).isGreaterThan(dark.attackRate());
    }

    @Test
    void twoLightsOverATowerStack() {
        AuraTower first = this.beaconWith();
        SniperTower lit = this.place(new SniperTower(this.context, 0, 0));
        float underOne = lit.stats().value(TowerStat.DISRUPTION_TAKEN);

        this.place(new AuraTower(this.context, 0, 0));

        assertThat(underOne).isEqualTo(first.light().get(TowerStat.DISRUPTION_TAKEN).multiply());
        assertThat(lit.stats().value(TowerStat.DISRUPTION_TAKEN)).isCloseTo(underOne * underOne, within(1e-6f));
    }

    @Test
    void aBeaconNeverLightsAnotherBeaconOrItself() {
        AuraTower first = this.beaconWith();
        AuraTower second = this.place(new AuraTower(this.context, 0, 0));

        assertThat(first.buffFor(second)).isEqualTo(StatModifiers.none());
        assertThat(first.buffFor(first)).isEqualTo(StatModifiers.none());
    }

    @Test
    void litTowersAreTheOthersWhoseCentreIsInTheLightInTheOrderBuilt() {
        AuraTower beacon = this.beaconWith();
        SniperTower near = this.place(new SniperTower(this.context, 0, 0));
        this.place(new SniperTower(this.context, (int) this.outside(), 0));
        this.place(new AuraTower(this.context, 0, 0));
        SplashTower later = this.place(new SplashTower(this.context, 0, 0));

        assertThat(beacon.litTowers()).containsExactly(near, later);
    }

    @Test
    void sellingTheBeaconTakesItsLightAway() {
        this.context.economy().startEconomy(100, 5);
        SniperTower lit = this.place(new SniperTower(this.context, 0, 0));
        AuraTower beacon = this.place(new AuraTower(this.context, 0, 0));

        this.context.towers().sell(beacon);

        assertThat(lit.stats().value(TowerStat.DISRUPTION_TAKEN)).isEqualTo(1f);
    }

    @Test
    void theLightRevealsEveryEnemyInsideForItsRevealTimeAndNoneOutside() {
        AuraTower beacon = this.beaconWith();
        FakeEnemyMob inside = FakeEnemyMob.ghostAt(0, 0);
        FakeEnemyMob beyond = FakeEnemyMob.ghostAt(this.outside(), 0);
        this.enemies(inside, beyond);

        beacon.doTick(1);

        assertThat(inside.appliedEffects()).filteredOn(effect -> effect.kind() == EffectKind.REVEALED)
                .extracting(Effect::authoredDurationTicks)
                .containsExactly(Math.round(beacon.stats().value(TowerStat.REVEAL_TIME)));
        assertThat(beyond.appliedEffects()).isEmpty();
        assertThat(beacon.revealedNow()).containsExactly(inside);
    }

    @Test
    void theSpotlightHoldsItsEnemyUntilItLeavesTheLightThenPicksAgain() {
        AuraTower beacon = this.beaconWith("Last Light");
        FakeEnemyMob first = FakeEnemyMob.at(0, 0).withDistanceToExit(100);
        FakeEnemyMob second = FakeEnemyMob.at(0, 0).withDistanceToExit(200);
        this.enemies(first, second);
        beacon.doTick(1);
        second.withDistanceToExit(50);

        beacon.doTick(2);
        EnemyMob heldWhileInside = beacon.heldEnemy().orElseThrow();
        first.moveTo(this.outside(), 0);
        beacon.doTick(3);

        assertThat(heldWhileInside).isSameAs(first);
        assertThat(beacon.heldEnemy()).containsSame(second);
        assertThat(second.hasEffect(EffectKind.PRIORITY)).isTrue();
    }

    @Test
    void theSearchlightHoldsTheHighestRankThenTheMostHealth() {
        AuraTower beacon = this.beaconWith("Searchlight");
        FakeEnemyMob grunt = FakeEnemyMob.at(0, 0).withHealth(900);
        FakeEnemyMob weakElite = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withHealth(100);
        FakeEnemyMob strongElite = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withHealth(200);
        this.enemies(grunt, weakElite, strongElite);

        beacon.doTick(1);

        assertThat(beacon.heldEnemy()).containsSame(strongElite);
    }

    @Test
    void theRingleaderHoldsAnEnemyThatLeadsItsAlliesBeforeThePack() {
        AuraTower beacon = this.beaconWith("Ringleader");
        FakeEnemyMob leader = FakeEnemyMob.at(this.context.getBoard().scale(), 0).leading();
        this.enemies(FakeEnemyMob.at(0, 0), FakeEnemyMob.at(0, 0), leader);

        beacon.doTick(1);

        assertThat(beacon.heldEnemy()).containsSame(leader);
    }

    @Test
    void withoutASpotlightTheLightHoldsNoOne() {
        AuraTower beacon = this.beaconWith();
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        this.enemies(enemy);

        beacon.doTick(1);

        assertThat(beacon.heldEnemy()).isEmpty();
        assertThat(enemy.hasEffect(EffectKind.PRIORITY)).isFalse();
    }

    @Test
    void glareExposesEveryEnemyInTheLight() {
        AuraTower beacon = this.beaconWith("Glare");
        FakeEnemyMob inside = FakeEnemyMob.at(0, 0);
        FakeEnemyMob beyond = FakeEnemyMob.at(this.outside(), 0);
        this.enemies(inside, beyond);

        beacon.doTick(1);

        assertThat(inside.hasEffect(EffectKind.EXPOSED)).isTrue();
        assertThat(beyond.hasEffect(EffectKind.EXPOSED)).isFalse();
    }

    @Test
    void daylightLaysItsLightOnTheWorldForEnemiesToRead() {
        AuraTower beacon = this.beaconWith("Daylight");

        beacon.doTick(1);

        assertThat(this.context.daylight().covers(0, 0)).isTrue();
        assertThat(this.context.daylight().covers(this.outside(), 0)).isFalse();
    }

    @Test
    void aLightWithoutDaylightLaysNone() {
        AuraTower beacon = this.beaconWith();

        beacon.doTick(1);

        assertThat(this.context.daylight().covers(0, 0)).isFalse();
    }

    @Test
    void prismCarriesOneMarkForEachLitTowerTypeHoweverManyAreLit() {
        AuraTower beacon = this.beaconWith("Prism");
        this.place(new SniperTower(this.context, 0, 0));
        this.place(new SniperTower(this.context, 0, 0));
        this.place(new MortarTower(this.context, 0, 0));
        FakeEnemyMob inside = FakeEnemyMob.at(0, 0);
        this.enemies(inside);

        beacon.doTick(1);
        beacon.doTick(2);

        assertThat(beacon.prismMarks()).containsExactly(PrismMark.SUNDER, PrismMark.CRACK);
        assertThat(inside.effectStacks(EffectKind.SUNDERED)).isEqualTo(PrismMark.SUNDER.stacks());
        assertThat(inside.hasEffect(EffectKind.CRACKED)).isTrue();
    }

    @Test
    void prismAddsItsStackOnTopOfWhatAnEnemyEnteringCarriesOnceAndNotEveryTick() {
        AuraTower beacon = this.beaconWith("Prism");
        this.place(new SniperTower(this.context, 0, 0));
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        enemy.applyEffect(Effect.sundered(2, 100, DamageSink.none()));
        this.enemies(enemy);

        beacon.doTick(1);
        beacon.doTick(2);

        assertThat(enemy.effectStacks(EffectKind.SUNDERED)).isEqualTo(2 + PrismMark.SUNDER.stacks());
    }

    @Test
    void eachLightAnEnemyEntersAddsItsOwnStack() {
        AuraTower first = this.beaconWith("Prism");
        AuraTower second = this.place(new AuraTower(this.context, 0, 0));
        UpgradePaths.buy(second, this.context, "Prism");
        this.place(new SniperTower(this.context, 0, 0));
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        this.enemies(enemy);

        first.doTick(1);
        second.doTick(1);

        assertThat(enemy.effectStacks(EffectKind.SUNDERED)).isEqualTo(2 * PrismMark.SUNDER.stacks());
    }

    @Test
    void withoutPrismTheLightCarriesNoMark() {
        AuraTower beacon = this.beaconWith();
        this.place(new SniperTower(this.context, 0, 0));

        assertThat(beacon.prismMarks()).isEmpty();
    }

    @Test
    void eachLightsStaticDischargesOnceTheEnemyIsBadlyHurt() {
        AuraTower beacon = this.beaconWith("Prism");
        AuraTower second = this.place(new AuraTower(this.context, 0, 0));
        UpgradePaths.buy(second, this.context, "Prism");
        SplashTower stormcaller = this.place(new SplashTower(this.context, 0, 0));
        UpgradePaths.buy(stormcaller, this.context, "Stormcaller");
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        this.enemies(enemy);
        beacon.doTick(1);
        second.doTick(1);
        boolean hitWhileHealthy = !enemy.hits().isEmpty();

        enemy.atHealthFraction(StaticMechanic.DISCHARGE_AT);
        beacon.doTick(2);
        second.doTick(2);

        assertThat(hitWhileHealthy).isFalse();
        assertThat(enemy.hits()).hasSize(2);
    }

    @Test
    void breakthroughTeachesEveryOtherLitTowerWhenOneOpensATier() {
        AuraTower beacon = this.beaconWith("Breakthrough");
        SniperTower opener = this.place(new SniperTower(this.context, 0, 0));
        SniperTower other = this.place(new SniperTower(this.context, 0, 0));
        SniperTower dark = this.place(new SniperTower(this.context, (int) this.outside(), 0));
        this.context.playtestRules().setUpgradeGatesIgnored(false);
        opener.beginTick(1);
        opener.earnXp(opener.upgradeTree().tiers().get(1).xp());
        opener.beginTick(2);

        beacon.doTick(2);
        beacon.doTick(3);

        assertThat(other.experience().xp()).isEqualTo(BreakthroughMechanic.XP);
        assertThat(dark.experience().xp()).isZero();
        assertThat(opener.experience().xp()).isEqualTo(opener.upgradeTree().tiers().get(1).xp());
    }

    @Test
    void mentorMarksTheLitTowerWithTheLeastXp() {
        AuraTower beacon = this.beaconWith("Mentor");
        SniperTower veteran = this.place(new SniperTower(this.context, 0, 0));
        SniperTower novice = this.place(new SniperTower(this.context, 0, 0));
        veteran.earnXp(50);

        assertThat(beacon.xpBonusFor(novice)).isEqualTo(MentorMechanic.XP);
        assertThat(beacon.xpBonusFor(veteran)).isZero();
    }

    @Test
    void kinshipCountsTheOtherTowerTypesInTheLight() {
        AuraTower beacon = this.beaconWith("Kinship");
        SniperTower sniper = this.place(new SniperTower(this.context, 0, 0));
        this.place(new SniperTower(this.context, 0, 0));
        this.place(new SplashTower(this.context, 0, 0));
        this.place(new MortarTower(this.context, 0, 0));
        this.place(new MortarTower(this.context, (int) this.outside(), 0));

        assertThat(beacon.xpBonusFor(sniper)).isEqualTo(KinshipMechanic.xpFor(2));
    }

    @Test
    void aLitTowerCarriesLitAndAJammedOneJammedAfterIt() {
        this.beaconWith();
        SniperTower jammed = this.place(new SniperTower(this.context, 0, 0));
        this.context.disruptions().add(jammed.getX(), jammed.getY(), new DisruptionAura(20f, 0.4f, 0.2f));
        jammed.beginTick(1);

        assertThat(jammed.conditions()).extracting(TowerBuffState::term)
                .containsExactly(TowerTerm.LIT, TowerTerm.JAMMED);
    }

    @Test
    void aBeaconAppliesItsRevealToEnemiesAndLitToTowers() {
        AuraTower beacon = this.beaconWith("Glare");

        assertThat(beacon.applies()).extracting(AppliedEffect::term)
                .containsExactly(EffectKind.REVEALED, TowerTerm.LIT, EffectKind.EXPOSED);
    }
}
