package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.TierTrees;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.A3;
import static td.fixtures.TierTrees.ALL_XP;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TOP;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.towerOwning;

/** How a path to a node is planned: its steps, what stops it and what it locks out. */
class PathPlannerTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aPathGathersTheUnlocksAndTheLineBeforeItsTargetInAnOrderThatCanBeBought() {
        Tower tower = towerOwning(this.context, ALL_XP);

        UpgradePath path = TREE.planPath(A3, tower, this.context);

        assertThat(path.steps()).containsExactly(TIER_ONE, A1, A2, A3);
        assertThat(path.isOpen()).isTrue();
        assertThat(path.total()).isEqualTo(path.steps().stream().mapToInt(UpgradeNode::price).sum());
    }

    @Test
    void aPathSkipsWhatIsOwnedAlready() {
        UpgradePath path = TREE.planPath(A3, towerOwning(this.context, ALL_XP, A1, TIER_ONE), this.context);

        assertThat(path.steps()).containsExactly(A2, A3);
    }

    @Test
    void theFirstStepShortOfXpBlocksThePath() {
        UpgradePath path = TREE.planPath(A3, towerOwning(this.context, 0), this.context);

        assertThat(path.isOpen()).isFalse();
        assertThat(path.block()).contains(new UpgradePath.Block(TIER_ONE, UpgradePath.Reason.XP));
    }

    @Test
    void aStepInATierWhoseXpIsShortBlocksThePathAtThatStep() {
        int xp = UpgradeTier.standard().get(1).xp();

        UpgradePath path = TREE.planPath(TOP, towerOwning(this.context, xp, A1, TIER_ONE), this.context);

        assertThat(path.block()).contains(new UpgradePath.Block(A3, UpgradePath.Reason.XP));
    }

    @Test
    void aPathToAClusterMembersFollowUpMakesThatPick() {
        UpgradePath path = TREE.planPath(B2, towerOwning(this.context, ALL_XP), this.context);

        assertThat(path.steps()).containsExactly(TIER_ONE, B1, B2);
        assertThat(path.isOpen()).isTrue();
    }

    @Test
    void aPathNeverMakesAClusterPickOffItsTargetsLine() {
        UpgradeNode afterBoth = UpgradeNode.of("both", "Both", 5).inTier(1).after(A1);
        UpgradeNode lone = UpgradeNode.of("lone.after", "Lone After", 5).inTier(1)
                .withRequires(UpgradeCondition.owns(afterBoth.id()).and(UpgradeCondition.owns(B1.id())));
        UpgradeTree tree = UpgradeTree.of(A1, B1, afterBoth, lone).withCluster(TierTrees.ROOTS);

        UpgradePath path = tree.planPath(lone, towerOwning(this.context, ALL_XP), this.context);

        assertThat(path.block()).contains(new UpgradePath.Block(B1, UpgradePath.Reason.CHOOSE_FIRST));
        assertThat(path.steps()).doesNotContain(B1);
    }

    @Test
    void aPathToANodeBehindTheOtherPickIsLockedOut() {
        UpgradePath path = TREE.planPath(B2, towerOwning(this.context, ALL_XP, A1), this.context);

        assertThat(path.steps()).isEmpty();
        assertThat(path.block()).contains(new UpgradePath.Block(B2, UpgradePath.Reason.LOCKED_OUT));
    }

    @Test
    void aPathNamesWhatItsPicksLockOut() {
        UpgradePath path = TREE.planPath(A2, towerOwning(this.context, ALL_XP), this.context);

        assertThat(path.locksOut()).containsExactly(B1, B2);
    }
}
