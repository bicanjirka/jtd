package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeLine;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeLinesTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private SniperTower sniper(String... names) {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        UpgradePaths.buy(tower, this.world, names);
        return tower;
    }

    private static List<UpgradeLine> linesOf(SniperTower tower) {
        return tower.upgradeTree().lines(tower.upgrades());
    }

    private static UpgradeLine lineOf(SniperTower tower, String firstNodeName) {
        return linesOf(tower).stream().filter(line -> line.first().displayName().equals(firstNodeName)).findFirst()
                .orElseThrow();
    }

    @Test
    void aChainsOwnedNodesFormOneLineAtItsFurthestLevel() {
        SniperTower tower = this.sniper("Focused Optics", "Focused Optics II");

        UpgradeLine line = lineOf(tower, "Focused Optics");

        assertThat(line.level()).isEqualTo(2);
        assertThat(line.artNode().displayName()).isEqualTo("Focused Optics");
        assertThat(line.owned()).hasSize(2);
    }

    @Test
    void theExtraLineKeepsOneArtToLevelFour() {
        SniperTower tower = this.sniper("Tradecraft", "Tradecraft II");

        UpgradeLine line = lineOf(tower, "Tradecraft");

        assertThat(line.level()).isEqualTo(2);
        assertThat(line.artNode().displayName()).isEqualTo("Tradecraft");
    }

    @Test
    void awakenAndTranscendentContinueAttune() {
        SniperTower tower = this.sniper("Awaken");

        UpgradeLine line = lineOf(tower, "Attune");

        assertThat(line.owned()).extracting(node -> node.displayName()).containsExactly("Attune", "Awaken");
        assertThat(line.level()).isEqualTo(2);
    }

    @Test
    void eachSpecialIsALineOfItsOwn() {
        SniperTower tower = this.sniper("Focused Optics", "Momentum");

        assertThat(lineOf(tower, "Momentum").owned()).hasSize(1);
    }

    @Test
    void linesKeepTheOrderTheirFirstNodeWasBought() {
        SniperTower tower = this.sniper("Focused Optics", "Range");

        List<String> firsts = linesOf(tower).stream().map(line -> line.first().displayName()).toList();

        assertThat(firsts).containsExactly("Attune", "Focused Optics", "Range");
    }
}
