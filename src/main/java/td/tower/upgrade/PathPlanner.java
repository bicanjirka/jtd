package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Works out one {@link UpgradePath}: gathers what the target lacks, prerequisites first, on a state that assumes each
 * gathered step bought, puts the unlocks of the paid tiers on the way before them, then judges the steps in order the
 * way a purchase would.
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
        if (this.tree.node(this.target.id()).filter(this.target::equals).isEmpty()) {
            return this.blocked(UpgradePath.Reason.UNREACHABLE);
        }
        if (this.tree.isLockedOut(this.target, owned)) {
            return this.blocked(UpgradePath.Reason.LOCKED_OUT);
        }
        this.gather(this.target);
        List<UpgradeNode> ordered = new ArrayList<>();
        int top = this.steps.stream().mapToInt(UpgradeNode::tier).max().orElse(0);
        for (int tier = 1; tier <= top; tier++) {
            this.tree.unlockOf(tier).filter(unlock -> !owned.owns(unlock.id()) && !this.steps.contains(unlock))
                    .ifPresent(ordered::add);
        }
        ordered.addAll(this.steps);
        UpgradeState state = owned;
        for (UpgradeNode step : ordered) {
            UpgradeState before = state;
            this.judge(step, before).ifPresent(reason -> this.blockAt(step, reason));
            state = state.with(step);
        }
        return new UpgradePath(this.target, ordered, Optional.ofNullable(this.block),
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
                this.blockAt(new UpgradePath.Block(needed.get(), UpgradePath.Reason.CHOOSE_FIRST,
                        this.alternativesTo(id, node.requires())));
            } else if (!this.assumed.owns(id)) {
                this.gather(needed.get());
            }
        }
        if (!this.assumed.owns(node.id())) {
            this.steps.add(node);
            this.assumed = this.assumed.with(node);
        }
    }

    /**
     * A cluster's pick the player didn't click, off the target's line: the path must not make it for them. A pick the
     * target's line starts from, or whose branch the target belongs to, is the player's by clicking the target.
     */
    private boolean offTheTargetsLine(UpgradeNode node) {
        if (this.tree.clusterOf(node).isEmpty() || this.tree.firstOf(this.target).equals(node)) {
            return false;
        }
        Optional<UpgradeNode> member = this.tree.branchOf(this.target);
        while (member.isPresent()) {
            if (member.get().equals(node)) {
                return false;
            }
            member = this.tree.branchOf(member.get());
        }
        return true;
    }

    /** The other nodes that, owned instead of {@code id}, would leave {@code requires} no longer missing it. */
    private List<UpgradeNode> alternativesTo(String id, UpgradeCondition requires) {
        return requires.mentionedNodes().stream().sorted()
                .filter(other -> !other.equals(id) && !this.assumed.owns(other))
                .flatMap(other -> this.tree.node(other).stream())
                .filter(other -> !requires.missingNodes(this.tower, this.assumed.with(other), this.context).contains(id))
                .toList();
    }

    private Optional<UpgradePath.Reason> judge(UpgradeNode step, UpgradeState state) {
        if (this.tree.isLockedOut(step, state)) {
            return Optional.of(UpgradePath.Reason.LOCKED_OUT);
        }
        if (!this.tree.xpMet(step.tier(), this.tower, this.context)) {
            return Optional.of(UpgradePath.Reason.XP);
        }
        if (!this.tree.isUnlock(step) && !step.requires().isSatisfied(this.tower, state, this.context)) {
            return Optional.of(UpgradePath.Reason.UNREACHABLE);
        }
        return Optional.empty();
    }

    private void blockAt(UpgradeNode node, UpgradePath.Reason reason) {
        this.blockAt(new UpgradePath.Block(node, reason));
    }

    private void blockAt(UpgradePath.Block first) {
        if (this.block == null) {
            this.block = first;
        }
    }
}
