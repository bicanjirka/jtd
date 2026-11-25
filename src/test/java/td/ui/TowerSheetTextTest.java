package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.fixtures.FakeAbilityMechanic;
import td.fixtures.FakeTower;
import td.fixtures.MagazineFixtures;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.MortarTower;
import td.tower.NodeProgress;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerBuffState;
import td.tower.TowerFactory;
import td.tower.TowerModifiers;
import td.tower.TowerStatRow;
import td.tower.TowerTerm;
import td.tower.ability.AbilityBar;
import td.tower.ability.AbilityUnits;
import td.tower.mortar.Magazine;
import td.tower.mortar.MagazineView;
import td.tower.mortar.ShellKind;
import td.tower.mortar.ShellProfile;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.LoadedShell;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.util.GameHost;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static td.tower.upgrade.MechanicCatalogue.entry;

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
    void theShopShowsWhatTheNextCopyCostsWithTheCopiesAlreadyBuilt() {
        this.place(TowerFactory.Type.SPLASH, 0, 0);
        this.place(TowerFactory.Type.SPLASH, 1, 0);

        InfoSheet sheet = this.shopSheet(TowerFactory.Type.SPLASH);

        int nextCopy = this.world.towers().priceOf(TowerFactory.Type.SPLASH);
        assertThat(nextCopy).isGreaterThan(TowerFactory.Type.SPLASH.price);
        assertThat(((SheetLine.TowerHeader) sheet.lines().getFirst()).value()).isEqualTo("$" + nextCopy);
    }


    @Test
    void aTowerThatFiresShowsItsDpsAndOneThatCritsShowsItsCritDamage() {
        SniperTower sniper = TowerFixtures.buffed(new SniperTower(this.world, 0, 0), this.world,
                TowerModifiers.critChance(0.1f));

        InfoSheet sheet = TowerSheetText.status(sniper.inspect());

        assertThat(labels(sheet)).contains("Fire rate", "DPS", "Crit chance", "Crit dmg");
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
        assertThat(ability.capacity()).isEqualTo(AbilityBar.CAPACITY);
        assertThat(ability.cost()).isEqualTo(2);
        assertThat(row(ability.tooltip().sheet(), EffectKind.CHARGED.title()).value()).isEqualTo("1.3 / " + AbilityBar.CAPACITY);
    }

    @Test
    void withCarpetBombingTheStripShowsTheSneakedNormalsAmongTheShellsTheNextFiringSends() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradePaths.buy(mortar, this.world, "Carpet Bombing");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.HEAVY, ShellKind.HEAVY, ShellKind.HEAVY));

        InfoSheet sheet = TowerSheetText.status(BoardRenderer.liveInspection(mortar));

        SheetLine.MagazineRow row = sheet.lines().stream().filter(SheetLine.MagazineRow.class::isInstance)
                .map(SheetLine.MagazineRow.class::cast).findFirst().orElseThrow();
        List<SheetLine.MagazineSlot> sent = row.slots().subList(0, row.loading());
        List<SheetLine.MagazineSlot> sneaked = sent.stream().filter(slot -> !slot.fromMagazine()).toList();
        assertThat(sent).hasSizeGreaterThan(1);
        for (int i = 0; i < sent.size(); i++) {
            assertThat(sent.get(i).fromMagazine()).as("slot " + i).isEqualTo(i % 2 == 0);
            assertThat(sent.get(i).shell()).as("slot " + i).isEqualTo(i % 2 == 0 ? LoadedShell.HEAVY : LoadedShell.NORMAL);
        }
        assertThat(row.slots()).hasSize(Magazine.SIZE + sneaked.size());
        assertThat(row.slots().subList(row.loading(), row.slots().size())).allMatch(SheetLine.MagazineSlot::fromMagazine);
        assertThat(sneaked.getFirst().tooltip().orElseThrow().key())
                .isNotEqualTo(row.slots().getLast().tooltip().orElseThrow().key());
    }

    @Test
    void aMortarsPaneShowsItsShellsFrontFirstThenARowPerKindWithItsShareBothOpeningThatKindsSheet() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradePaths.buy(mortar, this.world, "Tar Shell");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.TAR, ShellKind.HEAVY));

        InfoSheet sheet = TowerSheetText.status(BoardRenderer.liveInspection(mortar));

        List<SheetLine> lines = sheet.lines();
        SheetLine.MagazineRow row = lines.stream().filter(SheetLine.MagazineRow.class::isInstance)
                .map(SheetLine.MagazineRow.class::cast).findFirst().orElseThrow();
        MagazineView view = MortarTower.magazineViewOf(mortar).orElseThrow();
        assertThat(row.slots()).hasSize(view.shells().size()).extracting(SheetLine.MagazineSlot::shell)
                .startsWith(LoadedShell.TAR, LoadedShell.HEAVY);
        assertThat(row.loading()).isEqualTo(1);
        List<Row> kinds = lines.subList(lines.indexOf(row) + 1, lines.indexOf(row) + 4).stream()
                .map(Row.class::cast).toList();
        assertThat(kinds).extracting(Row::label).containsExactly(ShellKind.NORMAL.title(), ShellKind.HEAVY.title(),
                ShellKind.TAR.title());
        assertThat(kinds.get(2).value()).isEqualTo(TermNumbers.percent(view.chances().get(ShellKind.TAR)));
        assertThat(row.slots().getFirst().tooltip().orElseThrow()).isEqualTo(kinds.get(2).tooltip().orElseThrow());
    }

    @Test
    void withThickMixANormalRightBehindAPayloadShowsThePayloadItCarriesInItsSlotAndItsSheet() {
        Tower mortar = this.place(TowerFactory.Type.MORTAR, 0, 0);
        UpgradePaths.buy(mortar, this.world, "Napalm", "Thick Mix");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.NAPALM, ShellKind.NORMAL, ShellKind.NORMAL));

        InfoSheet sheet = TowerSheetText.status(BoardRenderer.liveInspection(mortar));

        SheetLine.MagazineRow row = sheet.lines().stream().filter(SheetLine.MagazineRow.class::isInstance)
                .map(SheetLine.MagazineRow.class::cast).findFirst().orElseThrow();
        SheetLine.MagazineSlot normal = row.slots().get(1);
        assertThat(normal.carried()).contains(LoadedShell.NAPALM);
        assertThat(row.slots().get(2).carried()).isEmpty();
        assertThat(row.slots().get(2).tooltip().orElseThrow().sheet().lines()).doesNotContain(SheetLine.Sentence.of(
                TermText.of("Carries some ").plus(ShellKind.NAPALM.mention()).asSentence()));
        assertThat(normal.tooltip().orElseThrow().sheet().lines()).contains(SheetLine.Sentence.of(
                TermText.of("Carries some ").plus(ShellKind.NAPALM.mention()).asSentence()));
    }

    @Test
    void aShellsSheetGivesWhatItDealsFliesAndAppliesAsTheTowerFiresItNow() {
        ShellProfile heavy = ShellProfile.of(ShellKind.HEAVY, Damage.physical(DamageUnits.ofPoints(77)), 3f,
                new ShellProfile.Blast(1f, 2f, 0.5f, List.of(EffectKind.CRACKED, EffectKind.DAZED)));

        InfoSheet sheet = TowerSheetText.shellSheet(heavy, 0.25f);

        assertThat(row(sheet, TowerStatRow.PHYSICAL_DAMAGE.shortName()).value()).isEqualTo("77");
        assertThat(row(sheet, "Speed").value()).isEqualTo("3");
        assertThat(row(sheet, "Blast edge").value()).isEqualTo("2");
        assertThat(row(sheet, "Share").value()).isEqualTo(TermNumbers.percent(0.25f));
        assertThat(sheet.lines()).contains(SheetLine.Sentence.of(TermText.of("Applies ").plus(EffectKind.CRACKED)
                .plus(", ").plus(EffectKind.DAZED).asSentence()));
    }

    @Test
    void aShellWhoseGroundBurnsSaysWhatEachPulseAddsToTheBurn() {
        ShellProfile napalm = ShellProfile.of(ShellKind.NAPALM, Damage.magic(DamageUnits.ofPoints(20)), 5f,
                new ShellProfile.Blast(1f, 2f, 0.5f, List.of(EffectKind.BURN))).withGroundPulse(
                Damage.magic(DamageUnits.ofPoints(37)));

        InfoSheet sheet = TowerSheetText.shellSheet(napalm, 0.25f);

        assertThat(row(sheet, EffectKind.BURN.title()).value()).isEqualTo("37");
    }
}
