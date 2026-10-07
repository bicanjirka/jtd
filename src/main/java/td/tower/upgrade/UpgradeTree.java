package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One tower type's upgrade nodes across all slots, and the exclusive choices among them.
 * {@link #offered(Tower, GameWorld)} is the query every caller shares.
 */
public record UpgradeTree(List<UpgradeNode> nodes, List<ExclusiveChoice> choices) {

    private static final UpgradeTree NONE = new UpgradeTree(List.of(), List.of());
    private static final UpgradeSlot[] SLOTS = UpgradeSlot.values();

    /**
     * Rejects duplicate node ids, which would make matching by id ambiguous, and a choice naming a
     * node outside the tree or a node already in another choice.
     */
    public UpgradeTree {
        Set<String> ids = new HashSet<>();
        for (UpgradeNode node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("Duplicate upgrade node id: " + node.id());
            }
        }
        Set<String> chosen = new HashSet<>();
        for (ExclusiveChoice choice : choices) {
            for (String id : choice.nodeIds()) {
                if (!ids.contains(id) || !chosen.add(id)) {
                    throw new IllegalArgumentException("Choice member missing or in two choices: " + id);
                }
            }
        }
        nodes = List.copyOf(nodes);
        choices = List.copyOf(choices);
    }

    public static UpgradeTree none() {
        return NONE;
    }

    public static UpgradeTree of(UpgradeNode... nodes) {
        return new UpgradeTree(List.of(nodes), List.of());
    }

    public static UpgradeTree of(List<UpgradeNode> nodes) {
        return new UpgradeTree(nodes, List.of());
    }

    /** This tree with {@code more} nodes declared after its own. */
    public UpgradeTree with(UpgradeNode... more) {
        List<UpgradeNode> next = new ArrayList<>(this.nodes);
        next.addAll(Arrays.asList(more));
        return new UpgradeTree(next, this.choices);
    }

    public UpgradeTree withChoice(ExclusiveChoice choice) {
        List<ExclusiveChoice> next = new ArrayList<>(this.choices);
        next.add(choice);
        return new UpgradeTree(this.nodes, next);
    }

    public List<UpgradeNode> nodesIn(UpgradeSlot slot) {
        return this.nodes.stream().filter(n -> n.slot() == slot).toList();
    }

    /**
     * Unowned nodes whose prerequisites are met and whose choice still has a pick left, by slot
     * and then declaration order - the order the upgrade panel and number keys use. A node that
     * isn't offered is absent, not disabled.
     */
    public List<UpgradeNode> offered(Tower tower, GameWorld context) {
        List<UpgradeNode> offered = new ArrayList<>();
        for (UpgradeSlot slot : SLOTS) {
            for (int i = 0; i < this.nodes.size(); i++) {
                UpgradeNode node = this.nodes.get(i);
                if (node.slot() == slot && this.isOffered(node, tower, context)) {
                    offered.add(node);
                }
            }
        }
        return offered;
    }

    /** Whether {@code node} is one of {@link #offered}: the check a purchase makes. */
    public boolean offers(UpgradeNode node, Tower tower, GameWorld context) {
        return this.nodes.contains(node) && this.isOffered(node, tower, context);
    }

    /**
     * {@code target} preceded by every node a tower must own first, in the order to buy them: the chain
     * before it and the base nodes that open it. Where the base chain gives a choice, the cheaper side.
     */
    public List<UpgradeNode> pathTo(UpgradeNode target) {
        Set<UpgradeNode> path = new LinkedHashSet<>();
        this.collectPath(target, path);
        return List.copyOf(path);
    }

    private void collectPath(UpgradeNode node, Set<UpgradeNode> path) {
        for (String id : node.requires().requiredNodes().stream().sorted().toList()) {
            this.nodes.stream().filter(candidate -> candidate.id().equals(id)).findFirst()
                    .ifPresent(required -> this.collectPath(required, path));
        }
        path.add(node);
    }

    public Optional<ExclusiveChoice> choiceOf(UpgradeNode node) {
        return Optional.ofNullable(this.choiceContaining(node));
    }

    // Allocation-free: offered() runs for every tower on every frame build.
    private boolean isOffered(UpgradeNode node, Tower tower, GameWorld context) {
        UpgradeState owned = tower.upgrades();
        if (owned.owns(node.id())) {
            return false;
        }
        ExclusiveChoice choice = this.choiceContaining(node);
        return (choice == null || !choice.isSpent(owned)) && node.requires().isSatisfied(tower, context);
    }

    private ExclusiveChoice choiceContaining(UpgradeNode node) {
        for (int i = 0; i < this.choices.size(); i++) {
            if (this.choices.get(i).contains(node.id())) {
                return this.choices.get(i);
            }
        }
        return null;
    }

    /**
     * The other members of {@code node}'s choice in {@code offered}: what buying it would lock
     * out, or would leave fewer picks for. Empty for a node in no choice.
     */
    public List<UpgradeNode> rivalsOnOffer(UpgradeNode node, List<UpgradeNode> offered) {
        return this.choiceOf(node)
                .map(choice -> offered.stream().filter(n -> !n.equals(node) && choice.contains(n.id())).toList())
                .orElse(List.of());
    }

    /**
     * Whether buying {@code node} uses its choice's last pick, locking out for good the members
     * still unowned. False for a node in no choice, and for a special while a second slot is left.
     */
    public boolean isLastPick(UpgradeNode node, UpgradeState owned) {
        ExclusiveChoice choice = this.choiceContaining(node);
        return choice != null && choice.picksLeft(owned) == 1;
    }

    /**
     * Every owned node that belongs to a choice, in purchase order, with the members of its
     * choice still unowned.
     */
    public List<UpgradeDecision> decisions(UpgradeState owned) {
        List<UpgradeDecision> decisions = new ArrayList<>();
        for (UpgradeNode node : owned.owned()) {
            this.choiceOf(node).ifPresent(choice -> decisions.add(new UpgradeDecision(node, this.nodes.stream()
                    .filter(n -> choice.contains(n.id()) && !owned.owns(n.id()))
                    .toList())));
        }
        return decisions;
    }
}
