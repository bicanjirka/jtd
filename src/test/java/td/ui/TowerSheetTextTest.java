package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.util.GameHost;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerSheetTextTest {

    private final GameWorld world = WorldFixtures.newWorld();

    /** As the toolbar builds it: a tower in a throwaway world, never placed. */
    private static InfoSheet shopSheet(TowerFactory.Type type) {
        return TowerSheetText.shop(TowerFactory.createTower(type, new GameWorld(GameHost.noOp()), 0, 0).inspect());
    }

    private Tower place(TowerFactory.Type type, int x, int y) {
        Tower tower = TowerFactory.createTower(type, this.world, x, y);
        this.world.towers().add(tower);
        return tower;
    }

    private static List<String> labels(InfoSheet sheet) {
        return rows(sheet).stream().map(Row::label).toList();
    }

    private static List<Row> rows(InfoSheet sheet) {
        return sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast).toList();
    }

    private static Row row(InfoSheet sheet, String label) {
        return rows(sheet).stream().filter(row -> row.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void theShopOpensWithTheTowersOwnGlyphItsNameAndItsPrice() {
        InfoSheet sheet = shopSheet(TowerFactory.Type.SPLASH);

        assertThat(sheet.lines().getFirst()).isEqualTo(
                new SheetLine.Title(Glyph.TOWER_BODY, Palette.TOWER_SPLASH_BODY, "Splash tower", "$15"));
    }

    @Test
    void theShopListsStatsThenBehavioursOneRowEachThenWhatNoRowSays() {
        InfoSheet sheet = shopSheet(TowerFactory.Type.MORTAR);

        assertThat(labels(sheet)).containsExactly("Range", "Physical damage", "Fire rate", "Splash radius", "Slows", "Targets");
        assertThat(row(sheet, "Slows")).isEqualTo(Row.effect(Palette.STATUS_MARKER_SLOW, "Slows", "50%, 2 s"));
        assertThat(sheet.lines().getLast()).isEqualTo(new SheetLine.Prose("Lobs a slow, unguided shell."));
    }

    @Test
    void damageIsNamedAndColouredByItsType() {
        InfoSheet seeker = shopSheet(TowerFactory.Type.SEEKER);

        assertThat(row(seeker, "Magic damage").tone()).contains(Palette.DAMAGE_MAGIC);
        assertThat(row(shopSheet(TowerFactory.Type.SNIPER), "Physical damage").tone()).contains(Palette.DAMAGE_PHYSICAL);
    }

    @Test
    void aSonarShowsARotationAndCritChanceShowsOnlyWhereATowerHasAny() {
        assertThat(labels(shopSheet(TowerFactory.Type.SONAR))).contains("Rotation").doesNotContain("Fire rate", "Crit chance");
        assertThat(row(shopSheet(TowerFactory.Type.SNIPER), "Crit chance").value()).isEqualTo("15%");
    }

    @Test
    void theStatusTitleLeavesTheSellValueToTheButtonAndLeavesOutTheDescription() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);

        InfoSheet sheet = TowerSheetText.status(mortar.inspect());

        assertThat(sheet.lines().getFirst()).isEqualTo(new SheetLine.Title(Glyph.TOWER_BODY, Palette.TOWER_MORTAR_BODY,
                "Mortar tower", ""));
        assertThat(sheet.lines()).noneMatch(SheetLine.Prose.class::isInstance);
    }

    @Test
    void aStatAnAuraRaisesReadsBaseToCurrentInTheBetterColourAndTheAuraGetsARow() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        this.place(TowerFactory.Type.AURA, 1, 0);

        InfoSheet sheet = TowerSheetText.status(sniper.inspect());

        assertThat(row(sheet, "Physical damage")).extracting(Row::value, Row::trend).containsExactly("30 → 36", Trend.BETTER);
        assertThat(row(sheet, "Fire rate").trend()).isEqualTo(Trend.NONE);
        assertThat(row(sheet, "Aura")).isEqualTo(Row.toned(Glyph.RING, Palette.TOWER_AURA_RING, "Aura", ""));
    }

    @Test
    void theStatusCountsKillsAndDamageDealtOnOneRow() {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none());
        tower.setKillCount(3);

        InfoSheet sheet = TowerSheetText.status(tower.inspect());

        assertThat(row(sheet, "Kills")).isEqualTo(Row.plain(Glyph.SKULL, "Kills", "3 · 0 dmg"));
    }

    @Test
    void anUpgradeShowsInTheStatsItRaisesWhileTheUpgradesPanelNamesIt() {
        this.world.economy().startEconomy(1000, 5);
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradeNode range = sniper.offeredUpgrades(this.world).getFirst();

        sniper.buyUpgrade(range);

        InfoSheet sheet = TowerSheetText.status(sniper.inspect());
        assertThat(row(sheet, "Range").trend()).isEqualTo(Trend.BETTER);
        assertThat(rows(sheet)).noneMatch(row -> row.glyph() == Glyph.PIP);
        assertThat(labels(sheet)).doesNotContain("Awaken");
    }

    @Test
    void theShopNeverListsUpgrades() {
        Tower sniper = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.world, 0, 0);

        InfoSheet sheet = TowerSheetText.shop(sniper.inspect());

        assertThat(labels(sheet)).doesNotContain("Awaken");
        assertThat(rows(sheet)).noneMatch(row -> row.glyph() == Glyph.PIP);
    }
}
