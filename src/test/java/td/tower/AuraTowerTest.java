package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

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
        this.context.economy().startEconomy(1000, 5);
        SniperTower neighbour = new SniperTower(this.context, 0, 0);
        this.context.towers().add(neighbour);
        this.addClusterFiller();
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        int damageBeforeUpgrade = neighbour.damageCurrent();
        UpgradePaths.awakenVeteran(aura);

        boolean chosen = aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));

        assertThat(chosen).isTrue();
        assertThat(neighbour.damageCurrent()).isGreaterThan(damageBeforeUpgrade);
    }

    @Test
    void amplifyingCoreIiAlsoGrantsAFireRateBonus() {
        this.context.economy().startEconomy(1000, 5);
        SniperTower neighbour = new SniperTower(this.context, 0, 0);
        this.context.towers().add(neighbour);
        this.addClusterFiller();
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.awakenVeteran(aura);
        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));

        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core II"));

        assertThat(neighbour.fireRateCurrent()).isGreaterThan(1.0);
    }

    @Test
    void anAuraNeverBuffsAnotherAuraEvenWithBroadcast() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower first = new AuraTower(this.context, 0, 0);
        this.context.towers().add(first);
        AuraTower second = new AuraTower(this.context, 0, 0);
        this.context.towers().add(second);
        UpgradePaths.awakenVeteran(first);
        first.buyUpgrade(UpgradePaths.named(first, "Broadcast"));

        assertThat(first.buffFor(second)).isEqualTo(TowerBuff.none());
    }

    @Test
    void amplifyingCoreAddsATenthToTheBuffPerLevelAndTheSecondAddsTenPercentFireRate() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        SniperTower neighbour = new SniperTower(this.context, 0, 0);
        this.context.towers().add(neighbour);
        UpgradePaths.awakenVeteran(aura);

        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));
        TowerBuff first = aura.buffFor(neighbour);
        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core II"));
        TowerBuff second = aura.buffFor(neighbour);

        assertThat(first.damageBonus()).isCloseTo(0.3f, within(1e-6f));
        assertThat(first.rangeBonus()).isCloseTo(0.3f, within(1e-6f));
        assertThat(first.fireRateBonus()).isZero();
        assertThat(second.damageBonus()).isCloseTo(0.4f, within(1e-6f));
        assertThat(second.fireRateBonus()).isCloseTo(0.1f, within(1e-6f));
    }

    @Test
    void onAmplifyingCoreKinshipAddsFivePercentBuffStrengthPerOtherTowerType() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        this.context.towers().add(new SplashTower(this.context, 0, 0));
        this.context.towers().add(new MortarTower(this.context, 0, 0));
        UpgradePaths.awakenVeteran(aura);

        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));

        assertThat(aura.buffFor(sniper).damageBonus()).isCloseTo(0.3f + 2 * 0.05f, within(1e-6f));
    }

    private AuraTower coreAuraBuffing(int towers) {
        this.context.economy().startEconomy(100_000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        for (int i = 0; i < towers; i++) {
            this.context.towers().add(new SniperTower(this.context, 0, 0));
        }
        UpgradePaths.buy(aura, this.context, "Amplifying Core", "Amplifying Core II");
        return aura;
    }

    @Test
    void keenEdgeWaitsForFourBuffedTowersAndAnotherAuraBeside() {
        AuraTower aura = this.coreAuraBuffing(3);
        UpgradeNode keenEdge = UpgradePaths.named(aura, "Keen Edge");
        this.context.towers().add(new AuraTower(this.context, 0, 0));

        boolean withThree = aura.buyUpgrade(keenEdge);
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        boolean withFour = aura.buyUpgrade(keenEdge);

        assertThat(withThree).isFalse();
        assertThat(withFour).isTrue();
    }

    @Test
    void keenEdgeNeedsAnotherAuraBesideEvenWithFourBuffedTowers() {
        AuraTower aura = this.coreAuraBuffing(4);

        assertThat(aura.buyUpgrade(UpgradePaths.named(aura, "Keen Edge"))).isFalse();
    }

    @Test
    void keenEdgeGivesBuffedTowersHalfAgainTheirCritDamage() {
        AuraTower aura = this.coreAuraBuffing(4);
        this.context.towers().add(new AuraTower(this.context, 0, 0));
        SniperTower buffed = (SniperTower) aura.buffedTowers().getFirst();
        float before = buffed.stats().attack().critMultiplier();

        aura.buyUpgrade(UpgradePaths.named(aura, "Keen Edge"));

        assertThat(buffed.stats().attack().critMultiplier()).isCloseTo(before + 0.5f, within(1e-6f));
    }

    private AuraTower broadcastAura(String... nodes) {
        this.context.economy().startEconomy(100_000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.buy(aura, this.context, nodes);
        return aura;
    }

    @Test
    void broadcastIiHalvesTheDisruptionAgainSoAQuarterIsLeft() {
        this.broadcastAura("Broadcast", "Broadcast II");
        SniperTower shielded = new SniperTower(this.context, 0, 0);
        this.context.towers().add(shielded);
        this.context.disruptions().add(shielded.getX(), shielded.getY(), new DisruptionAura(20f, 0.4f, 0.2f));

        shielded.beginTick(1);

        assertThat(shielded.fireRateCurrent()).isCloseTo(1.0 / 1.1, within(1e-6));
    }

    @Test
    void broadcastIiMakesATimedBuffOnABuffedTowerLastTwiceAsLong() {
        this.broadcastAura("Broadcast", "Broadcast II");
        SniperTower buffed = new SniperTower(this.context, 0, 0);
        this.context.towers().add(buffed);
        SniperTower alone = new SniperTower(this.context, 30, 30);
        this.context.towers().add(alone);
        TowerBuff burst = TowerBuff.fireRate(0.5f);

        buffed.grantTimedBuff(burst, 100);
        alone.grantTimedBuff(burst, 100);
        buffed.beginTick(150);
        alone.beginTick(150);

        assertThat(buffed.fireRateCurrent()).isGreaterThan(1.9);
        assertThat(alone.fireRateCurrent()).isEqualTo(1.0);
    }

    @Test
    void conduitWaitsForFourBuffedTowers() {
        AuraTower aura = this.broadcastAura("Broadcast", "Broadcast II");
        UpgradeNode conduit = UpgradePaths.named(aura, "Conduit");
        for (int i = 0; i < 3; i++) {
            this.context.towers().add(new SniperTower(this.context, 0, 0));
        }

        boolean withThree = aura.buyUpgrade(conduit);
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        boolean withFour = aura.buyUpgrade(conduit);

        assertThat(withThree).isFalse();
        assertThat(withFour).isTrue();
    }

    @Test
    void conduitLengthensWhatBuffedTowersPutOnEnemiesByThirtyPercent() {
        AuraTower aura = this.broadcastAura("Broadcast", "Broadcast II");
        SniperTower buffed = new SniperTower(this.context, 0, 0);
        this.context.towers().add(buffed);
        for (int i = 0; i < 3; i++) {
            this.context.towers().add(new SniperTower(this.context, 0, 0));
        }
        FakeEnemyMob plainTarget = FakeEnemyMob.at(0, 0);
        FakeEnemyMob lengthenedTarget = FakeEnemyMob.at(0, 0);
        buffed.applyStacks(plainTarget, EffectKind.VULNERABLE, 1);

        aura.buyUpgrade(UpgradePaths.named(aura, "Conduit"));
        buffed.applyStacks(lengthenedTarget, EffectKind.VULNERABLE, 1);

        int plain = plainTarget.appliedEffects().getFirst().remainingTicks();
        int lengthened = lengthenedTarget.appliedEffects().getFirst().remainingTicks();
        assertThat(lengthened).isEqualTo(Math.round(plain * 1.3f));
    }

    @Test
    void broadcastAddsThirtyPercentRangeToTheAurasOwnReach() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.awakenVeteran(aura);
        float before = aura.getRangeReal();

        aura.buyUpgrade(UpgradePaths.named(aura, "Broadcast"));

        assertThat(aura.getRangeReal()).isCloseTo(before * 1.3f / 1f, within(before * 0.01f));
    }

    @Test
    void theHeadChainsOfTheAuraExcludeEachOther() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.awakenVeteran(aura);
        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));

        assertThat(aura.offeredUpgrades(this.context)).extracting(UpgradeNode::displayName).doesNotContain("Broadcast");
    }

    @Test
    void towersInRangeOfAnAuraTakeHalfTheDisruptionFromTheStart() {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        SniperTower shielded = new SniperTower(this.context, 0, 0);
        this.context.towers().add(shielded);
        this.context.disruptions().add(shielded.getX(), shielded.getY(), new DisruptionAura(20f, 0.4f, 0.2f));

        shielded.beginTick(1);

        assertThat(shielded.fireRateCurrent()).isCloseTo(1.0 / 1.2, within(1e-6));
    }

    @Test
    void twoAurasLeaveAQuarterOfTheDisruption() {
        this.context.towers().add(new AuraTower(this.context, 0, 0));
        this.context.towers().add(new AuraTower(this.context, 0, 0));
        SniperTower shielded = new SniperTower(this.context, 0, 0);
        this.context.towers().add(shielded);
        this.context.disruptions().add(shielded.getX(), shielded.getY(), new DisruptionAura(20f, 0.4f, 0.2f));

        shielded.beginTick(1);

        assertThat(shielded.fireRateCurrent()).isCloseTo(1.0 / 1.1, within(1e-6));
    }

    @Test
    void witheringFieldGivesEveryEnemyInsideTheAuraAVulnerabilityStackEveryInterval() {
        this.addClusterFiller();
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        UpgradePaths.buy(aura, this.context, "Withering Field");
        FakeEnemyMob inside = FakeEnemyMob.at(aura.getX(), aura.getY());
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(aura.getX(), aura.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(10_000, 10_000);
        this.context.enemies().setEnemies(new EnemyMob[]{inside, hidden, outside});

        for (int t = 1; t < 20; t++) {
            aura.doTick(t);
        }
        assertThat(inside.appliedEffects()).isEmpty();
        aura.doTick(20);

        assertThat(inside.appliedEffects()).extracting(Effect::kind).containsExactly(EffectKind.VULNERABLE);
        assertThat(hidden.appliedEffects()).extracting(Effect::kind).containsExactly(EffectKind.VULNERABLE);
        assertThat(outside.appliedEffects()).isEmpty();
    }

    @Test
    void withoutWitheringFieldTheAuraAppliesNothingToEnemies() {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        FakeEnemyMob inside = FakeEnemyMob.at(aura.getX(), aura.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{inside});

        for (int t = 1; t <= 40; t++) {
            aura.doTick(t);
        }

        assertThat(inside.appliedEffects()).isEmpty();
    }
}
