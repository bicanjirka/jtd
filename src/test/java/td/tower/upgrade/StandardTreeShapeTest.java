package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The shape every tower's tree shares, walked on a fake tree that has every part of it. */
class StandardTreeShapeTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode A4A = UpgradeTier.HEAD_4.node("a.4a", "A IV-A", LIST_PRICE).after(A3);
    private static final UpgradeNode A4B = UpgradeTier.HEAD_4.node("a.4b", "A IV-B", LIST_PRICE).after(A3);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeNode B2 = UpgradeTier.HEAD_2.node("b.2", "B II", LIST_PRICE).after(B1);
    private static final UpgradeNode X1 = UpgradeTier.EXTRA_1.node("x.1", "X", LIST_PRICE);
    private static final UpgradeNode X2 = UpgradeTier.EXTRA_2.node("x.2", "X II", LIST_PRICE).after(X1);
    private static final UpgradeNode X3 = UpgradeTier.EXTRA_3.node("x.3", "X III", LIST_PRICE).after(X2);
    private static final UpgradeNode X4 = UpgradeTier.EXTRA_4.node("x.4", "X IV", LIST_PRICE).after(X3);
    private static final UpgradeNode S1 = UpgradeTier.SPECIAL.node("s.1", "S1", LIST_PRICE);
    private static final UpgradeNode S2 = UpgradeTier.SPECIAL.node("s.2", "S2", LIST_PRICE);
    private static final UpgradeNode S3 = UpgradeTier.SPECIAL.node("s.3", "S3", LIST_PRICE);
    private static final List<UpgradeNode> BASE = StandardBaseSlot.nodes(LIST_PRICE, A3);

    private static final UpgradeTree TREE = UpgradeTree.of(BASE)
            .with(A1, A2, A3, A4A, A4B, B1, B2, X1, X2, X3, X4, S1, S2, S3)
            .withChoice(ExclusiveChoice.oneOf(A1, B1))
            .withChoice(ExclusiveChoice.oneOf(A4A, A4B))
            .withChoice(ExclusiveChoice.specials(S1, S2, S3));

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aFreshTowerIsOfferedRangeAndAttuneOnly() {
        assertThat(this.offeredAfter()).extracting(UpgradeNode::id)
                .containsExactly(StandardBaseSlot.RANGE_ID, StandardBaseSlot.ATTUNE_ID);
    }

    @Test
    void attuneOffersBothChainRootsAndTheExtraNodeButNoSpecial() {
        List<UpgradeNode> offered = this.offeredAfter(base(StandardBaseSlot.ATTUNE_ID));

        assertThat(inSlot(offered, UpgradeSlot.HEAD)).containsExactly(A1, B1, X1);
        assertThat(inSlot(offered, UpgradeSlot.SPECIAL)).isEmpty();
    }

    @Test
    void choosingAChainLocksTheOtherOutButKeepsTheExtraNode() {
        List<UpgradeNode> offered = this.offeredAfter(base(StandardBaseSlot.ATTUNE_ID), A1);

        assertThat(inSlot(offered, UpgradeSlot.HEAD)).containsExactly(A2, X1);
    }

    @Test
    void theExtraNodeLocksNothingOut() {
        List<UpgradeNode> offered = this.offeredAfter(base(StandardBaseSlot.ATTUNE_ID), X1);

        assertThat(inSlot(offered, UpgradeSlot.HEAD)).containsExactly(A1, B1, X2);
    }

    @Test
    void levelThreeOfAChainAndOfTheExtraNodeWaitForAwaken() {
        UpgradeNode attune = base(StandardBaseSlot.ATTUNE_ID);

        List<UpgradeNode> beforeAwaken = this.offeredAfter(attune, A1, A2, X1, X2);
        List<UpgradeNode> afterAwaken = this.offeredAfter(attune, base(StandardBaseSlot.AWAKEN_ID), A1, A2, X1, X2);

        assertThat(beforeAwaken).doesNotContain(A3, X3);
        assertThat(afterAwaken).contains(A3, X3);
    }

    @Test
    void awakenOffersOneSpecialOfThreeAndBuyingOneClosesTheSlot() {
        UpgradeNode attune = base(StandardBaseSlot.ATTUNE_ID);
        UpgradeNode awaken = base(StandardBaseSlot.AWAKEN_ID);

        assertThat(inSlot(this.offeredAfter(attune, awaken), UpgradeSlot.SPECIAL)).containsExactly(S1, S2, S3);
        assertThat(inSlot(this.offeredAfter(attune, awaken, S1), UpgradeSlot.SPECIAL)).isEmpty();
    }

    @Test
    void transcendentOffersTheSecondSpecialFromTheSameSetAndThenLocksTheLastOut() {
        List<UpgradeNode> transcended = List.of(base(StandardBaseSlot.ATTUNE_ID), base(StandardBaseSlot.AWAKEN_ID),
                A1, A2, A3, S1, base(StandardBaseSlot.TRANSCENDENT_ID));

        List<UpgradeNode> secondSlot = inSlot(this.offeredAfter(transcended), UpgradeSlot.SPECIAL);
        List<UpgradeNode> bothFilled = inSlot(this.offeredAfter(plus(transcended, S2)), UpgradeSlot.SPECIAL);

        assertThat(secondSlot).containsExactly(S2, S3);
        assertThat(bothFilled).isEmpty();
    }

    @Test
    void transcendentTakesAwakensPlaceAndSaysWhatItStillNeeds() {
        UpgradeNode transcendent = base(StandardBaseSlot.TRANSCENDENT_ID);
        UpgradeNode attune = base(StandardBaseSlot.ATTUNE_ID);
        UpgradeNode awaken = base(StandardBaseSlot.AWAKEN_ID);
        Tower awakened = this.towerOwning(attune, awaken);
        Tower withSpecial = this.towerOwning(attune, awaken, S1);
        Tower ready = this.towerOwning(attune, awaken, S1, A1, A2, A3);

        assertThat(TREE.offered(awakened, this.context)).contains(transcendent).doesNotContain(awaken);
        assertThat(transcendent.gate().progress(awakened, this.context)).isEqualTo("needs a special and a level III head");
        assertThat(transcendent.gate().progress(withSpecial, this.context)).isEqualTo("needs a level III head");
        assertThat(transcendent.gate().isSatisfied(withSpecial, this.context)).isFalse();
        assertThat(transcendent.gate().isSatisfied(ready, this.context)).isTrue();
    }

    @Test
    void theExtraNodesLevelThreeDoesNotCountTowardTranscendent() {
        Tower tower = this.towerOwning(base(StandardBaseSlot.ATTUNE_ID), base(StandardBaseSlot.AWAKEN_ID), S1, X1, X2, X3);

        assertThat(base(StandardBaseSlot.TRANSCENDENT_ID).gate().isSatisfied(tower, this.context)).isFalse();
    }

    @Test
    void levelFourOffersTwoBranchesAndBuyingOneLocksTheOtherOut() {
        List<UpgradeNode> transcended = List.of(base(StandardBaseSlot.ATTUNE_ID), base(StandardBaseSlot.AWAKEN_ID),
                A1, A2, A3, S1, base(StandardBaseSlot.TRANSCENDENT_ID), X1, X2, X3);

        assertThat(inSlot(this.offeredAfter(transcended), UpgradeSlot.HEAD)).containsExactly(A4A, A4B, X4);
        assertThat(inSlot(this.offeredAfter(plus(transcended, A4A)), UpgradeSlot.HEAD)).containsExactly(X4);
    }

    @Test
    void levelFourAndTheExtraNodesFourthLevelWaitForTranscendent() {
        List<UpgradeNode> offered = this.offeredAfter(base(StandardBaseSlot.ATTUNE_ID), base(StandardBaseSlot.AWAKEN_ID),
                A1, A2, A3, X1, X2, X3);

        assertThat(offered).doesNotContain(A4A, A4B, X4);
    }

    @Test
    void rangeTwoWaitsForAwakenAndRangeThreeForTranscendent() {
        UpgradeNode range1 = base(StandardBaseSlot.RANGE_ID);
        UpgradeNode range2 = base(StandardBaseSlot.RANGE_2_ID);
        UpgradeNode range3 = base(StandardBaseSlot.RANGE_3_ID);
        UpgradeNode attune = base(StandardBaseSlot.ATTUNE_ID);
        UpgradeNode awaken = base(StandardBaseSlot.AWAKEN_ID);

        assertThat(this.offeredAfter(range1, attune)).doesNotContain(range2);
        assertThat(this.offeredAfter(range1, attune, awaken)).contains(range2);
        assertThat(this.offeredAfter(range1, range2, attune, awaken)).doesNotContain(range3);
        assertThat(this.offeredAfter(range1, range2, attune, awaken, base(StandardBaseSlot.TRANSCENDENT_ID)))
                .contains(range3);
    }

    @Test
    void aTreeWithNoLevelThreeHeadHasNoTranscendentAndNoRangeThree() {
        assertThat(StandardBaseSlot.nodes(LIST_PRICE)).extracting(UpgradeNode::id)
                .doesNotContain(StandardBaseSlot.TRANSCENDENT_ID, StandardBaseSlot.RANGE_3_ID);
    }

    @Test
    void specialsPerChainWaitForTheirChainsRoot() {
        UpgradeNode forA = UpgradeTier.SPECIAL.node("s.a", "For A", LIST_PRICE).after(A1);
        UpgradeNode forB = UpgradeTier.SPECIAL.node("s.b", "For B", LIST_PRICE).after(B1);
        UpgradeTree tree = UpgradeTree.of(BASE).with(A1, B1, forA, forB).withChoice(ExclusiveChoice.oneOf(A1, B1));
        UpgradeNode attune = base(StandardBaseSlot.ATTUNE_ID);
        UpgradeNode awaken = base(StandardBaseSlot.AWAKEN_ID);

        List<UpgradeNode> noChain = tree.offered(this.towerOwning(attune, awaken), this.context);
        List<UpgradeNode> chainA = tree.offered(this.towerOwning(attune, awaken, A1), this.context);

        assertThat(inSlot(noChain, UpgradeSlot.SPECIAL)).isEmpty();
        assertThat(inSlot(chainA, UpgradeSlot.SPECIAL)).containsExactly(forA);
    }

    @Test
    void aChoiceOnOfferNamesItsRivalsAndANodeOutsideAChoiceHasNone() {
        List<UpgradeNode> offered = this.offeredAfter(base(StandardBaseSlot.ATTUNE_ID));

        assertThat(TREE.rivalsOnOffer(A1, offered)).containsExactly(B1);
        assertThat(TREE.rivalsOnOffer(X1, offered)).isEmpty();
    }

    @Test
    void aMadeChoiceRemembersWhatItWasChosenOver() {
        UpgradeState owned = state(base(StandardBaseSlot.ATTUNE_ID), X1, B1);

        assertThat(TREE.decisions(owned)).containsExactly(new UpgradeDecision(B1, List.of(A1)));
    }

    @Test
    void stepPricesFollowTheListPriceRoundedHalfUp() {
        assertThat(UpgradeTier.HEAD_2.price(15)).isEqualTo(23);
        assertThat(UpgradeTier.RANGE_1.price(25)).isEqualTo(15);
        assertThat(UpgradeTier.SPECIAL.price(LIST_PRICE)).isEqualTo(40);
    }

    @Test
    void aChoiceNamingANodeOutsideTheTreeOrAlreadyInAnotherChoiceIsRejected() {
        assertThatThrownBy(() -> UpgradeTree.of(A1).withChoice(ExclusiveChoice.oneOf(A1, B1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UpgradeTree.of(A1, B1, X1)
                .withChoice(ExclusiveChoice.oneOf(A1, B1))
                .withChoice(ExclusiveChoice.oneOf(B1, X1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aChoiceAllowingAPickForEveryMemberIsRejected() {
        assertThatThrownBy(() -> ExclusiveChoice.specials(S1, S2)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<UpgradeNode> offeredAfter(UpgradeNode... owned) {
        return TREE.offered(this.towerOwning(owned), this.context);
    }

    private List<UpgradeNode> offeredAfter(List<UpgradeNode> owned) {
        return this.offeredAfter(owned.toArray(UpgradeNode[]::new));
    }

    private Tower towerOwning(UpgradeNode... owned) {
        return FakeTower.owning(this.context, 0, 0, state(owned));
    }

    private static UpgradeState state(UpgradeNode... owned) {
        return new UpgradeState(List.of(owned));
    }

    private static List<UpgradeNode> plus(List<UpgradeNode> owned, UpgradeNode next) {
        return Stream.concat(owned.stream(), Stream.of(next)).toList();
    }

    private static List<UpgradeNode> inSlot(List<UpgradeNode> nodes, UpgradeSlot slot) {
        return nodes.stream().filter(n -> n.slot() == slot).toList();
    }

    private static UpgradeNode base(String id) {
        return BASE.stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }
}
