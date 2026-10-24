package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One tower type's upgrade nodes across all slots, and the exclusive choices among them.
 * {@link #offered(Tower, GameWorld)} is the query every caller shares.
 */
public record UpgradeTree(List<UpgradeNode> nodes, List<ExclusiveChoice> choices) {

    private static final UpgradeTree NONE = new UpgradeTree(List.of(), List.of());
    private static final UpgradeSlot[] SLOTS = UpgradeSlot.values();
    private static final int CHAIN_KEY = 1;
    private static final int RANGE_KEY = 2;
    private static final int EXTRA_KEY = 3;
    private static final int FIRST_BRANCH_KEY = 4;
    private static final int BRANCHES = 2;
    private static final int FIRST_SPECIAL_KEY = 6;
    private static final int SPECIAL_KEYS = 3;

    /**
     * Rejects duplicate node ids, which would make matching by id ambiguous, a choice naming a
     * node outside the tree or a node already in another choice, and a node no number key buys.
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
        for (UpgradeNode node : nodes) {
            if (keyOf(node, nodes, choices) == 0) {
                throw new IllegalArgumentException("No number key buys upgrade node: " + node.id());
            }
        }
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

    /** {@code owned}'s nodes grouped by line, in the order each line's first node was bought. */
    public List<UpgradeLine> lines(UpgradeState owned) {
        Map<UpgradeNode, List<UpgradeNode>> lines = new LinkedHashMap<>();
        for (UpgradeNode node : owned.owned()) {
            lines.computeIfAbsent(this.firstOf(node), first -> new ArrayList<>()).add(node);
        }
        return lines.entrySet().stream().map(line -> new UpgradeLine(line.getKey(), line.getValue())).toList();
    }

    /** The node whose art a node or line for {@code node} shows: a Core step, level IV or special its own, else its line's first. */
    public UpgradeNode artNodeOf(UpgradeNode node) {
        return node.tier().hasOwnArt() ? node : this.firstOf(node);
    }

    /** The node that starts {@code node}'s line: back along the prerequisite one tier step before, while there is one. */
    public UpgradeNode firstOf(UpgradeNode node) {
        UpgradeNode first = node;
        Optional<UpgradeNode> before = this.previousInLine(first);
        while (before.isPresent()) {
            first = before.get();
            before = this.previousInLine(first);
        }
        return first;
    }

    private Optional<UpgradeNode> previousInLine(UpgradeNode node) {
        Set<String> required = node.requires().requiredNodes();
        return node.tier().previous().flatMap(previous -> this.nodes.stream()
                .filter(candidate -> candidate.tier() == previous && required.contains(candidate.id())).findFirst());
    }

    public List<UpgradeNode> nodesIn(UpgradeSlot slot) {
        return this.nodes.stream().filter(n -> n.slot() == slot).toList();
    }

    /**
     * Unowned nodes whose prerequisites are met and whose choice still has a pick left, by slot
     * and then declaration order. A node that isn't offered is absent, not disabled.
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

    /**
     * The number key that buys {@code node}, the same for every tower: 1 the Core chain (Attune, Awaken, Transcendent),
     * 2 the Range line, 3 the head line that does not branch, 4 and 5 the two head lines that exclude each other
     * (and, past them, the two level-IV nodes that exclude each other), 6 to 8 the specials a tower can reach
     * together.
     */
    public int keyOf(UpgradeNode node) {
        return keyOf(node, this.nodes, this.choices);
    }

    /** 0 for a node outside the shape {@link #keyOf(UpgradeNode)} numbers, which the constructor rejects. */
    private static int keyOf(UpgradeNode node, List<UpgradeNode> nodes, List<ExclusiveChoice> choices) {
        return switch (node.slot()) {
            case CORE -> CHAIN_KEY;
            case BASE -> RANGE_KEY;
            case HEAD -> headKeyOf(node, nodes, choices);
            case SPECIAL -> specialKeyOf(node, nodes);
        };
    }

    private static int headKeyOf(UpgradeNode node, List<UpgradeNode> nodes, List<ExclusiveChoice> choices) {
        Optional<ExclusiveChoice> choice = choices.stream().filter(c -> c.contains(node.id())).findFirst();
        if (choice.isPresent()) {
            int index = choice.get().nodeIds().indexOf(node.id());
            return index < BRANCHES ? FIRST_BRANCH_KEY + index : 0;
        }
        // A line node follows its deepest head prerequisite: a node can also need the first node of another line.
        return headPrerequisites(node, nodes).stream()
                .max(Comparator.comparingInt(prerequisite -> headDepth(prerequisite, nodes)))
                .map(prerequisite -> headKeyOf(prerequisite, nodes, choices))
                .orElse(EXTRA_KEY);
    }

    private static int headDepth(UpgradeNode node, List<UpgradeNode> nodes) {
        return 1 + headPrerequisites(node, nodes).stream().mapToInt(prerequisite -> headDepth(prerequisite, nodes))
                .max().orElse(0);
    }

    private static List<UpgradeNode> headPrerequisites(UpgradeNode node, List<UpgradeNode> nodes) {
        return nodes.stream()
                .filter(other -> other.slot() == UpgradeSlot.HEAD && node.requires().requiredNodes().contains(other.id()))
                .toList();
    }

    /** Specials that open on the same nodes form one set, and number from 6 in the order they are declared. */
    private static int specialKeyOf(UpgradeNode node, List<UpgradeNode> nodes) {
        List<UpgradeNode> set = nodes.stream()
                .filter(other -> other.slot() == UpgradeSlot.SPECIAL
                        && other.requires().requiredNodes().equals(node.requires().requiredNodes()))
                .toList();
        int index = set.indexOf(node);
        return index < SPECIAL_KEYS ? FIRST_SPECIAL_KEY + index : 0;
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
