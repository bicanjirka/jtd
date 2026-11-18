package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.NodeWords;
import td.tower.TowerModifiers;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
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
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;

class UpgradeSheetTextTest {

    private static final UpgradeNode VETERAN = UpgradeNode.of("veteran", "Veteran", 10)
            .withModifiers(TowerModifiers.damagePercent(0.2f));
    private static final UpgradeNode RANGE = UpgradeNode.of("range", "Range", 30)
            .withModifiers(TowerModifiers.rangePercent(0.15f));
    private static final int TIER_ONE_XP = UpgradeTier.standard().get(1).xp();

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
        return UpgradeSheetText.hover(this.offer(tower, node), UpgradeHover.of(node, tower, this.world), TREE);
    }

    /** A tower on the shared tiered tree with {@code xp}, that bought {@code targets} and their paths. */
    private FakeTower choosing(int xp, UpgradeNode... targets) {
        this.world.economy().startEconomy(1000, 5);
        FakeTower choosing = FakeTower.offering(this.world, 1, 1, TREE);
        choosing.earnXp(xp);
        for (UpgradeNode target : targets) {
            assertThat(choosing.buyPath(target)).as(target.displayName()).isTrue();
        }
        return choosing;
    }

    @Test
    void theHoverShowsTheFaceNameAndPriceThenWhatItAdds() {
        UpgradeOffer offer = this.offer(VETERAN);

        InfoSheet sheet = UpgradeSheetText.hover(offer);

        assertThat(sheet.lines().subList(0, 2)).containsExactly(
                new SheetLine.IconTitle(offer.face(), "Veteran", offer.face().tone(), "$10"),
                new SheetLine.Gap());
        assertThat(sheet.lines().getLast()).isInstanceOfSatisfying(Row.class, row -> {
            assertThat(row.label()).isEqualTo("Damage");
            assertThat(row.value()).isEqualTo("+20%");
        });
    }

    @Test
    void aNodesSentencesFollowItsStatRows() {
        FakeTower describing = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE))
                .describing(List.of(TermText.of("first"), TermText.of("second")));

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(describing, RANGE));

        List<SheetLine> rows = sheet.lines().subList(sheet.lines().indexOf(new SheetLine.Gap()) + 1, sheet.lines().size());
        NodeWords words = describing.wordsOf(RANGE);
        assertThat(rows).hasSize(words.stats().size() + 2);
        assertThat(rows.subList(words.stats().size(), rows.size())).containsExactly(
                SheetLine.Sentence.of(TermText.of("first").asSentence()),
                SheetLine.Sentence.of(TermText.of("second").asSentence()));
    }

    @Test
    void aNodeSentencesMentionsAreDefinedBelowThemInOrder() {
        FakeTower describing = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE))
                .describing(List.of(TermText.of("brings ").plus(EffectKind.CHILL), TermText.of(EffectKind.BURN)));

        List<SheetLine> lines = UpgradeSheetText.hover(this.offer(describing, RANGE)).lines();

        assertThat(lines.subList(lines.lastIndexOf(new SheetLine.Divider()) + 1, lines.size())).containsExactly(
                definition(TermText.of(EffectKind.CHILL), EffectKind.CHILL.describe()),
                definition(TermText.of(EffectKind.BURN), EffectKind.BURN.describe()));
    }

    @Test
    void aRepeatedMentionIsDefinedOnceButADifferentDetailIsDefinedAgain() {
        TermText detail = TermText.of("only here");
        FakeTower describing = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE))
                .describing(List.of(TermText.of(EffectKind.CHILL), TermText.of("again ").plus(EffectKind.CHILL),
                        TermText.of(EffectKind.CHILL, detail)));

        List<SheetLine> lines = UpgradeSheetText.hover(this.offer(describing, RANGE)).lines();

        assertThat(lines.subList(lines.lastIndexOf(new SheetLine.Divider()) + 1, lines.size())).containsExactly(
                definition(TermText.of(EffectKind.CHILL), EffectKind.CHILL.describe()),
                definition(TermText.of(EffectKind.CHILL, detail), detail));
    }

    @Test
    void aMentionInsideADefinitionGetsNoDefinitionOfItsOwn() {
        TermText detail = TermText.of("as ").plus(EffectKind.BURN);
        FakeTower describing = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(RANGE))
                .describing(List.of(TermText.of(EffectKind.CHILL, detail)));

        List<SheetLine> lines = UpgradeSheetText.hover(this.offer(describing, RANGE)).lines();

        assertThat(lines.subList(lines.lastIndexOf(new SheetLine.Divider()) + 1, lines.size()))
                .containsExactly(definition(TermText.of(EffectKind.CHILL, detail), detail));
    }

    private static SheetLine definition(TermText mention, TermText meaning) {
        return new SheetLine.Prose(mention.plus(" — ").plus(meaning.asSentence()));
    }

    @Test
    void aClosedTiersNodeIsACrossNamingWhatTheTierLacksAndAnOpenOneHasNoGateRow() {
        FakeTower fresh = this.choosing(0);

        InfoSheet closed = UpgradeSheetText.hover(this.offer(fresh, MID));
        InfoSheet open = UpgradeSheetText.hover(this.offer(fresh, LONE));

        assertThat(closed.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of(UpgradeTier.nameOf(1) + " · XP 0 / " + TIER_ONE_XP)));
        assertThat(open.lines()).noneMatch(line -> line instanceof SheetLine.Sentence sentence
                && sentence.glyph().isPresent());
    }

    @Test
    void aTierWhoseXpIsMetButUnlockIsNotSaysToUnlockIt() {
        FakeTower ready = this.choosing(TIER_ONE_XP);

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(ready, MID));

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of(UpgradeTier.nameOf(1) + " · unlock " + TermNumbers.cash(TIER_ONE.price()))));
    }

    @Test
    void aTiersUnlockSaysWhatItOpens() {
        InfoSheet sheet = UpgradeSheetText.hover(this.offer(this.choosing(TIER_ONE_XP), TIER_ONE));

        assertThat(sheet.lines()).contains(SheetLine.Sentence.of(TermText.of("Opens " + UpgradeTier.nameOf(1)
                + ": every upgrade in it then costs only credits.")));
    }

    @Test
    void aClusterMembersHoverNamesItsClusterAndTheOtherMembers() {
        InfoSheet member = UpgradeSheetText.hover(this.offer(this.choosing(0), A1));
        InfoSheet lone = UpgradeSheetText.hover(this.offer(this.choosing(0), LONE));

        assertThat(member.lines()).containsSubsequence(Row.plain(Glyph.LOCK, "One of Roots", ""),
                Row.plain(Glyph.DOT, B1.displayName(), ""));
        assertThat(lone.lines()).noneMatch(line -> line instanceof Row row && row.glyph() == Glyph.LOCK);
    }

    @Test
    void aLinesNextLevelSaysWhatItAddsTo() {
        FakeTower started = this.choosing(TIER_ONE_XP, A1);

        InfoSheet sheet = UpgradeSheetText.hover(this.offer(started, A2));

        assertThat(sheet.lines()).contains(SheetLine.Sentence.of(TermText.of("Adds to " + A1.displayName() + ":")));
    }

    @Test
    void aPathsHoverListsItsStepsAndTotalAndSaysAClickBuysThem() {
        FakeTower fresh = this.choosing(TIER_ONE_XP);

        InfoSheet sheet = this.sheet(fresh, A2);

        int total = TIER_ONE.price() + A1.price() + A2.price();
        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.CHEVRON, "Path · 3 steps", ""),
                Row.plain(Glyph.DOT, TIER_ONE.displayName(), "$" + TIER_ONE.price()),
                Row.plain(Glyph.DOT, A1.displayName(), "$" + A1.price()),
                Row.plain(Glyph.DOT, A2.displayName(), "$" + A2.price()),
                Row.plain(Glyph.CHEVRON, "Total", "$" + total),
                SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("Click to buy all 3")),
                Row.toned(Glyph.LOCK, Palette.UPGRADE_GATE_UNMET, "Locks out", ""),
                Row.plain(Glyph.DOT, B1.displayName() + " line", ""));
    }

    @Test
    void aPathTheCashDoesNotCoverSaysHowMuchIsShort() {
        FakeTower fresh = this.choosing(TIER_ONE_XP);
        this.world.economy().startEconomy(1, 5);

        InfoSheet sheet = this.sheet(fresh, MID);
        UpgradePath path = TREE.planPath(MID, fresh, this.world);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Short $" + (path.total() - 1))));
        assertThat(UpgradeSheetText.footer(Optional.of(UpgradeHover.of(MID, fresh, this.world))))
                .isEqualTo("2 steps · $" + path.total() + " · short $" + (path.total() - 1));
    }

    @Test
    void duringFreeBuildAPathTheCashDoesNotCoverIsStillOneClickBuys() {
        FakeTower fresh = this.choosing(TIER_ONE_XP);
        this.world.economy().startEconomy(1, 5);
        this.world.economy().setFreeBuild(true);

        UpgradeHover hover = UpgradeHover.of(MID, fresh, this.world);
        InfoSheet sheet = this.sheet(fresh, MID);

        assertThat(UpgradeSheetText.footerWarns(Optional.of(hover))).isFalse();
        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET,
                TermText.of("Click to buy all 2")));
    }

    @Test
    void aBlockedPathNamesTheStepThatStopsIt() {
        FakeTower novice = this.choosing(0);

        UpgradePath path = TREE.planPath(A2, novice, this.world);
        InfoSheet sheet = this.sheet(novice, A2);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Blocked: " + TIER_ONE.displayName() + " needs more XP")));
        assertThat(UpgradeSheetText.footer(Optional.of(UpgradeHover.of(A2, novice, this.world))))
                .isEqualTo("3 steps · $" + path.total() + " · blocked: XP");
    }

    @Test
    void downgradingWhatNoWaveHasUsedRefundsItInFullAndTheFooterSaysSo() {
        FakeTower fresh = this.choosing(0, LONE);
        Optional<UpgradeHover> hover = Optional.of(UpgradeHover.of(LONE, fresh, this.world));

        InfoSheet sheet = UpgradeSheetText.downgradeConfirmation((UpgradeHover.Downgrading) hover.get(), TREE);

        assertThat(sheet.lines()).contains(Row.toned(Glyph.CASH, Palette.TERM_STATUS_CASH, "Refund 100%",
                "+$" + LONE.price()));
        assertThat(sheet.lines()).noneMatch(line -> line instanceof Row row && row.label().equals("Refund 75%"));
        assertThat(UpgradeSheetText.footer(hover)).endsWith(" · full");
        assertThat(UpgradeSheetText.footerWarns(hover)).isFalse();
    }

    @Test
    void aDowngradeTakingUsedAndUnusedNodesListsEachRefundApart() {
        FakeTower used = this.choosing(TIER_ONE_XP, A1);
        WorldFixtures.passAWave(this.world);
        used.buyPath(A2);
        UpgradeHover.Downgrading downgrading = (UpgradeHover.Downgrading) UpgradeHover.of(A1, used, this.world);

        InfoSheet sheet = UpgradeSheetText.downgradeConfirmation(downgrading, TREE);

        assertThat(sheet.lines()).containsSubsequence(
                Row.toned(Glyph.CASH, Palette.TERM_STATUS_CASH, "Refund 100%", "+$" + A2.price()),
                Row.toned(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET, "Refund 75%", "+$" + downgrading.refund().shared()));
        assertThat(UpgradeSheetText.footer(Optional.of(downgrading))).doesNotContain("full");
    }

    @Test
    void anOwnedNodesHoverListsWhatADowngradeTakesAndRefundsAndWhatReopens() {
        FakeTower chosen = this.choosing(0, A1);
        WorldFixtures.passAWave(this.world);
        UpgradeHover.Downgrading downgrading = (UpgradeHover.Downgrading) UpgradeHover.of(A1, chosen, this.world);

        InfoSheet sheet = UpgradeSheetText.downgradeConfirmation(downgrading, TREE);

        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.DOT, A1.displayName(), "$" + A1.price()),
                Row.toned(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET, "Refund 75%", "+$" + downgrading.refund().total()),
                Row.plain(Glyph.LOCK, "Reopens", ""),
                Row.plain(Glyph.DOT, B1.displayName() + " line", ""));
        assertThat(UpgradeSheetText.footer(Optional.of(downgrading)))
                .isEqualTo("Right-click: downgrade 1 · +$" + downgrading.refund().total());
        assertThat(UpgradeSheetText.downgradeButton(downgrading))
                .isEqualTo("Downgrade ( +$" + downgrading.refund().total() + " )");
    }

    @Test
    void aSwitchsHoverNamesTheMemberItReplacesAndWhatItSellsBack() {
        FakeTower picked = this.choosing(TIER_ONE_XP, A2);
        WorldFixtures.passAWave(this.world);
        UpgradeHover.Switching switching = (UpgradeHover.Switching) UpgradeHover.of(B1, picked, this.world);

        InfoSheet sheet = this.sheet(picked, B1);

        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.CHEVRON, "Replaces", A1.displayName()),
                Row.plain(Glyph.CROSS, "Sells back", ""),
                Row.plain(Glyph.DOT, A2.displayName(), "$" + A2.price()),
                SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("Click to switch")));
        assertThat(UpgradeSheetText.footer(Optional.of(switching)))
                .isEqualTo("Click: switch · sells 1 · +$" + switching.refund().total());
    }

    @Test
    void whileAWaveIsOutASwitchAndADowngradeSayTheyWaitAndTheFooterWarns() {
        FakeTower picked = this.choosing(0, A1);
        this.world.waves().announce(0);
        Optional<UpgradeHover> switching = Optional.of(UpgradeHover.of(B1, picked, this.world));
        Optional<UpgradeHover> downgrading = Optional.of(UpgradeHover.of(A1, picked, this.world));

        assertThat(UpgradeSheetText.footer(switching)).isEqualTo(UpgradeSheetText.BETWEEN_WAVES);
        assertThat(UpgradeSheetText.footer(downgrading)).isEqualTo(UpgradeSheetText.BETWEEN_WAVES);
        assertThat(UpgradeSheetText.footerWarns(switching)).isTrue();
        assertThat(this.sheet(picked, B1).lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS,
                Palette.UPGRADE_GATE_UNMET, TermText.of(UpgradeSheetText.BETWEEN_WAVES)));
    }

    @Test
    void withNothingHoveredTheFooterSaysWhatTheButtonsDo() {
        assertThat(UpgradeSheetText.footer(Optional.empty())).isEqualTo("Click: buy · Right-click: downgrade");
    }

    @Test
    void aGateHoldsTheXpAtItsOwnOnceMet() {
        assertThat(UpgradeSheetText.gateLabel(40, TIER_ONE_XP)).isEqualTo("XP 40 / " + TIER_ONE_XP);
        assertThat(UpgradeSheetText.gateLabel(TIER_ONE_XP + 5, TIER_ONE_XP))
                .isEqualTo("XP " + TIER_ONE_XP + " / " + TIER_ONE_XP);
    }

    @Test
    void theSinglesFooterCountsWhatTheButtonBuysAndItsTotal() {
        assertThat(UpgradeSheetText.singlesFooter(1, 5)).isEqualTo("1 single · $5");
        assertThat(UpgradeSheetText.singlesFooter(3, 24)).isEqualTo("3 singles · $24");
    }
}
