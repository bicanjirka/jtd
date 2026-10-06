package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One tower type's upgrade nodes across all slots, and the exclusive choices among them.
 * {@link #offered(Tower, GameWorld)} is the query every caller shares.
 */
public record UpgradeTree(List<UpgradeNode> nodes, List<ExclusiveChoice> choices) {

    private static final UpgradeTree NONE = new UpgradeTree(List.of(), List.of());

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
        return this.nodes.stream()
                .filter(n -> this.offers(n, tower, context))
                .sorted(Comparator.comparing(UpgradeNode::slot))
                .toList();
    }

    /** Whether {@code node} is one of {@link #offered}: the check a purchase makes. */
    public boolean offers(UpgradeNode node, Tower tower, GameWorld context) {
        UpgradeState owned = tower.upgrades();
        return this.nodes.contains(node)
                && !owned.owns(node.id())
                && this.choiceOf(node).map(choice -> !choice.isSpent(owned)).orElse(true)
                && node.requires().isSatisfied(tower, context);
    }

    public Optional<ExclusiveChoice> choiceOf(UpgradeNode node) {
        return this.choices.stream().filter(choice -> choice.contains(node.id())).findFirst();
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
