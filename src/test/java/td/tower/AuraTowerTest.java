package td.tower;

import org.junit.jupiter.api.Test;
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
        aura.buyUpgrade(UpgradePaths.named(aura, "Awaken"));

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
        aura.buyUpgrade(UpgradePaths.named(aura, "Awaken"));
        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core"));

        aura.buyUpgrade(UpgradePaths.named(aura, "Amplifying Core II"));

        assertThat(neighbour.coolDownCurrent()).isLessThan(neighbour.coolDownMax);
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

        first.buyUpgrade(UpgradePaths.named(first, "Awaken"));
        first.buyUpgrade(UpgradePaths.named(first, "Resonance Field"));
        UpgradeNode resonanceFieldTwo = UpgradePaths.named(first, "Resonance Field II");
        first.buyUpgrade(resonanceFieldTwo);

        assertThat(first.buffFor(second)).isNotEqualTo(TowerBuff.none());
    }
}
