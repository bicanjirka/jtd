package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.term.TermText;
import td.tower.NodeProgress;
import td.tower.Tower;
import td.tower.TowerBuffState;
import td.tower.TowerFactory;
import td.tower.TowerInspection;
import td.tower.TowerStatLine;
import td.tower.TowerStatRow;
import td.tower.TowerTerm;
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

    /** As the toolbar builds it: a tower in a throwaway world, never placed, at the live price. */
    private InfoSheet shopSheet(TowerFactory.Type type) {
        return TowerSheetText.shop(TowerFactory.createTower(type, new GameWorld(GameHost.noOp()), 0, 0).inspect()
                .withPrice(this.world.towers().priceOf(type)));
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

    private static SheetLine.IconRow iconRow(InfoSheet sheet, String label) {
        return sheet.lines().stream().filter(SheetLine.IconRow.class::isInstance).map(SheetLine.IconRow.class::cast)
                .filter(row -> row.label().equals(label)).findFirst().orElseThrow();
    }

    private static InfoSheet liveStatus(Tower tower) {
        return TowerSheetText.status(tower.inspect().withConditions(tower.conditions()));
    }

    private static Row row(InfoSheet sheet, String label) {
        return rows(sheet).stream().filter(row -> row.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void theToolbarTooltipLeavesOutHowAShotTravelsAndTurns() {
        TowerInspection tower = TowerInspection.of(TowerFactory.Type.MORTAR, 10, List.of(
                TowerStatLine.fixed(TowerStatRow.RANGE, 3), TowerStatLine.fixed(TowerStatRow.ROTATION, 2),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, 4), TowerStatLine.fixed(TowerStatRow.PROJECTILE_SIZE, 1)));

        InfoSheet tooltip = TowerSheetText.shopTooltip(tower);

        assertThat(labels(tooltip)).contains("Range").doesNotContain("Rotation", "Projectile speed", "Projectile size");
        assertThat(labels(TowerSheetText.shop(tower))).contains("Rotation", "Projectile speed", "Projectile size");
    }

    @Test
    void theShopSheetEndsWithHowToPlaceAndCancelButTheToolbarTooltipDoesNot() {
        TowerInspection tower = TowerInspection.of(TowerFactory.Type.SNIPER, 10, List.of());

        InfoSheet shop = TowerSheetText.shop(tower);
        InfoSheet tooltip = TowerSheetText.shopTooltip(tower);

        assertThat(shop.lines().getLast()).isEqualTo(new SheetLine.Prose(TermText.of(TowerSheetText.PLACING_HINT)));
        assertThat(tooltip.lines()).doesNotContain(new SheetLine.Prose(TermText.of(TowerSheetText.PLACING_HINT)));
    }

    @Test
    void theShopOpensWithTheTowersOwnGlyphItsNameAndItsPrice() {
        InfoSheet sheet = shopSheet(TowerFactory.Type.SPLASH);

        assertThat(sheet.lines().getFirst()).isEqualTo(new SheetLine.Title(Glyph.TOWER_BODY, Palette.TOWER_SPLASH_BODY,
                TowerFactory.Type.SPLASH.displayName() + " tower", "$" + TowerFactory.Type.SPLASH.price));
    }

    @Test
    void theShopShowsWhatTheNextCopyCostsWithTheCopiesAlreadyBuilt() {
        this.place(TowerFactory.Type.SPLASH, 0, 0);
        this.place(TowerFactory.Type.SPLASH, 1, 0);

        InfoSheet sheet = this.shopSheet(TowerFactory.Type.SPLASH);

        int nextCopy = this.world.towers().priceOf(TowerFactory.Type.SPLASH);
        assertThat(nextCopy).isGreaterThan(TowerFactory.Type.SPLASH.price);
        assertThat(((SheetLine.Title) sheet.lines().getFirst()).value()).isEqualTo("$" + nextCopy);
    }

    @Test
    void theShopListsStatsThenWhomItTargetsThenWhatItAppliesThenWhatNoRowSays() {
        InfoSheet sheet = shopSheet(TowerFactory.Type.MORTAR);

        assertThat(labels(sheet)).containsExactly("Range", "Physical damage", "Fire rate", "Splash radius",
                "Projectile speed", "Projectile size", "Targets");
        assertThat(iconRow(sheet, "Applies").icons()).isNotEmpty();
        assertThat(sheet.lines()).endsWith(new SheetLine.Prose(TermText.of("Lobs a slow, unguided shell.")),
                new SheetLine.Gap(), new SheetLine.Prose(TermText.of(TowerSheetText.PLACING_HINT)));
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
        assertThat(row(shopSheet(TowerFactory.Type.SNIPER), "Crit chance").value()).isEqualTo("5%");
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
    void aStatAnAuraRaisesReadsBaseToCurrentInTheBetterColourAndTheAuraIsABuff() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        this.place(TowerFactory.Type.AURA, 1, 0);

        InfoSheet sheet = liveStatus(sniper);

        assertThat(row(sheet, "Physical damage")).extracting(Row::value, Row::trend).containsExactly("40 → 48", Trend.BETTER);
        assertThat(row(sheet, "Fire rate").trend()).isEqualTo(Trend.NONE);
        assertThat(iconRow(sheet, "Buffs").icons()).hasSize(1);
    }

    @Test
    void aJammedTowersStatusShowsADebuffAndTheLostRangeAndRateInTheWorseColour() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        this.world.disruptions().add(sniper.getX(), sniper.getY(), new DisruptionAura(20f, 0.3f, 0.2f));
        sniper.beginTick(0);

        InfoSheet sheet = liveStatus(sniper);

        assertThat(iconRow(sheet, "Debuffs").icons()).hasSize(1);
        assertThat(row(sheet, "Range")).extracting(Row::value, Row::trend).containsExactly("4 → 3.8", Trend.WORSE);
        assertThat(row(sheet, "Fire rate")).extracting(Row::value, Row::trend).containsExactly("0.4 → 0.3/s", Trend.WORSE);
        assertThat(row(sheet, "Physical damage").trend()).isEqualTo(Trend.NONE);
    }

    @Test
    void theStatusCountsKillsAndDamageDealtOnOneRow() {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none());
        tower.setKillCount(3);

        InfoSheet sheet = TowerSheetText.status(tower.inspect());

        assertThat(row(sheet, "Kills")).isEqualTo(Row.plain(Glyph.SKULL, "Kills", "3 · 0 dmg"));
    }

    @Test
    void theStatusShowsTheTowersXpAndTheRankItOpened() {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none());
        tower.earnXp(160);

        InfoSheet sheet = TowerSheetText.status(tower.inspect());

        assertThat(row(sheet, "XP")).isEqualTo(Row.toned(Glyph.FILLED_DIAMOND, Palette.TOWER_RANK, "XP", "160 · Expert"));
    }

    @Test
    void anUpgradeShowsInTheStatsItRaisesWhileTheUpgradesPanelNamesIt() {
        this.world.economy().startEconomy(1000, 5);
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradeNode range = UpgradePaths.named(sniper, "Range");

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

    @Test
    void eachOwnedLineIsOnePerkIconAtItsLevel() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradePaths.buy(sniper, this.world, "Focused Optics II");

        InfoSheet sheet = liveStatus(sniper);

        SheetLine.IconRow perks = iconRow(sheet, "Perks");
        assertThat(perks.icons()).extracting(icon -> icon.face().numeral()).contains("II");
        assertThat(perks.icons().stream().filter(icon -> icon.key().id().contains(UpgradePaths.named(sniper, "Focused Optics").id())))
                .hasSize(1);
    }

    @Test
    void aPerkTooltipListsTheLinesNodesAndWhatTheyWereChosenOver() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradePaths.buy(sniper, this.world, "Focused Optics II");

        String optics = UpgradePaths.named(sniper, "Focused Optics").id();
        List<SheetLine> tooltip = iconRow(liveStatus(sniper), "Perks").icons().stream()
                .filter(icon -> icon.key().id().contains(optics)).findFirst().orElseThrow().tooltip().lines();

        assertThat(tooltip.getFirst()).isInstanceOf(SheetLine.IconTitle.class);
        assertThat(tooltip.stream().filter(Row.class::isInstance).filter(line -> ((Row) line).glyph() == Glyph.PIP))
                .isNotEmpty();
    }

    @Test
    void aCountingPerkShowsItsProgress() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradePaths.buy(sniper, this.world, "Focused Optics");
        String id = UpgradePaths.named(sniper, "Focused Optics").id();

        InfoSheet sheet = TowerSheetText.status(sniper.inspect().withProgress(List.of(new NodeProgress(id, 2, 4))));

        assertThat(iconRow(sheet, "Perks").icons()).extracting(icon -> icon.face().corner()).contains("2/4");
    }

    @Test
    void steadyAimShowsItsStacksAsABuff() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        TowerBuffState aim = TowerBuffState.of(TowerTerm.STEADY_AIM, TermText.of("more crit")).withStacks(3)
                .withClock(40, 60);

        InfoSheet sheet = TowerSheetText.status(sniper.inspect().withConditions(List.of(aim)));

        assertThat(iconRow(sheet, "Buffs").icons()).extracting(icon -> icon.face().corner()).containsExactly("3");
    }

    @Test
    void anAppliedEffectsTooltipSaysEachSourceOnTheTower() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradePaths.buy(mortar, this.world, "Napalm");

        SheetLine.IconRow applies = iconRow(liveStatus(mortar), "Applies");

        assertThat(applies.icons()).anyMatch(icon -> icon.tooltip().lines().stream()
                .anyMatch(line -> line.toString().contains("Napalm: ")));
    }
}
