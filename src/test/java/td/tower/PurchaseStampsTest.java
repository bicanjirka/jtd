package td.tower;

import org.junit.jupiter.api.Test;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseStampsTest {

    private static final UpgradeNode NODE = UpgradeTier.HEAD_1.node("a.1", "A", 10);

    @Test
    void whatWasBoughtInABreakIsUnusedUntilTheNextWaveStarts() {
        PurchaseStamps stamps = PurchaseStamps.built(OptionalLong.of(3)).withBought(NODE, OptionalLong.of(3));

        assertThat(stamps.useAfter(3).allUnused()).isTrue();
        assertThat(stamps.useAfter(4).anyUnused()).isFalse();
    }

    @Test
    void whatWasBoughtWhileAWaveWasOutIsUsedAtOnce() {
        PurchaseStamps stamps = PurchaseStamps.built(OptionalLong.empty()).withBought(NODE, OptionalLong.empty());

        assertThat(stamps.useAfter(3).anyUnused()).isFalse();
    }

    @Test
    void aNodeBoughtAgainAfterADowngradeTakesTheBreakItWasBoughtAgainIn() {
        PurchaseStamps stamps = PurchaseStamps.built(OptionalLong.of(1)).withBought(NODE, OptionalLong.of(1))
                .withRemoved(NODE).withBought(NODE, OptionalLong.of(2));

        assertThat(stamps.useAfter(2).isUnused(NODE)).isTrue();
        assertThat(stamps.useAfter(2).allUnused()).isFalse();
    }

    @Test
    void aRebuiltTowerKeepsWhatItsSaveSaysIsUnusedForTheBreakItIsRebuiltIn() {
        PurchaseStamps stamps = PurchaseStamps.of(new TowerUse(true, Set.of("a.1")), List.of(NODE), 5);

        assertThat(stamps.useAfter(5).isUnused(NODE)).isTrue();
        assertThat(stamps.useAfter(5).allUnused()).isFalse();
    }
}
