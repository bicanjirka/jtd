package td.tower;

import org.junit.jupiter.api.Test;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.S1;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.state;

class UpgradeLinesTest {

    private static UpgradeLine lineOf(UpgradeState owned, String firstNodeName) {
        return TREE.lines(owned).stream().filter(line -> line.first().displayName().equals(firstNodeName)).findFirst()
                .orElseThrow();
    }

    @Test
    void aChainsOwnedNodesFormOneLineAtItsFurthestLevel() {
        UpgradeLine line = lineOf(state(A1, TIER_ONE, A2), A1.displayName());

        assertThat(line.level()).isEqualTo(2);
        assertThat(line.artNode()).isEqualTo(A1);
        assertThat(line.owned()).containsExactly(A1, A2);
    }

    @Test
    void aClusterMemberWithNothingAfterItIsALineOfItsOwn() {
        assertThat(lineOf(state(A1, TIER_ONE, S1), S1.displayName()).owned()).containsExactly(S1);
    }

    @Test
    void linesKeepTheOrderTheirFirstNodeWasBoughtAndATiersUnlockIsNone() {
        List<String> firsts = TREE.lines(state(LONE, TIER_ONE, A1)).stream().map(line -> line.first().displayName())
                .toList();

        assertThat(firsts).containsExactly(LONE.displayName(), A1.displayName());
    }
}
