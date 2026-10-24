package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Works out one {@link UpgradePath}: gathers what the target lacks, prerequisites first, on a state that
 * assumes each gathered step bought, then judges the steps in order the way a purchase would.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class PathPlanner {

    private final UpgradeTree tree;
    private final Tower tower;
    private final GameWorld context;
    private final UpgradeNode target;
    private final List<UpgradeNode> steps = new ArrayList<>();
    private UpgradeState assumed;
    private UpgradePath.Block block;

    private PathPlanner(UpgradeTree tree, Tower tower, GameWorld context, UpgradeNode target) {
        this.tree = tree;
        this.tower = tower;
        this.context = context;
        this.target = target;
        this.assumed = tower.upgrades();
    }

    static UpgradePath plan(UpgradeTree tree, UpgradeNode target, Tower tower, GameWorld context) {
        return new PathPlanner(tree, tower, context, target).plan();
    }

    private UpgradePath plan() {
        UpgradeState owned = this.tower.upgrades();
        if (owned.owns(this.target.id())) {
            return this.blocked(UpgradePath.Reason.OWNED);
        }
        if (!this.tree.nodes().contains(this.target)) {
            return this.blocked(UpgradePath.Reason.UNREACHABLE);
        }
        if (this.tree.isLockedOut(this.target, owned)) {
            return this.blocked(UpgradePath.Reason.LOCKED_OUT);
        }
        this.gather(this.target);
        UpgradeState state = owned;
        for (UpgradeNode step : this.steps) {
            this.judge(step, state).ifPresent(reason -> this.blockAt(step, reason));
            state = state.with(step);
        }
        return new UpgradePath(this.target, this.steps, Optional.ofNullable(this.block),
                this.tree.newlyLockedOut(owned, state));
    }

    private UpgradePath blocked(UpgradePath.Reason reason) {
        return new UpgradePath(this.target, List.of(), Optional.of(new UpgradePath.Block(this.target, reason)), List.of());
    }

    private void gather(UpgradeNode node) {
        List<String> missing = node.requires().missingNodes(this.tower, this.assumed, this.context).stream().sorted().toList();
        for (String id : missing) {
            Optional<UpgradeNode> needed = this.tree.node(id);
            if (needed.isEmpty()) {
                this.blockAt(node, UpgradePath.Reason.UNREACHABLE);
            } else if (this.tree.isLockedOut(needed.get(), this.assumed)) {
                this.blockAt(needed.get(), UpgradePath.Reason.LOCKED_OUT);
            } else if (this.offTheTargetsLine(needed.get())) {
                this.blockAt(needed.get(), UpgradePath.Reason.CHOOSE_FIRST);
            } else if (!this.assumed.owns(id)) {
                this.gather(needed.get());
            }
        }
        if (!this.assumed.owns(node.id())) {
            this.steps.add(node);
            this.assumed = this.assumed.with(node);
        }
    }

    /** An exclusive pick the player didn't click, on another line: the path must not make that choice for them. */
    private boolean offTheTargetsLine(UpgradeNode node) {
        return this.tree.choiceOf(node).isPresent() && !this.tree.firstOf(node).equals(this.tree.firstOf(this.target));
    }

    private Optional<UpgradePath.Reason> judge(UpgradeNode step, UpgradeState state) {
        boolean waived = this.context.playtestRules().upgradeGatesIgnored();
        if (this.tree.isLockedOut(step, state)) {
            return Optional.of(UpgradePath.Reason.LOCKED_OUT);
        }
        if (!step.requires().isSatisfied(this.tower, state, this.context)) {
            return Optional.of(UpgradePath.Reason.UNREACHABLE);
        }
        if (!waived && !step.xpMet(this.tower)) {
            return Optional.of(UpgradePath.Reason.XP);
        }
        if (!waived && !step.gate().isSatisfied(this.tower, state, this.context)) {
            return Optional.of(UpgradePath.Reason.GATE);
        }
        return Optional.empty();
    }

    private void blockAt(UpgradeNode node, UpgradePath.Reason reason) {
        if (this.block == null) {
            this.block = new UpgradePath.Block(node, reason);
        }
    }
}
