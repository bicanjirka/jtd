package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeSheetTextTest {

    private static final UpgradeNode VETERAN = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
            .withBuff(TowerBuff.damage(0.2f))
            .withGate(new KillCountCondition(10));
    private static final UpgradeNode RANGE = UpgradeNode.of("range", UpgradeSlot.BASE, "Range", 30)
            .withBuff(TowerBuff.range(0.15f));

    private final GameWorld world = WorldFixtures.newWorld();
    private final FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE, VETERAN));

    /** As the Upgrades panel reads it: the tower's offered nodes, numbered from one. */
    private UpgradeOffer offer(UpgradeNode node) {
        int number = this.tower.offeredUpgrades(this.world).indexOf(node) + 1;
        return UpgradeOffer.of(node, number, this.tower, this.world);
    }

    @Test
    void aButtonShowsItsKeyNameAndPriceWhenItCanBeBought() {
        this.world.economy().startEconomy(100, 5);

        assertThat(UpgradeSheetText.buttonText(this.offer(RANGE))).isEqualTo("1  Range  $30");
        assertThat(this.offer(RANGE).buyable()).isTrue();
    }

    @Test
    void aGatedButtonShowsItsProgressInsteadOfItsPrice() {
        this.world.economy().startEconomy(100, 5);
        this.tower.setKillCount(7);

        assertThat(UpgradeSheetText.buttonText(this.offer(VETERAN))).isEqualTo("2  Veteran  7/10 kills");
        assertThat(this.offer(VETERAN).buyable()).isFalse();
    }

    @Test
    void anUnaffordableButtonSaysWhatItNeeds() {
        this.world.economy().startEconomy(20, 5);

        assertThat(UpgradeSheetText.buttonText(this.offer(RANGE))).isEqualTo("1  Range  need $30");
    }

    @Test
    void theHoverShowsNameAndPriceInTheSlotColourThenTheGateThenOneRowPerBonus() {
        this.tower.setKillCount(10);

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(VETERAN));

        assertThat(sheet.lines()).containsExactly(
                new SheetLine.Title(Glyph.PIP, Palette.TOWER_UPGRADE_HEAD, "Veteran", "$10"),
                Row.toned(Glyph.CHECK, Palette.UPGRADE_GATE_MET, "10/10 kills", ""),
                new SheetLine.Gap(),
                Row.plain(Glyph.DOT, "Damage", "+20%"));
    }

    @Test
    void anUnmetGateIsACrossAndANodeMoneyAloneBuysHasNoGateRow() {
        InfoSheet gated = UpgradeSheetText.hover(this.offer(VETERAN));
        InfoSheet free = UpgradeSheetText.hover(this.offer(RANGE));

        assertThat(gated.lines()).contains(Row.toned(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, "0/10 kills", ""));
        assertThat(free.lines()).noneMatch(line -> line instanceof Row row
                && (row.glyph() == Glyph.CHECK || row.glyph() == Glyph.CROSS));
    }

    @Test
    void aSlotHeaderNamesWhatItHoldsOrThatItIsLocked() {
        assertThat(UpgradeSheetText.slotHeader(UpgradeSlot.BASE, Optional.of(RANGE), true)).isEqualTo("Base: Range");
        assertThat(UpgradeSheetText.slotHeader(UpgradeSlot.HEAD, Optional.empty(), true)).isEqualTo("Head");
        assertThat(UpgradeSheetText.slotHeader(UpgradeSlot.SPECIAL, Optional.empty(), false)).isEqualTo("Special: locked");
    }
}
