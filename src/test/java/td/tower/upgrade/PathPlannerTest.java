package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How a path to a node is planned: its steps, what stops it and what it locks out. */
class PathPlannerTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeNode B2 = UpgradeTier.HEAD_2.node("b.2", "B II", LIST_PRICE).after(B1);
    private static final UpgradeNode FOR_A = UpgradeTier.SPECIAL.node("s.a", "For A", LIST_PRICE).after(A1);
    private static final UpgradeNode FREE = UpgradeTier.SPECIAL.node("s.free", "Free", LIST_PRICE);
    private static final UpgradeNode OTHER = UpgradeTier.SPECIAL.node("s.other", "Other", LIST_PRICE);
    private static final List<UpgradeNode> SHARED = StandardBaseSlot.nodes(LIST_PRICE, 4f, A3);

    private static final UpgradeTree TREE = UpgradeTree.of(SHARED)
            .with(A1, A2, A3, B1, B2, FOR_A, FREE, OTHER)
            .withChoice(ExclusiveChoice.oneOf(A1, B1))
            .withChoice(ExclusiveChoice.specials(FOR_A, FREE, OTHER));

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aPathGathersTheCoreStepsAndTheLineBeforeItsTargetInAnOrderThatCanBeBought() {
        Tower tower = this.towerOwning();
        tower.earnXp(1_000);

        UpgradePath path = TREE.planPath(A3, tower, this.context);

        assertThat(path.steps()).extracting(UpgradeNode::displayName)
                .containsExactlyInAnyOrder("Attune", "Awaken", "A", "A II", "A III").endsWith("A III");
        assertThat(path.isOpen()).isTrue();
        assertThat(path.total()).isEqualTo(path.steps().stream().mapToInt(UpgradeNode::price).sum());
    }

    @Test
    void aPathSkipsWhatIsOwnedAlready() {
        UpgradePath path = TREE.planPath(A3, this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), A1), this.context);

        assertThat(path.steps()).extracting(UpgradeNode::displayName).containsExactlyInAnyOrder("Awaken", "A II", "A III");
    }

    @Test
    void theFirstStepShortOfXpBlocksThePath() {
        UpgradePath path = TREE.planPath(A3, this.towerOwning(), this.context);

        assertThat(path.isOpen()).isFalse();
        assertThat(path.block()).contains(new UpgradePath.Block(A2, UpgradePath.Reason.XP));
    }

    @Test
    void aGateIsJudgedAsIfTheEarlierStepsWereOwned() {
        UpgradeNode gated = UpgradeTier.EXTRA_1.node("g", "Gated", LIST_PRICE).after(A2)
                .withGate(UpgradeCondition.owns(A2.id()));
        UpgradeTree tree = UpgradeTree.of(SHARED).with(A1, A2, A3, gated);
        Tower tower = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), A1);
        tower.earnXp(1_000);

        UpgradePath path = tree.planPath(gated, tower, this.context);

        assertThat(path.steps()).containsExactly(A2, gated);
        assertThat(path.isOpen()).isTrue();
    }

    @Test
    void anUnmetGateBlocksThePathAtItsStep() {
        UpgradeNode transcendent = core(StandardBaseSlot.TRANSCENDENT_ID);
        Tower tower = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), core(StandardBaseSlot.AWAKEN_ID), A1, A2, FREE);
        tower.earnXp(1_000);

        UpgradePath path = TREE.planPath(transcendent, tower, this.context);

        assertThat(path.block()).contains(new UpgradePath.Block(transcendent, UpgradePath.Reason.GATE));
    }

    @Test
    void aPathNeverMakesAnExclusivePickOffItsTargetsLine() {
        Tower tower = this.towerOwning();
        tower.earnXp(1_000);

        UpgradePath path = TREE.planPath(FOR_A, tower, this.context);

        assertThat(path.block()).contains(new UpgradePath.Block(A1, UpgradePath.Reason.CHOOSE_FIRST));
        assertThat(path.steps()).doesNotContain(A1);
    }

    @Test
    void aPathToANodeARivalClosedIsLockedOut() {
        UpgradePath path = TREE.planPath(B2, this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), A1), this.context);

        assertThat(path.steps()).isEmpty();
        assertThat(path.block()).contains(new UpgradePath.Block(B2, UpgradePath.Reason.LOCKED_OUT));
    }

    @Test
    void aSecondSpecialsPathBuysTranscendentFirst() {
        Tower tower = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), core(StandardBaseSlot.AWAKEN_ID), FREE);

        UpgradePath path = TREE.planPath(OTHER, tower, this.context);

        assertThat(path.steps()).extracting(UpgradeNode::displayName).containsExactly("Transcendent", "Other");
    }

    @Test
    void aPathNamesWhatItsPicksLockOut() {
        UpgradePath path = TREE.planPath(A2, this.towerOwning(), this.context);

        assertThat(path.locksOut()).containsExactly(B1, B2);
    }

    private Tower towerOwning(UpgradeNode... owned) {
        return FakeTower.owning(this.context, 0, 0, new UpgradeState(List.of(owned)));
    }

    private static UpgradeNode core(String id) {
        return SHARED.stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }
}
