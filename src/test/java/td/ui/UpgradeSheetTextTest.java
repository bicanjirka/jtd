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
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import java.util.List;

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

    /** As the Upgrades panel reads it: the tower's offered nodes, numbered by the key that buys each. */
    private UpgradeOffer offer(UpgradeNode node) {
        return offer(this.tower, node);
    }

    private UpgradeOffer offer(FakeTower offering, UpgradeNode node) {
        List<UpgradeNode> offered = offering.offeredUpgrades(this.world);
        return UpgradeOffer.of(node, offered, offering, this.world);
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
    void aButtonShowsItsKeyNameAndPriceWhenItCanBeBought() {
        this.world.economy().startEconomy(100, 5);

        assertThat(UpgradeSheetText.buttonText(this.offer(RANGE))).isEqualTo("1  Range\t$30");
        assertThat(this.offer(RANGE).buyable()).isTrue();
    }

    @Test
    void aGatedButtonShowsItsProgressInsteadOfItsPrice() {
        this.world.economy().startEconomy(100, 5);
        this.tower.earnXp(7);

        assertThat(UpgradeSheetText.buttonText(this.offer(VETERAN))).isEqualTo("3  Veteran\tXP 7/10");
        assertThat(this.offer(VETERAN).buyable()).isFalse();
    }

    @Test
    void anUnaffordableButtonSaysWhatItNeeds() {
        this.world.economy().startEconomy(20, 5);

        assertThat(UpgradeSheetText.buttonText(this.offer(RANGE))).isEqualTo("1  Range\tneed $30");
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

        InfoSheet sheet = UpgradeSheetText.hover(UpgradeOffer.of(executioner, List.of(executioner), sniper, this.world));

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
    void aSlotOnOfferOrHoldingANodeIsJustItsName() {
        UpgradeState owned = UpgradeState.none().with(RANGE);

        assertThat(UpgradeSheetText.slotHeader(UpgradeSlot.BASE, owned, false)).isEqualTo("Base");
        assertThat(UpgradeSheetText.slotHeader(UpgradeSlot.HEAD, owned, true)).isEqualTo("Head");
    }

    @Test
    void aClosedSlotsHeaderSaysWhatOpensIt() {
        UpgradeNode forOptics = UpgradeTier.SPECIAL.node("for.optics", "Lens Flare", LIST_PRICE).after(OPTICS);
        UpgradeTree perChain = UpgradeTree.of(StandardBaseSlot.nodes(LIST_PRICE, 4f)).with(OPTICS, EYE, forOptics)
                .withChoice(ExclusiveChoice.oneOf(OPTICS, EYE));
        FakeTower fresh = this.choosing();
        FakeTower awakened = FakeTower.offering(this.world, 2, 2, perChain);
        perChain.nodesIn(UpgradeSlot.BASE).stream()
                .filter(n -> n.id().equals(StandardBaseSlot.ATTUNE_ID) || n.id().equals(StandardBaseSlot.AWAKEN_ID))
                .forEach(awakened::buyUpgrade);

        assertThat(this.header(fresh, UpgradeSlot.HEAD)).isEqualTo("Head: needs Attune");
        assertThat(this.header(fresh, UpgradeSlot.SPECIAL)).isEqualTo("Special: needs Awaken");
        assertThat(this.header(awakened, UpgradeSlot.SPECIAL)).isEqualTo("Special: choose a chain first");
    }

    /** As the panel builds it: whether anything in the slot is offered right now. */
    private String header(FakeTower tower, UpgradeSlot slot) {
        boolean offered = tower.offeredUpgrades(this.world).stream().anyMatch(n -> n.slot() == slot);
        return UpgradeSheetText.slotHeader(slot, tower.upgrades(), offered);
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
    void theChoiceLabelCountsTheMembersOnOffer() {
        FakeTower awakened = this.choosing("Attune", "Awaken");

        UpgradeOffer fifth = this.offer(awakened, FIFTH);

        assertThat(UpgradeSheetText.choiceLabel(fifth.rivals().size() + 1)).isEqualTo("1 of 3");
    }

    @Test
    void aLinesNextLevelSaysWhatItAddsTo() {
        FakeTower steady = this.choosing("Attune", "Steady");

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(steady, STEADY_TWO));

        assertThat(sheet.lines()).contains(SheetLine.Sentence.of(TermText.of("Adds to Steady:")));
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
