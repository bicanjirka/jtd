package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.fixtures.FakeAbilityMechanic;
import td.fixtures.FakeTower;
import td.fixtures.MagazineFixtures;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.term.TermText;
import td.tower.MortarTower;
import td.tower.NodeProgress;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerBuffState;
import td.tower.TowerFactory;
import td.tower.TowerInspection;
import td.tower.TowerModifiers;
import td.tower.TowerStat;
import td.tower.TowerStatLine;
import td.tower.TowerStatRow;
import td.tower.TowerTerm;
import td.tower.ability.AbilityUnits;
import td.tower.mortar.MagazineView;
import td.tower.mortar.ShellKind;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.LoadedShell;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.ui.render.TowerLook;
import td.util.GameHost;
import td.util.GameWorld;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static td.tower.upgrade.MechanicCatalogue.entry;

class TowerSheetTextTest {

    private static final UpgradeNode LINE = UpgradeNode.of("test.line", "Line", 1)
            .withModifiers(TowerModifiers.damage(1f));
    private static final UpgradeNode LINE_2 = UpgradeNode.of("test.line.2", "Line II", 1)
            .withModifiers(TowerModifiers.damage(1f)).after(LINE);

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

    /** A tower whose tree is one line of two nodes. */
    private Tower lineTower() {
        return FakeTower.offering(this.world, 0, 0, UpgradeTree.of(List.of(LINE)).with(LINE_2));
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
    void theToolbarTooltipShowsEveryRowTheShopDoes() {
        TowerInspection tower = TowerInspection.of(TowerFactory.Type.MORTAR, 10, List.of(
                TowerStatLine.fixed(TowerStat.RANGE, TowerStat.RANGE, 3),
                TowerStatLine.fixed(TowerStatRow.FIRE_RATE, TowerStat.ATTACK_SPEED, 2),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, TowerStat.PROJECTILE_SPEED, 4)));

        InfoSheet tooltip = TowerSheetText.shopTooltip(tower);

        assertThat(labels(tooltip)).containsExactly("Range", "Fire rate", "Proj speed");
        assertThat(labels(TowerSheetText.shop(tower))).isEqualTo(labels(tooltip));
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

        assertThat(sheet.lines().getFirst()).isEqualTo(new SheetLine.TowerHeader(TowerLook.SPLASH,
                TowerFactory.Type.SPLASH.displayName() + " tower", "$" + TowerFactory.Type.SPLASH.price));
    }

    @Test
    void theShopShowsWhatTheNextCopyCostsWithTheCopiesAlreadyBuilt() {
        this.place(TowerFactory.Type.SPLASH, 0, 0);
        this.place(TowerFactory.Type.SPLASH, 1, 0);

        InfoSheet sheet = this.shopSheet(TowerFactory.Type.SPLASH);

        int nextCopy = this.world.towers().priceOf(TowerFactory.Type.SPLASH);
        assertThat(nextCopy).isGreaterThan(TowerFactory.Type.SPLASH.price);
        assertThat(((SheetLine.TowerHeader) sheet.lines().getFirst()).value()).isEqualTo("$" + nextCopy);
    }

    @Test
    void theShopListsStatsThenWhomItTargetsThenWhatItAppliesThenWhatNoRowSays() {
        InfoSheet sheet = shopSheet(TowerFactory.Type.MORTAR);

        assertThat(labels(sheet)).startsWith("Range").endsWith("Targets");
        assertThat(iconRow(sheet, "Applies").icons()).isNotEmpty();
        assertThat(sheet.lines()).endsWith(new SheetLine.Prose(TermText.of(TowerFactory.createTower(TowerFactory.Type.MORTAR, this.world, 0, 0).inspect().description())),
                new SheetLine.Gap(), new SheetLine.Prose(TermText.of(TowerSheetText.PLACING_HINT)));
    }

    @Test
    void damageIsNamedAndColouredByItsType() {
        InfoSheet seeker = shopSheet(TowerFactory.Type.SEEKER);

        assertThat(row(seeker, "Magic dmg").tone()).contains(Palette.DAMAGE_MAGIC);
        assertThat(row(shopSheet(TowerFactory.Type.SNIPER), "Phys dmg").tone()).contains(Palette.DAMAGE_PHYSICAL);
    }


    @Test
    void aTowerThatFiresShowsItsDpsAndOneThatCritsShowsItsCritDamage() {
        SniperTower sniper = TowerFixtures.buffed(new SniperTower(this.world, 0, 0), this.world,
                TowerModifiers.critChance(0.1f));

        InfoSheet sheet = TowerSheetText.status(sniper.inspect());

        assertThat(labels(sheet)).contains("Fire rate", "DPS", "Crit chance", "Crit dmg");
    }

    @Test
    void theStatusTitleLeavesTheSellValueToTheButtonAndLeavesOutTheDescription() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);

        InfoSheet sheet = TowerSheetText.status(mortar.inspect());

        assertThat(sheet.lines().getFirst()).isEqualTo(new SheetLine.TowerHeader(TowerLook.MORTAR,
                "Mortar tower", ""));
        assertThat(sheet.lines()).noneMatch(SheetLine.Prose.class::isInstance);
    }

    @Test
    void aStatAReceivedBuffRaisesReadsBaseToCurrentInTheBetterColourAndTheBuffIsABuff() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        this.world.towers().add(FakeTower.offering(this.world, 1, 0, UpgradeTree.none())
                .giving(TowerModifiers.damagePercent(0.2f)));
        sniper.recalculateStats();

        InfoSheet sheet = liveStatus(sniper);

        assertThat(row(sheet, "Phys dmg")).extracting(Row::trend, Row::arrow)
                .containsExactly(Trend.BETTER, SheetLine.Arrow.UP);
        assertThat(row(sheet, "Fire rate").trend()).isEqualTo(Trend.NONE);
        assertThat(iconRow(sheet, "Buffs").icons()).hasSize(1);
    }

    @Test
    void aPrismBeaconsPaneListsTheMarksItsLightCarriesNow() {
        Tower beacon = this.place(TowerFactory.Type.AURA, 0, 0);
        UpgradePaths.buy(beacon, this.world, "Prism");
        InfoSheet empty = TowerSheetText.status(BoardRenderer.liveInspection(beacon));
        this.place(TowerFactory.Type.SNIPER, 1, 0);

        InfoSheet lit = TowerSheetText.status(BoardRenderer.liveInspection(beacon));

        assertThat(empty.lines()).noneMatch(line -> line instanceof SheetLine.IconRow row
                && row.label().equals("Carries"));
        assertThat(iconRow(lit, "Carries").icons()).hasSize(1);
    }

    @Test
    void aJammedTowersStatusShowsADebuffAndTheLostRangeAndRateInTheWorseColour() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        this.world.disruptions().add(sniper.getX(), sniper.getY(), new DisruptionAura(20f, 0.3f, 0.2f));
        sniper.beginTick(0);

        InfoSheet sheet = liveStatus(sniper);

        assertThat(iconRow(sheet, "Debuffs").icons()).hasSize(1);
        assertThat(row(sheet, "Range")).extracting(Row::trend, Row::arrow)
                .containsExactly(Trend.WORSE, SheetLine.Arrow.DOWN);
        assertThat(row(sheet, "Fire rate")).extracting(Row::trend, Row::unit).containsExactly(Trend.WORSE, "/s");
        assertThat(row(sheet, "Phys dmg").trend()).isEqualTo(Trend.NONE);
    }

    @Test
    void theStatusCountsKillsAndDamageDealtOnOneRow() {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none());
        tower.setKillCount(3);

        InfoSheet sheet = TowerSheetText.status(tower.inspect());

        assertThat(row(sheet, "Kills")).isEqualTo(Row.plain(Glyph.SKULL, "Kills", "3 · 0").withGloss("dmg", ""));
    }

    @Test
    void theStatusShowsTheTowersXp() {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none());
        tower.earnXp(160);

        InfoSheet sheet = TowerSheetText.status(tower.inspect());

        assertThat(row(sheet, "XP")).isEqualTo(Row.plain(Glyph.FILLED_DIAMOND, "XP", "160"));
    }

    @Test
    void anUpgradeShowsInTheStatsItRaisesWhileTheUpgradesPanelNamesIt() {
        this.world.economy().startEconomy(1000, 5);
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradeNode range = UpgradePaths.named(mortar, "Elevation");

        mortar.buyUpgrade(range);

        InfoSheet sheet = TowerSheetText.status(mortar.inspect());
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
        Tower tower = this.lineTower();
        UpgradePaths.buy(tower, this.world, LINE_2.displayName());

        InfoSheet sheet = liveStatus(tower);

        SheetLine.IconRow perks = iconRow(sheet, "Perks");
        assertThat(perks.icons()).extracting(icon -> icon.face().numeral()).contains("II");
        assertThat(perks.icons().stream().filter(icon -> icon.key().id().contains(LINE.id()))).hasSize(1);
    }

    @Test
    void aPerkTooltipListsTheLinesNodesAndWhatTheyWereChosenOver() {
        Tower tower = this.lineTower();
        UpgradePaths.buy(tower, this.world, LINE_2.displayName());

        List<SheetLine> tooltip = iconRow(liveStatus(tower), "Perks").icons().stream()
                .filter(icon -> icon.key().id().contains(LINE.id())).findFirst().orElseThrow().tooltip().lines();

        assertThat(tooltip.getFirst()).isInstanceOf(SheetLine.IconTitle.class);
        assertThat(tooltip.stream().filter(Row.class::isInstance).filter(line -> ((Row) line).glyph() == Glyph.PIP))
                .isNotEmpty();
    }

    @Test
    void aCountingPerkShowsItsProgress() {
        Tower sniper = this.place(TowerFactory.Type.SNIPER, 0, 0);
        UpgradePaths.buy(sniper, this.world, "Fourth Shot");
        String id = UpgradePaths.named(sniper, "Fourth Shot").id();

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

    @Test
    void eachAbilityShowsItsBarOneUnitLongerThanItsCostAndItsHoverReadsTheFillInUnits() {
        UpgradeNode bolt = UpgradeNode.of("test.bolt", "Bolt", 0);
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.none().with(bolt),
                MechanicCatalogue.of(
                        entry(bolt.id(), () -> FakeAbilityMechanic.of(TowerTerm.HOLLOW_POINT, EffectKind.CHARGED, 2))));
        this.world.economy().startEconomy(100, 5);
        tower.buyUpgrade(bolt);
        tower.earn(AbilityUnits.of(0.25f));

        InfoSheet sheet = TowerSheetText.status(tower.inspect().withConditions(tower.conditions())
                .withProgress(tower.progress()).withAbilities(tower.abilities()));

        SheetLine.AbilityRow ability = sheet.lines().stream().filter(SheetLine.AbilityRow.class::isInstance)
                .map(SheetLine.AbilityRow.class::cast).findFirst().orElseThrow();
        assertThat(ability.name()).isEqualTo(TowerTerm.HOLLOW_POINT.title());
        assertThat(ability.capacity()).isEqualTo(3);
        assertThat(ability.cost()).isEqualTo(2);
        assertThat(row(ability.tooltip().sheet(), EffectKind.CHARGED.title()).value()).isEqualTo("1.3 / 3");
    }

    @Test
    void aMortarsPaneShowsItsMagazineFrontFirstThenEachKindsShareInWords() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradePaths.buy(mortar, this.world, "Tar");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.TAR, ShellKind.HEAVY));

        InfoSheet sheet = TowerSheetText.status(BoardRenderer.liveInspection(mortar));

        List<SheetLine> lines = sheet.lines();
        SheetLine.MagazineRow row = lines.stream().filter(SheetLine.MagazineRow.class::isInstance)
                .map(SheetLine.MagazineRow.class::cast).findFirst().orElseThrow();
        MagazineView view = MortarTower.magazineViewOf(mortar).orElseThrow();
        assertThat(row.shells()).hasSize(view.shells().size()).startsWith(LoadedShell.TAR, LoadedShell.HEAVY);
        assertThat(row.loading()).isEqualTo(1);
        assertThat(row.shares()).extracting(SheetLine.ShellShare::shell)
                .containsExactly(LoadedShell.NORMAL, LoadedShell.HEAVY, LoadedShell.TAR);
        assertThat(row.shares().stream().mapToDouble(SheetLine.ShellShare::share).sum()).isCloseTo(1.0, within(1e-3));
        assertThat(lines.get(lines.indexOf(row) + 1))
                .isEqualTo(SheetLine.Sentence.of(TowerSheetText.chancesText(view.chances())));
    }

    @Test
    void theChancesInWordsNameEachKindWithItsShareInOrder() {
        Map<ShellKind, Float> chances = new EnumMap<>(ShellKind.class);
        chances.put(ShellKind.NORMAL, 0.6f);
        chances.put(ShellKind.HEAVY, 0.4f);

        TermText text = TowerSheetText.chancesText(chances);

        assertThat(text).isEqualTo(TermText.of("Normal 60% · ").plus(TowerTerm.HEAVY_SHELL).plus(" 40%"));
    }
}
