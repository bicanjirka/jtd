package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.term.TermNumbers;
import td.term.TermText;
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
    }

    @Test
    void duringFreeBuildAPathTheCashDoesNotCoverIsStillOneClickBuys() {
        FakeTower fresh = this.choosing(TIER_ONE_XP);
        this.world.economy().startEconomy(1, 5);
        this.world.economy().setFreeBuild(true);

        InfoSheet sheet = this.sheet(fresh, MID);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET,
                TermText.of("Click to buy all 2")));
    }

    @Test
    void aBlockedPathNamesTheStepThatStopsIt() {
        FakeTower novice = this.choosing(0);

        InfoSheet sheet = this.sheet(novice, A2);

        assertThat(sheet.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Blocked: " + TIER_ONE.displayName() + " needs more XP")));
    }

    @Test
    void downgradingWhatNoWaveHasUsedRefundsItInFull() {
        FakeTower fresh = this.choosing(0, LONE);
        UpgradeHover.Downgrading downgrading = (UpgradeHover.Downgrading) UpgradeHover.of(LONE, fresh, this.world);

        InfoSheet sheet = UpgradeSheetText.downgradeConfirmation(downgrading, TREE);

        assertThat(sheet.lines()).contains(Row.toned(Glyph.CASH, Palette.TERM_STATUS_CASH, "Refund 100%",
                "+$" + LONE.price()));
        assertThat(sheet.lines()).noneMatch(line -> line instanceof Row row && row.label().equals("Refund 75%"));
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
        assertThat(UpgradeSheetText.downgradeButton(downgrading))
                .isEqualTo("Downgrade ( +$" + downgrading.refund().total() + " )");
    }

    @Test
    void aSwitchsHoverNamesTheMemberItReplacesAndWhatItSellsBack() {
        FakeTower picked = this.choosing(TIER_ONE_XP, A2);
        WorldFixtures.passAWave(this.world);

        InfoSheet sheet = this.sheet(picked, B1);

        assertThat(sheet.lines()).containsSubsequence(
                Row.plain(Glyph.CHEVRON, "Replaces", A1.displayName()),
                Row.plain(Glyph.CROSS, "Sells back", ""),
                Row.plain(Glyph.DOT, A2.displayName(), "$" + A2.price()),
                SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("Click to switch")));
    }

    @Test
    void whileAWaveIsOutASwitchAndADowngradeSayTheyWait() {
        FakeTower picked = this.choosing(0, A1);
        this.world.waves().announce(0);

        SheetLine waits = SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of(UpgradeSheetText.BETWEEN_WAVES));

        assertThat(this.sheet(picked, B1).lines()).contains(waits);
        assertThat(this.sheet(picked, A1).lines()).contains(waits);
    }

    @Test
    void aGateHoldsTheXpAtItsOwnOnceMet() {
        assertThat(UpgradeSheetText.gateLabel(40, TIER_ONE_XP)).isEqualTo("XP 40 / " + TIER_ONE_XP);
        assertThat(UpgradeSheetText.gateLabel(TIER_ONE_XP + 5, TIER_ONE_XP))
                .isEqualTo("XP " + TIER_ONE_XP + " / " + TIER_ONE_XP);
    }

    @Test
    void theSinglesHoverListsWhatTheStripBuysWithItsTotalAndSaysWhenTheCreditsCoverNone() {
        InfoSheet affordable = UpgradeSheetText.singles(List.of(A1, LONE), "1", false);
        InfoSheet shortOfCash = UpgradeSheetText.singles(List.of(A1), "1", true);

        assertThat(affordable.lines()).containsSubsequence(
                new SheetLine.Title(Glyph.CASH, Palette.TERM_STATUS_CASH, "Buy singles", "$" + (A1.price() + LONE.price())),
                Row.plain(Glyph.DOT, A1.displayName(), "$" + A1.price()),
                Row.plain(Glyph.DOT, LONE.displayName(), "$" + LONE.price()));
        assertThat(shortOfCash.lines()).contains(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                TermText.of("Short of credits")));
    }

    @Test
    void aGatesHoverNamesItsTierWithTheXpTowardItAndWhatIsLeftToEarn() {
        InfoSheet sheet = UpgradeSheetText.gate(1, 40, 100);

        assertThat(sheet.lines()).containsExactly(
                new SheetLine.Title(Glyph.LOCK, Palette.UPGRADE_GATE_UNMET, "Tier I locked", "XP 40 / 100"),
                SheetLine.Sentence.of(TermText.of("Earn 60 more XP to open it")));
    }
}
