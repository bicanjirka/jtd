package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.term.TermText;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerModifiers;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeSheetTextTest {

    private static final UpgradeNode VETERAN = UpgradeNode.of("veteran", UpgradeSlot.HEAD, "Veteran", 10)
            .withModifiers(TowerModifiers.damagePercent(0.2f))
            .withXp(10);
    private static final UpgradeNode RANGE = UpgradeNode.of("range", UpgradeSlot.BASE, "Range", 30)
            .withModifiers(TowerModifiers.rangePercent(0.15f));

    private static final int LIST_PRICE = 10;
    private static final UpgradeNode OPTICS = UpgradeTier.HEAD_1.node("optics", "Focused Optics", LIST_PRICE);
    private static final UpgradeNode EYE = UpgradeTier.HEAD_1.node("eye", "Marksman's Eye", LIST_PRICE);
    private static final UpgradeNode STEADY = UpgradeTier.EXTRA_1.node("steady", "Steady", LIST_PRICE);
    private static final UpgradeNode STEADY_TWO = UpgradeTier.EXTRA_2.node("steady.2", "Steady II", LIST_PRICE)
            .after(STEADY);
    private static final UpgradeNode MARKED = UpgradeTier.SPECIAL.node("marked", "Marked Round", LIST_PRICE);
    private static final UpgradeNode FIFTH = UpgradeTier.SPECIAL.node("fifth", "Fifth Shot", LIST_PRICE);
    private static final UpgradeNode MOMENTUM = UpgradeTier.SPECIAL.node("momentum", "Momentum", LIST_PRICE);
    private static final UpgradeTree CHOICES = UpgradeTree.of(StandardBaseSlot.nodes(LIST_PRICE, 4f))
            .with(OPTICS, EYE, STEADY, STEADY_TWO, MARKED, FIFTH, MOMENTUM)
            .withChoice(ExclusiveChoice.oneOf(OPTICS, EYE))
            .withChoice(ExclusiveChoice.specials(MARKED, FIFTH, MOMENTUM));

    private final GameWorld world = WorldFixtures.newWorld();
    private final FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE, VETERAN));

    /** As the Upgrades panel reads it for a hover. */
    private UpgradeOffer offer(UpgradeNode node) {
        return offer(this.tower, node);
    }

    private UpgradeOffer offer(FakeTower offering, UpgradeNode node) {
        return UpgradeOffer.of(node, offering, this.world);
    }

    /** The hover sheet as the panel builds it: the node, then what a click on it does. */
    private InfoSheet sheet(FakeTower tower, UpgradeNode node) {
        return UpgradeSheetText.hover(this.offer(tower, node), UpgradeHover.of(node, tower, this.world), CHOICES);
    }

    /** A tower on the {@link #CHOICES} tree that bought {@code names} through the real mechanism. */
    private FakeTower choosing(String... names) {
        this.world.economy().startEconomy(1000, 5);
        FakeTower choosing = FakeTower.offering(this.world, 1, 1, CHOICES);
        choosing.earnXp(1_000);
        for (String name : names) {
            choosing.buyUpgrade(CHOICES.nodes().stream().filter(n -> n.displayName().equals(name)).findFirst().orElseThrow());
        }
        return choosing;
    }

    @Test
    void theHoverShowsTheFaceNamePriceGatesThenWhatItAdds() {
        this.tower.earnXp(10);
        UpgradeOffer offer = this.offer(VETERAN);

        InfoSheet sheet = UpgradeSheetText.hover(offer);

        assertThat(sheet.lines()).containsExactly(
                new SheetLine.IconTitle(offer.face(), "Veteran", offer.face().tone(), "$10"),
                SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("XP 10/10")),
                new SheetLine.Gap(),
                Row.plain(Glyph.DOT, "Damage", "+20%"));
    }

    @Test
    void aNodesSentencesFollowItsStatRows() {
        Tower sniper = TowerFactory.createTower(TowerFactory.Type.SNIPER, this.world, 3, 3);
        UpgradeNode executioner = sniper.upgradeTree().nodes().stream()
                .filter(node -> node.displayName().equals("Executioner")).findFirst().orElseThrow();

        InfoSheet sheet = UpgradeSheetText.hover(UpgradeOffer.of(executioner, sniper, this.world));

        List<SheetLine> rows = sheet.lines().subList(sheet.lines().indexOf(new SheetLine.Gap()) + 1, sheet.lines().size());
        NodeWords words = sniper.wordsOf(executioner);
        assertThat(rows).hasSize(words.stats().size() + words.sentences().size() + 1);
        assertThat(rows.getLast()).isEqualTo(SheetLine.Sentence.of(words.sentences().getLast().asSentence()));
    }

    @Test
    void anUnmetGateIsACrossAndANodeMoneyAloneBuysHasNoGateRow() {
        InfoSheet gated = UpgradeSheetText.hover(this.offer(VETERAN));
        InfoSheet free = UpgradeSheetText.hover(this.offer(RANGE));

        assertThat(gated.lines()).contains(
                SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, TermText.of("XP 0/10")));
        assertThat(free.lines()).noneMatch(line -> line instanceof SheetLine.Sentence sentence
                && sentence.glyph().isPresent());
    }

    @Test
    void aChainRootsHoverNamesTheChainItLocksOutAndTheExtraNodeLocksNothing() {
        FakeTower attuned = this.choosing("Attune");

        InfoSheet root = UpgradeSheetText.hover(this.offer(attuned, OPTICS));
        InfoSheet extra = UpgradeSheetText.hover(this.offer(attuned, STEADY));

        assertThat(root.lines()).containsSubsequence(Row.plain(Glyph.LOCK, "Locks out", ""),
                Row.plain(Glyph.DOT, "Marksman's Eye", ""));
        assertThat(extra.lines()).noneMatch(line -> line instanceof Row row && row.glyph() == Glyph.LOCK);
    }

    @Test
    void aFirstSpecialsHoverSaysTheOthersWaitForTranscendentRatherThanLockingThemOut() {
        FakeTower awakened = this.choosing("Attune", "Awaken");

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(awakened, FIFTH));

        assertThat(sheet.lines()).contains(Row.plain(Glyph.LOCK, "Next pick", "Transcendent"))
                .noneMatch(line -> line instanceof Row row && row.label().equals("Locks out"));
    }

    @Test
    void aLinesNextLevelSaysWhatItAddsTo() {
        FakeTower steady = this.choosing("Attune", "Steady");

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(steady, STEADY_TWO));

        assertThat(sheet.lines()).contains(SheetLine.Sentence.of(TermText.of("Adds to Steady:")));
    }

    @Test
    void aSealedNodesHoverNamesTheCoreStepThatOpensIt() {
        FakeTower fresh = this.choosing();

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(fresh, OPTICS));

        assertThat(sheet.lines()).contains(Row.toned(Glyph.LOCK, Palette.TOWER_UPGRADE_CORE, "Opens with", "Attune"));
    }

    @Test
    void aPathsHoverListsItsStepsAndTotalAndSaysAClickBuysThem() {
        FakeTower fresh = this.choosing();

        InfoSheet sheet = this.sheet(fresh, OPTICS);

        UpgradeNode attune = core(StandardBaseSlot.ATTUNE_ID);
        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.CHEVRON, "Path · 2 steps", ""),
                Row.plain(Glyph.DOT, "Attune", "$" + attune.price()),
                Row.plain(Glyph.DOT, "Focused Optics", "$" + OPTICS.price()),
                Row.plain(Glyph.CHEVRON, "Total", "$" + (attune.price() + OPTICS.price())),
                SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("Click to buy all 2")),
                Row.toned(Glyph.LOCK, Palette.UPGRADE_GATE_UNMET, "Locks out", ""),
                Row.plain(Glyph.DOT, "Marksman's Eye", ""));
    }

    @Test
    void aPathTheCashDoesNotCoverSaysHowMuchIsShort() {
        FakeTower fresh = this.choosing();
        this.world.economy().startEconomy(1, 5);

        InfoSheet sheet = this.sheet(fresh, STEADY);
        UpgradePath path = CHOICES.planPath(STEADY, fresh, this.world);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Short $" + (path.total() - 1))));
        assertThat(UpgradeSheetText.footer(Optional.of(UpgradeHover.of(STEADY, fresh, this.world))))
                .isEqualTo("2 steps · $" + path.total() + " · short $" + (path.total() - 1));
    }

    @Test
    void aBlockedPathNamesTheStepThatStopsIt() {
        this.world.economy().startEconomy(1000, 5);
        FakeTower novice = FakeTower.offering(this.world, 4, 4, CHOICES);

        UpgradePath path = CHOICES.planPath(STEADY_TWO, novice, this.world);
        InfoSheet sheet = this.sheet(novice, STEADY_TWO);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Blocked: Steady needs more XP")));
        assertThat(UpgradeSheetText.footer(Optional.of(UpgradeHover.of(STEADY_TWO, novice, this.world))))
                .isEqualTo("3 steps · $" + path.total() + " · blocked: XP");
    }

    @Test
    void anOwnedNodesHoverListsWhatADowngradeTakesAndRefundsAndWhatReopens() {
        FakeTower chosen = this.choosing("Attune", "Focused Optics");
        UpgradeHover.Downgrading downgrading = (UpgradeHover.Downgrading) UpgradeHover.of(OPTICS, chosen, this.world);

        InfoSheet sheet = UpgradeSheetText.downgradeConfirmation(downgrading, CHOICES);

        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.DOT, "Focused Optics", "$" + OPTICS.price()),
                Row.toned(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET, "Refund 75%", "+$" + downgrading.refund()),
                Row.plain(Glyph.LOCK, "Reopens", ""),
                Row.plain(Glyph.DOT, "Marksman's Eye", ""));
        assertThat(UpgradeSheetText.footer(Optional.of(downgrading)))
                .isEqualTo("Right-click: downgrade 1 · +$" + downgrading.refund());
        assertThat(UpgradeSheetText.downgradeButton(downgrading)).isEqualTo("Downgrade ( +$" + downgrading.refund() + " )");
    }

    @Test
    void withNothingHoveredTheFooterSaysWhatTheButtonsDo() {
        assertThat(UpgradeSheetText.footer(Optional.empty())).isEqualTo("Click: buy · Right-click: downgrade");
    }

    private static UpgradeNode core(String id) {
        return CHOICES.node(id).orElseThrow();
    }

    @Test
    void theXpLabelCountsTowardTheNextOfferedNodeXpOpens() {
        this.world.economy().startEconomy(1000, 5);
        FakeTower tower = FakeTower.offering(this.world, 3, 3, CHOICES);
        tower.earnXp(40);
        tower.buyUpgrade(CHOICES.nodes().stream().filter(n -> n.id().equals(StandardBaseSlot.ATTUNE_ID)).findFirst().orElseThrow());
        tower.buyUpgrade(STEADY);

        assertThat(this.xpLabel(tower)).isEqualTo("XP 40 / 75 to Steady II");
    }

    @Test
    void onceNoOfferedNodeWaitsOnXpTheLabelNamesTheRank() {
        this.tower.earnXp(60);

        assertThat(this.xpLabel(this.tower)).isEqualTo("XP 60 · Seasoned");
    }

    /** As the panel builds it, from the tower's XP and what it offers. */
    private String xpLabel(FakeTower tower) {
        int xp = tower.experience().xp();
        return UpgradeSheetText.xpLabel(xp, UpgradeSheetText.nextXpGate(tower.offeredUpgrades(this.world), xp));
    }
}
