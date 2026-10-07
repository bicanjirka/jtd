package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/** Every tower here shares one cell, which satisfies the cluster gates. */
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
    void anAuraDoesNotBuffAnotherAuraUntilResonanceFieldIiIsBought() {
        this.context.economy().startEconomy(1000, 5);
        AuraTower first = new AuraTower(this.context, 0, 0);
        this.context.towers().add(first);
        AuraTower second = new AuraTower(this.context, 0, 0);
        this.context.towers().add(second);
        this.addClusterFiller();

        assertThat(first.buffFor(second)).isEqualTo(TowerBuff.none());

        UpgradePaths.awakenVeteran(first);
        first.buyUpgrade(UpgradePaths.named(first, "Resonance Field"));
        UpgradeNode resonanceFieldTwo = UpgradePaths.named(first, "Resonance Field II");
        first.buyUpgrade(resonanceFieldTwo);

        assertThat(first.buffFor(second)).isNotEqualTo(TowerBuff.none());
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
