package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.stat.StatModifiers;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AuraTowerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    private void addClusterFiller() {
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        this.context.towers().add(new SniperTower(this.context, 0, 0));
    }

    @Test
    void amplifyingCoreIncreasesTheBuffANeighbourReceives() {
        SniperTower neighbour = new SniperTower(this.context, 0, 0);
        this.context.towers().add(neighbour);
        this.addClusterFiller();
        AuraTower aura = this.auraWith("Awaken");
        int damageBeforeUpgrade = neighbour.damageCurrent();

        UpgradePaths.buy(aura, this.context, "Amplifying Core");

        assertThat(neighbour.damageCurrent()).isGreaterThan(damageBeforeUpgrade);
    }

    @Test
    void anAuraNeverBuffsAnotherAuraEvenWithBroadcast() {
        AuraTower first = this.auraWith("Broadcast");
        AuraTower second = new AuraTower(this.context, 0, 0);
        this.context.towers().add(second);

        assertThat(first.buffFor(second)).isEqualTo(StatModifiers.none());
    }

    @Test
    void onAmplifyingCoreKinshipStrengthensTheBuffForEachOtherTowerTypeInRange() {
        AuraTower aura = this.auraWith("Amplifying Core");
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        float alone = aura.buffFor(sniper).get(TowerStat.DAMAGE).percentAdd();

        this.context.towers().add(new SplashTower(this.context, 0, 0));
        this.context.towers().add(new MortarTower(this.context, 0, 0));

        assertThat(aura.buffFor(sniper).get(TowerStat.DAMAGE).percentAdd()).isGreaterThan(alone);
    }

    @Test
    void anAurasEmissionNeverIncludesABlastRadius() {
        AuraTower aura = this.auraWith("Broadcast II", "Conduit", "Withering Field");
        SplashTower splash = new SplashTower(this.context, 0, 0);
        this.context.towers().add(splash);

        assertThat(aura.buffFor(splash).stats()).isNotEmpty().doesNotContain(TowerStat.AREA_RADIUS);
    }

    private AuraTower auraWith(String... nodes) {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.buy(aura, this.context, nodes);
        return aura;
    }

    @Test
    void broadcastWidensTheAurasOwnReach() {
        AuraTower aura = this.auraWith("Awaken");
        float before = aura.getRangeReal();

        UpgradePaths.buy(aura, this.context, "Broadcast");

        assertThat(aura.getRangeReal()).isGreaterThan(before);
    }

    @Test
    void towersInRangeOfAnAuraTakeLessDisruptionFromTheStart() {
        this.context.towers().add(new AuraTower(this.context, 0, 0));
        SniperTower shielded = new SniperTower(this.context, 0, 0);
        SniperTower exposed = new SniperTower(this.context, 30, 30);
        this.context.towers().add(shielded);
        this.context.towers().add(exposed);
        DisruptionAura jammer = new DisruptionAura(20f, 0.4f, 0.2f);
        this.context.disruptions().add(shielded.getX(), shielded.getY(), jammer);
        this.context.disruptions().add(exposed.getX(), exposed.getY(), jammer);

        shielded.beginTick(1);
        exposed.beginTick(1);

        assertThat(exposed.attackRate()).isLessThan(1.0);
        assertThat(shielded.attackRate()).isGreaterThan(exposed.attackRate());
    }

    @Test
    void aTowerGivenAWitherIntervalMakesItsNextHitApplyVulnerableOnceEachInterval() {
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        TowerFixtures.buffed(sniper, this.context, TowerModifiers.witherEvery(200));
        FakeEnemyMob target = FakeEnemyMob.at(sniper.getX(), sniper.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int t = 1; t <= 260; t++) {
            sniper.beginTick(t);
            sniper.doTick(t);
        }

        assertThat(target.hits().size()).isGreaterThan(4);
        assertThat(target.appliedEffects()).extracting(Effect::kind)
                .containsExactly(EffectKind.VULNERABLE, EffectKind.VULNERABLE);
    }

    @Test
    void aTowerWithoutAWitherIntervalNeverWithers() {
        SniperTower alone = new SniperTower(this.context, 30, 30);
        this.context.towers().add(alone);
        FakeEnemyMob target = FakeEnemyMob.at(alone.getX(), alone.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int t = 1; t <= 60; t++) {
            alone.beginTick(t);
            alone.doTick(t);
        }

        assertThat(target.appliedEffects()).isEmpty();
    }

    @Test
    void chosenBuffsOnlyTheMostExperiencedTowerInRangeAtTripleStrength() {
        AuraTower aura = this.auraWith("Chosen");
        SniperTower veteran = new SniperTower(this.context, 0, 0);
        SniperTower novice = new SniperTower(this.context, 0, 0);
        this.context.towers().add(veteran);
        this.context.towers().add(novice);
        veteran.earnXp(500);
        float plain = new AuraTower(this.context, 0, 0).buffFor(veteran).get(TowerStat.DAMAGE).percentAdd();

        assertThat(aura.buffFor(veteran).get(TowerStat.DAMAGE).percentAdd()).isCloseTo(3 * plain, within(1e-6f));
        assertThat(aura.buffFor(novice)).isEqualTo(StatModifiers.none());
        assertThat(aura.buffedTowers()).containsExactly(veteran);
    }

    @Test
    void chosenFollowsTheTowerThatOvertakesTheHero() {
        AuraTower aura = this.auraWith("Chosen");
        SniperTower first = new SniperTower(this.context, 0, 0);
        SniperTower second = new SniperTower(this.context, 0, 0);
        this.context.towers().add(first);
        this.context.towers().add(second);
        first.earnXp(100);
        aura.doTick(1);
        int firstBuffed = first.damageCurrent();

        second.earnXp(500);
        aura.doTick(2);

        assertThat(firstBuffed).isGreaterThan(first.damageBase);
        assertThat(first.damageCurrent()).isEqualTo(first.damageBase);
        assertThat(second.damageCurrent()).isGreaterThan(second.damageBase);
    }

    private AuraTower rallyAura() {
        this.context.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(900, 0))));
        return this.auraWith("Rally");
    }

    @Test
    void rallyMarksThePathSpotNearestTheAura() {
        AuraTower aura = this.rallyAura();

        assertThat(aura.rallySpot()).hasValueSatisfying(spot -> {
            assertThat(spot.x()).isCloseTo(aura.getX(), within(1.0));
            assertThat(spot.y()).isCloseTo(0.0, within(1.0));
        });
    }

    @Test
    void anEnemyOnTheRallySpotGivesBuffedTowersMoreAttackSpeedForFiveSecondsThenTenSecondsOfCooldown() {
        AuraTower aura = this.rallyAura();
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(aura.getX(), 0)});

        aura.doTick(1);
        double during = sniper.attackRate();
        sniper.beginTick(101);
        aura.doTick(101);
        double afterBuff = sniper.attackRate();
        sniper.beginTick(201);
        aura.doTick(201);
        double secondRally = sniper.attackRate();

        assertThat(during).isCloseTo(1.3, within(1e-6));
        assertThat(afterBuff).isEqualTo(1.0);
        assertThat(secondRally).isCloseTo(1.3, within(1e-6));
    }

    @Test
    void rallyDoesNothingWithoutAnEnemyOnTheSpot() {
        AuraTower aura = this.rallyAura();
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        this.context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(aura.getX(), 500)});

        aura.doTick(1);

        assertThat(sniper.attackRate()).isEqualTo(1.0);
    }

    @Test
    void aBuffedTowerCarriesAuraWithWhatItReceives() {
        SniperTower buffed = new SniperTower(this.context, 0, 0);
        this.context.towers().add(buffed);
        this.context.towers().add(new AuraTower(this.context, 0, 0));

        assertThat(buffed.conditions()).singleElement().satisfies(state -> {
            assertThat(state.term()).isEqualTo(TowerTerm.AURA);
            assertThat(state.stacks()).isZero();
        });
    }

    @Test
    void aJammedTowerCarriesJammedAsADebuffAfterItsBuffs() {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        SniperTower jammed = new SniperTower(this.context, 0, 0);
        this.context.towers().add(jammed);
        this.context.disruptions().add(jammed.getX(), jammed.getY(), new DisruptionAura(20f, 0.4f, 0.2f));
        jammed.beginTick(1);

        assertThat(jammed.conditions()).extracting(TowerBuffState::term)
                .containsExactly(TowerTerm.AURA, TowerTerm.JAMMED);
    }

    @Test
    void aBeaconAppliesItsAuraAndRallyToTowers() {
        AuraTower aura = this.rallyAura();

        assertThat(aura.applies()).extracting(AppliedEffect::term).containsExactly(TowerTerm.AURA, TowerTerm.RALLY);
        assertThat(aura.applies().getLast().sources()).extracting(AppliedEffect.Source::name).containsExactly("Rally");
    }
}
