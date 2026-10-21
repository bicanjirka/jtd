package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Invariants every real tower's nodes must hold against its stat sheet. */
class TowerStatCatalogueTest {

    private final GameWorld context = WorldFixtures.newWorld();

    private List<AbstractTower> everyTower() {
        List<AbstractTower> towers = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            towers.add(TowerFactory.createTower(type, this.context, 0, 0).accept(new AsAbstractTower()));
        }
        return towers;
    }

    @Test
    void everyNodeChangesOnlyUniversalStatsOrStatsItsTowerSets() {
        for (AbstractTower tower : this.everyTower()) {
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                for (TowerStat stat : node.effects().modifiers().stats()) {
                    assertThat(stat.universal() || tower.baseStats().stats().isSet(stat))
                            .as("%s's %s changes %s", tower.getType(), node.displayName(), stat).isTrue();
                }
            }
        }
    }

    @Test
    void everyEmittedStatIsUniversal() {
        for (AbstractTower tower : this.everyTower()) {
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                assertThat(node.effects().emission().stats())
                        .as("%s's %s emits", tower.getType(), node.displayName())
                        .allMatch(TowerStat::universal);
            }
        }
    }

    @Test
    void everyNodeSaysWhatItDoes() {
        for (AbstractTower tower : this.everyTower()) {
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                NodeWords words = tower.wordsOf(node);
                assertThat(words.stats().size() + words.sentences().size())
                        .as("%s's %s", tower.getType(), node.displayName()).isPositive();
            }
        }
    }

    @Test
    void everyMechanicSaysWhatItDoes() {
        for (AbstractTower tower : this.everyTower()) {
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                assertThat(tower.mechanicCatalogue().describe(node)).as("%s's %s", tower.getType(), node.displayName())
                        .allMatch(text -> !text.isEmpty());
            }
        }
    }

    @Test
    void onlyBaseChainNodesCarryANote() {
        for (AbstractTower tower : this.everyTower()) {
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                if (!node.id().startsWith("base.")) {
                    assertThat(node.effects().note()).as("%s's %s", tower.getType(), node.displayName()).isEmpty();
                }
            }
        }
    }

    @Test
    void aPassiveTowersNodesCarryNoProcsOrHitBonuses() {
        for (AbstractTower tower : this.everyTower()) {
            if (!tower.isPassive()) {
                continue;
            }
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                assertThat(node.effects().procs()).as("%s's %s", tower.getType(), node.displayName()).isEmpty();
                assertThat(node.effects().hitBonuses()).as("%s's %s", tower.getType(), node.displayName()).isEmpty();
            }
        }
    }

    private static final class AsAbstractTower implements TowerVisitor<AbstractTower> {

        @Override
        public AbstractTower visitSniperTower(SniperTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitSplashTower(SplashTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitSonarTower(SonarTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitPulseTower(PulseTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitAuraTower(AuraTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitMortarTower(MortarTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitSeekerTower(SeekerTower tower) {
            return tower;
        }

        @Override
        public AbstractTower visitCinderTower(CinderTower tower) {
            return tower;
        }
    }
}
