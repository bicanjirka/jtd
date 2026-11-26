package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.Tower;
import td.tower.TowerStat;
import td.tower.proc.Proc;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One tower type's upgrades as a stack of tiers that climbs from the bottom, its clusters and its fork.
 * {@link #offered(Tower, GameWorld)} is the query every caller shares.
 * <p>
 * A node needs its tier open and the nodes it comes after owned. A cluster's members exclude each other; what comes
 * after a member hides while another member is the active one. A tier above tier 0 that has a price or a bonus has a
 * node of its own ({@link #tierNodeOf(int)}) that carries its bonus and its perks. A paid tier's is its unlock
 * ({@link #unlockOf(int)}), bought like any other node, so a purchase, a refund and a save treat it as any other. A
 * free tier's is granted: the tower holds it once its XP opens the tier, and it is never bought, sold or saved.
 */
public final class UpgradeTree {

    private static final String TIER_PREFIX = "tier.";
    private static final UpgradeTree NONE = new UpgradeTree(List.of(UpgradeTier.open()), List.of(), List.of(),
            Optional.empty());

    private final List<UpgradeTier> tiers;
    private final List<UpgradeNode> nodes;
    private final List<UpgradeCluster> clusters;
    private final Optional<UpgradeCluster> fork;
    private final List<Optional<UpgradeNode>> tierNodes;
    private final Map<String, UpgradeNode> byId;
    private final Map<String, UpgradeCluster> clusterByMember;
    private final Map<String, String> branchMember;
    private final Map<String, Set<String>> mentionedBefore;

    /**
     * Rejects a bonus on tier 0, duplicate node ids, a node outside the tiers or before a node it comes after, a cluster member missing
     * or in two clusters, a cluster whose members differ in tier or price, and a node that comes
     * after itself through other nodes, on which every walk back along prerequisites would never end.
     */
    private UpgradeTree(List<UpgradeTier> tiers, List<UpgradeNode> nodes, List<UpgradeCluster> clusters,
                        Optional<UpgradeCluster> fork) {
        if (tiers.isEmpty() || tiers.getFirst().xp() != 0 || tiers.getFirst().isPaid()) {
            throw new IllegalArgumentException("Tier 0 is open from the start: " + tiers);
        }
        if (tiers.getFirst().gives()) {
            throw new IllegalArgumentException("Tier 0 is the tower's base and gives no bonus or perk: "
                    + tiers.getFirst());
        }
        this.tiers = List.copyOf(tiers);
        this.nodes = List.copyOf(nodes);
        this.clusters = List.copyOf(clusters);
        this.fork = fork;
        List<Optional<UpgradeNode>> own = new ArrayList<>();
        for (int tier = 0; tier < tiers.size(); tier++) {
            UpgradeTier gate = tiers.get(tier);
            boolean hasNode = tier > 0 && (gate.isPaid() || gate.gives());
            own.add(hasNode ? Optional.of(tierNode(tier, gate)) : Optional.empty());
        }
        this.tierNodes = List.copyOf(own);
        Map<String, UpgradeNode> ids = new LinkedHashMap<>();
        for (UpgradeNode node : this.nodes) {
            if (ids.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate upgrade node id: " + node.id());
            }
            if (node.tier() >= tiers.size()) {
                throw new IllegalArgumentException("Upgrade node " + node.id() + " sits above the top tier");
            }
        }
        for (Optional<UpgradeNode> tierNode : this.tierNodes) {
            tierNode.filter(node -> ids.containsKey(node.id())).ifPresent(node -> {
                throw new IllegalArgumentException("Upgrade node id taken by a tier's own node: " + node.id());
            });
        }
        for (int tier = 0; tier < tiers.size(); tier++) {
            this.unlockOf(tier).ifPresent(unlock -> ids.put(unlock.id(), unlock));
        }
        this.byId = Map.copyOf(ids);
        this.clusterByMember = clustersByMember(this.allClusters(), ids);
        Set<String> acyclic = new HashSet<>();
        for (UpgradeNode node : this.nodes) {
            rejectCycleFrom(node, ids, new LinkedHashSet<>(), acyclic);
            for (String before : node.requires().mentionedNodes()) {
                UpgradeNode required = ids.get(before);
                if (required != null && required.tier() > node.tier()) {
                    throw new IllegalArgumentException("Upgrade node " + node.id() + " comes after " + before
                            + ", a tier above it");
                }
            }
        }
        this.mentionedBefore = this.mentionedClosures();
        this.branchMember = this.branchMembers();
    }

    /** {@code gate}'s own node at {@code tier}, carrying its bonus and its perks. */
    private static UpgradeNode tierNode(int tier, UpgradeTier gate) {
        UpgradeNode node = UpgradeNode.of(TIER_PREFIX + tier, UpgradeTier.nameOf(tier), gate.unlockPrice()).inTier(tier)
                .withModifiers(gate.bonus());
        for (Proc perk : gate.perks()) {
            node = node.withProc(perk);
        }
        return node;
    }

    public static UpgradeTree none() {
        return NONE;
    }

    /** {@code nodes} on the standard tiers, with no cluster and no fork. */
    public static UpgradeTree of(UpgradeNode... nodes) {
        return of(List.of(nodes));
    }

    public static UpgradeTree of(List<UpgradeNode> nodes) {
        return new UpgradeTree(UpgradeTier.standard(), nodes, List.of(), Optional.empty());
    }

    /** This tree with {@code more} nodes declared after its own. */
    public UpgradeTree with(UpgradeNode... more) {
        List<UpgradeNode> next = new ArrayList<>(this.nodes);
        next.addAll(Arrays.asList(more));
        return new UpgradeTree(this.tiers, next, this.clusters, this.fork);
    }

    public UpgradeTree withCluster(UpgradeCluster cluster) {
        List<UpgradeCluster> next = new ArrayList<>(this.clusters);
        next.add(cluster);
        return new UpgradeTree(this.tiers, this.nodes, next, this.fork);
    }

    /**
     * This tree with {@code sides} as its fork: a cluster like any other, bought and switched as one, whose pick is the
     * tower's identity. The balance tools take each side as a build family, and a line after a side shows its art.
     */
    public UpgradeTree withFork(UpgradeCluster sides) {
        return new UpgradeTree(this.tiers, this.nodes, this.clusters, Optional.of(sides));
    }

    public UpgradeTree withTiers(List<UpgradeTier> next) {
        return new UpgradeTree(next, this.nodes, this.clusters, this.fork);
    }

    /** Every upgrade in declaration order, the fork's sides included, the tiers' own nodes not. */
    public List<UpgradeNode> nodes() {
        return this.nodes;
    }

    public List<UpgradeTier> tiers() {
        return this.tiers;
    }

    public List<UpgradeCluster> clusters() {
        return this.clusters;
    }

    public Optional<UpgradeCluster> fork() {
        return this.fork;
    }

    /**
     * {@code tier}'s own node, which carries its bonus: its unlock when the tier is paid, else granted with its XP.
     * Empty for tier 0, for a free tier with no bonus and outside the tiers.
     */
    public Optional<UpgradeNode> tierNodeOf(int tier) {
        return tier < 0 || tier >= this.tierNodes.size() ? Optional.empty() : this.tierNodes.get(tier);
    }

    /** The node that pays {@code tier} open; empty for a free tier and outside the tiers. */
    public Optional<UpgradeNode> unlockOf(int tier) {
        return this.tierNodeOf(tier).filter(_ -> this.tiers.get(tier).isPaid());
    }

    /** Whether {@code tier} needs no paying open, or {@code owned} paid it. */
    public boolean isPaidOpen(int tier, UpgradeState owned) {
        return this.unlockOf(tier).map(unlock -> owned.owns(unlock.id())).orElse(true);
    }

    /** Whether {@code node} is a tier's unlock rather than an upgrade. */
    public boolean isUnlock(UpgradeNode node) {
        return this.unlockOf(node.tier()).filter(node::equals).isPresent();
    }

    /** Whether {@code node} is a free tier's own node, which the tower holds by its XP and never buys. */
    public boolean isGranted(UpgradeNode node) {
        return this.tierNodeOf(node.tier()).filter(node::equals).isPresent() && !this.isUnlock(node);
    }

    /** Whether a tower with {@code xp} that owns {@code owned} holds {@code node}: owns it, or earned its tier. */
    public boolean isHeld(UpgradeNode node, int xp, UpgradeState owned) {
        return this.isGranted(node) ? this.tiersEarned(xp, owned) >= node.tier() : owned.owns(node.id());
    }

    /** The node with {@code id}, a tier's unlock included; empty for an id outside this tree. */
    public Optional<UpgradeNode> node(String id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    /** The cluster or fork {@code node} is a member of. */
    public Optional<UpgradeCluster> clusterOf(UpgradeNode node) {
        return Optional.ofNullable(this.clusterByMember.get(node.id()));
    }

    public boolean isForkSide(UpgradeNode node) {
        return this.fork.filter(sides -> sides.contains(node.id())).isPresent();
    }

    /** The member of an earlier cluster whose branch {@code node} belongs to: what it hides behind. */
    public Optional<UpgradeNode> branchOf(UpgradeNode node) {
        return Optional.ofNullable(this.branchMember.get(node.id())).map(this.byId::get);
    }

    /** Whether {@code node} comes after a cluster member {@code owned} has not picked, so the panel leaves it out. */
    public boolean isHidden(UpgradeNode node, UpgradeState owned) {
        String member = this.branchMember.get(node.id());
        return member != null && !owned.owns(member);
    }

    /** Whether switching picks and downgrading are allowed now: only between waves, never while one is out. */
    public static boolean changesAllowed(GameWorld context) {
        return !context.waves().clock().out();
    }

    /** Whether {@code tower} has the XP of every tier up to {@code tier}, or playtesting waives it. */
    public boolean xpMet(int tier, Tower tower, GameWorld context) {
        if (context.playtestRules().upgradeGatesIgnored()) {
            return true;
        }
        int xp = tower.experience().xp();
        for (int t = 1; t <= tier && t < this.tiers.size(); t++) {
            if (xp < this.tiers.get(t).xp()) {
                return false;
            }
        }
        return tier < this.tiers.size();
    }

    /** Whether {@code tier} and every tier below it are open for {@code tower}. */
    public boolean isOpen(int tier, Tower tower, GameWorld context) {
        return this.isOpen(tier, tower, tower.upgrades(), context);
    }

    // Allocation-free: offered() runs for every tower on every frame build.
    private boolean isOpen(int tier, Tower tower, UpgradeState owned, GameWorld context) {
        return tier >= 0 && this.xpMet(tier, tower, context) && this.unlocksHeld(tier, owned);
    }

    /** Whether {@code owned} holds the unlock of every paid tier up to {@code tier}. */
    private boolean unlocksHeld(int tier, UpgradeState owned) {
        for (int t = 1; t <= tier && t < this.tiers.size(); t++) {
            Optional<UpgradeNode> own = this.tierNodes.get(t);
            if (own.isPresent() && this.tiers.get(t).isPaid() && !owned.owns(own.get().id())) {
                return false;
            }
        }
        return true;
    }

    /** The highest tier open for {@code tower}: 0 at least. */
    public int highestOpenTier(Tower tower, GameWorld context) {
        int tier = 0;
        while (tier + 1 < this.tiers.size() && this.isOpen(tier + 1, tower, context)) {
            tier++;
        }
        return tier;
    }

    /**
     * How many tiers above tier 0 a tower with {@code xp} that owns {@code owned} has opened, counting up from tier I
     * while each has its XP and its unlock. Only earned XP counts, never playtesting's waiver: a tier's bonus is what
     * the tower earns.
     */
    public int tiersEarned(int xp, UpgradeState owned) {
        int earned = 0;
        while (earned + 1 < this.tiers.size() && xp >= this.tiers.get(earned + 1).xp()
                && this.unlocksHeld(earned + 1, owned)) {
            earned++;
        }
        return earned;
    }

    /**
     * Every node whose stats a tower with {@code xp} that owns {@code owned} holds: the own node of each tier it has
     * earned, then its other owned nodes in purchase order. A paid tier's unlock counts only once its XP is earned
     * too, so playtesting's waiver never grants a tier's bonus.
     */
    public List<UpgradeNode> heldNodes(int xp, UpgradeState owned) {
        List<UpgradeNode> held = new ArrayList<>();
        int earned = this.tiersEarned(xp, owned);
        for (int tier = 1; tier <= earned; tier++) {
            this.tierNodes.get(tier).ifPresent(held::add);
        }
        owned.owned().stream().filter(node -> !this.isUnlock(node)).forEach(held::add);
        return held;
    }

    /** What {@link #heldNodes} add to the tower's own stats. */
    public StatModifiers<TowerStat> heldModifiers(int xp, UpgradeState owned) {
        return this.heldNodes(xp, owned).stream().map(node -> node.effects().modifiers())
                .reduce(StatModifiers.none(), StatModifiers::plus);
    }

    /**
     * What {@code tower} may buy now, money aside: a paid tier's unlock once its XP is met, and every node whose tier is
     * open, whose prerequisites are owned and whose cluster is free, by tier and then declaration order.
     */
    public List<UpgradeNode> offered(Tower tower, GameWorld context) {
        List<UpgradeNode> offered = new ArrayList<>();
        for (int tier = 0; tier < this.tiers.size(); tier++) {
            Optional<UpgradeNode> unlock = this.unlockOf(tier);
            if (unlock.isPresent() && this.isOffered(unlock.get(), tower, context)) {
                offered.add(unlock.get());
            }
            for (int i = 0; i < this.nodes.size(); i++) {
                UpgradeNode node = this.nodes.get(i);
                if (node.tier() == tier && this.isOffered(node, tower, context)) {
                    offered.add(node);
                }
            }
        }
        return offered;
    }

    /** Whether {@code node} is one of {@link #offered}: the check a purchase makes. */
    public boolean offers(UpgradeNode node, Tower tower, GameWorld context) {
        return this.byId.get(node.id()) == node && this.isOffered(node, tower, context);
    }

    private boolean isOffered(UpgradeNode node, Tower tower, GameWorld context) {
        UpgradeState owned = tower.upgrades();
        if (owned.owns(node.id())) {
            return false;
        }
        if (this.isUnlock(node)) {
            return this.isOpen(node.tier() - 1, tower, owned, context) && this.xpMet(node.tier(), tower, context);
        }
        UpgradeCluster cluster = this.clusterByMember.get(node.id());
        return this.isOpen(node.tier(), tower, owned, context) && (cluster == null || !cluster.isTaken(owned))
                && !this.isHidden(node, owned) && node.requires().isSatisfied(tower, context);
    }

    /**
     * {@code target} preceded by every node a tower must own first, in the order to buy them: the unlocks of the paid
     * tiers up to its own, then the line before it, taking the cheaper side of an either-or.
     */
    public List<UpgradeNode> pathTo(UpgradeNode target) {
        Set<UpgradeNode> line = new LinkedHashSet<>();
        this.collectPath(target, line);
        int top = line.stream().mapToInt(UpgradeNode::tier).max().orElse(0);
        List<UpgradeNode> path = new ArrayList<>();
        for (int tier = 1; tier <= top; tier++) {
            this.unlockOf(tier).filter(unlock -> !line.contains(unlock)).ifPresent(path::add);
        }
        path.addAll(line);
        return List.copyOf(path);
    }

    /**
     * What buying {@code target} for {@code tower} takes now: every unowned node it needs first, the unlocks of the
     * tiers on the way included, judged in buy order as if the earlier ones were owned. It never makes a cluster's
     * pick off the target's own line.
     */
    public UpgradePath planPath(UpgradeNode target, Tower tower, GameWorld context) {
        return PathPlanner.plan(this, target, tower, context);
    }

    /**
     * What downgrading {@code node} takes from {@code tower}: replays the owned nodes in purchase order without it,
     * dropping each whose prerequisites, cluster or tier now fail.
     */
    public UpgradeRemoval removalFor(UpgradeNode node, Tower tower, GameWorld context) {
        UpgradeState owned = tower.upgrades();
        if (!owned.owns(node.id())) {
            return new UpgradeRemoval(node, List.of(), owned);
        }
        UpgradeState kept = UpgradeState.none();
        List<UpgradeNode> removed = new ArrayList<>();
        for (UpgradeNode next : owned.owned()) {
            if (!next.id().equals(node.id()) && this.holds(next, tower, kept, context)) {
                kept = kept.with(next);
            } else {
                removed.add(next);
            }
        }
        return new UpgradeRemoval(node, removed, kept);
    }

    /**
     * What switching {@code to}'s cluster or fork to it takes: the member {@code tower} owns now gives its place to
     * {@code to}, and every owned node that no longer holds without it is sold. Empty unless another member is owned.
     */
    public Optional<UpgradeSwitch> switchFor(UpgradeNode to, Tower tower, GameWorld context) {
        UpgradeState owned = tower.upgrades();
        UpgradeCluster cluster = this.clusterByMember.get(to.id());
        if (cluster == null || owned.owns(to.id()) || this.byId.get(to.id()) != to) {
            return Optional.empty();
        }
        Optional<UpgradeNode> from = owned.owned().stream().filter(node -> cluster.contains(node.id())).findFirst();
        if (from.isEmpty()) {
            return Optional.empty();
        }
        UpgradeState kept = UpgradeState.none();
        List<UpgradeNode> sold = new ArrayList<>();
        for (UpgradeNode next : owned.owned()) {
            if (next.equals(from.get())) {
                kept = kept.with(to);
            } else if (this.holds(next, tower, kept, context)) {
                kept = kept.with(next);
            } else {
                sold.add(next);
            }
        }
        return Optional.of(new UpgradeSwitch(from.get(), to, sold, kept));
    }

    /** Whether {@code node}, owned, still holds on top of {@code kept}; XP never drops, so it is not asked. */
    private boolean holds(UpgradeNode node, Tower tower, UpgradeState kept, GameWorld context) {
        if (this.isUnlock(node)) {
            return this.unlocksHeld(node.tier() - 1, kept);
        }
        UpgradeCluster cluster = this.clusterByMember.get(node.id());
        return this.unlocksHeld(node.tier(), kept) && node.requires().isSatisfied(tower, kept, context)
                && (cluster == null || !cluster.isTaken(kept));
    }

    /** {@code tier}'s singles: its nodes in no cluster, in declaration order. */
    public List<UpgradeNode> singlesIn(int tier) {
        return this.nodes.stream().filter(node -> node.tier() == tier && !this.clusterByMember.containsKey(node.id()))
                .toList();
    }

    /**
     * Whether no purchase can bring {@code node} to {@code owned} any more: it is unowned and another member of its
     * cluster is the active one, or a node it needs is locked out.
     */
    public boolean isLockedOut(UpgradeNode node, UpgradeState owned) {
        if (owned.owns(node.id())) {
            return false;
        }
        UpgradeCluster cluster = this.clusterByMember.get(node.id());
        if (cluster != null && cluster.isTaken(owned)) {
            return true;
        }
        return node.requires().lockedOut(id -> this.node(id).map(needed -> this.isLockedOut(needed, owned)).orElse(true));
    }

    /** The nodes {@code after} locks out that {@code before} did not, in declaration order. */
    public List<UpgradeNode> newlyLockedOut(UpgradeState before, UpgradeState after) {
        return this.nodes.stream()
                .filter(node -> this.isLockedOut(node, after) && !this.isLockedOut(node, before))
                .toList();
    }

    /** {@code owned}'s upgrades grouped by line, in the order each line's first node was bought; no unlock. */
    public List<UpgradeLine> lines(UpgradeState owned) {
        Map<UpgradeNode, List<UpgradeNode>> lines = new LinkedHashMap<>();
        for (UpgradeNode node : owned.owned()) {
            if (!this.isUnlock(node)) {
                lines.computeIfAbsent(this.firstOf(node), first -> new ArrayList<>()).add(node);
            }
        }
        return lines.entrySet().stream().map(line -> {
            UpgradeNode tip = line.getValue().stream()
                    .max((a, b) -> Integer.compare(this.levelOf(a), this.levelOf(b))).orElseThrow();
            return new UpgradeLine(line.getKey(), line.getValue(), this.levelOf(tip), this.artNodeOf(tip));
        }).toList();
    }

    /**
     * The node whose art {@code node} shows: a cluster member's, a node that asks for one, or a line's first node its own, a later step its line's
     * first, and a line that starts after a fork's side that side's.
     */
    public UpgradeNode artNodeOf(UpgradeNode node) {
        if (node.ownArt() || this.clusterByMember.containsKey(node.id())) {
            return node;
        }
        UpgradeNode first = this.firstOf(node);
        return this.nodes.stream().filter(this::isForkSide)
                .filter(side -> first.requires().requiredNodes().contains(side.id())).findFirst().orElse(first);
    }

    /** The node that starts {@code node}'s line: back along the node it comes after, while there is one. */
    public UpgradeNode firstOf(UpgradeNode node) {
        UpgradeNode first = node;
        Optional<UpgradeNode> before = this.previousInLine(first);
        while (before.isPresent()) {
            first = before.get();
            before = this.previousInLine(first);
        }
        return first;
    }

    /** How far along its line {@code node} is: 1 for a line's first node. */
    public int levelOf(UpgradeNode node) {
        int level = 1;
        Optional<UpgradeNode> before = this.previousInLine(node);
        while (before.isPresent()) {
            level++;
            before = this.previousInLine(before.get());
        }
        return level;
    }

    /**
     * The highest-tier node {@code node} comes after, the fork's sides aside: their column says it already. A node
     * that comes after one of several is after none of them in particular, so it is no line's step.
     */
    public Optional<UpgradeNode> previousInLine(UpgradeNode node) {
        if (!node.requires().mentionedNodes().equals(node.requires().requiredNodes())) {
            return Optional.empty();
        }
        UpgradeNode previous = null;
        for (UpgradeNode candidate : this.nodes) {
            if (node.requires().requiredNodes().contains(candidate.id()) && !this.isForkSide(candidate)
                    && (previous == null || candidate.tier() >= previous.tier())) {
                previous = candidate;
            }
        }
        return Optional.ofNullable(previous);
    }

    /** Every owned cluster member, in purchase order, with the members of its cluster it is not. */
    public List<UpgradeDecision> decisions(UpgradeState owned) {
        List<UpgradeDecision> decisions = new ArrayList<>();
        for (UpgradeNode node : owned.owned()) {
            this.clusterOf(node).ifPresent(cluster -> decisions.add(new UpgradeDecision(node, cluster.memberIds()
                    .stream().filter(id -> !id.equals(node.id())).map(this.byId::get).toList())));
        }
        return decisions;
    }

    private List<UpgradeCluster> allClusters() {
        List<UpgradeCluster> all = new ArrayList<>();
        this.fork.ifPresent(all::add);
        all.addAll(this.clusters);
        return all;
    }

    private static Map<String, UpgradeCluster> clustersByMember(List<UpgradeCluster> clusters,
                                                                Map<String, UpgradeNode> ids) {
        Map<String, UpgradeCluster> byMember = new HashMap<>();
        for (UpgradeCluster cluster : clusters) {
            UpgradeNode first = ids.get(cluster.memberIds().getFirst());
            for (String id : cluster.memberIds()) {
                UpgradeNode member = ids.get(id);
                if (member == null || byMember.putIfAbsent(id, cluster) != null) {
                    throw new IllegalArgumentException("Cluster member missing or in two clusters: " + id);
                }
                if (first == null || member.tier() != first.tier() || member.price() != first.price()) {
                    throw new IllegalArgumentException("A cluster's members share a tier and a price: " + cluster);
                }
            }
        }
        return Map.copyOf(byMember);
    }

    private static void rejectCycleFrom(UpgradeNode node, Map<String, UpgradeNode> byId, Set<String> walking,
                                        Set<String> acyclic) {
        if (acyclic.contains(node.id())) {
            return;
        }
        if (!walking.add(node.id())) {
            throw new IllegalArgumentException("Upgrade nodes require each other: " + String.join(" -> ", walking)
                    + " -> " + node.id());
        }
        node.requires().mentionedNodes().stream().map(byId::get).filter(Objects::nonNull)
                .forEach(required -> rejectCycleFrom(required, byId, walking, acyclic));
        walking.remove(node.id());
        acyclic.add(node.id());
    }

    private void collectPath(UpgradeNode node, Set<UpgradeNode> path) {
        for (String id : node.requires().requiredNodes().stream().sorted().toList()) {
            this.node(id).ifPresent(required -> this.collectPath(required, path));
        }
        path.add(node);
    }

    /** Each node's every node it may come after, on both sides of an either-or, walked back to the start. */
    private Map<String, Set<String>> mentionedClosures() {
        Map<String, Set<String>> closures = new HashMap<>();
        for (UpgradeNode node : this.nodes) {
            Set<String> closure = new HashSet<>();
            List<String> walk = new ArrayList<>(node.requires().mentionedNodes());
            while (!walk.isEmpty()) {
                String id = walk.removeLast();
                UpgradeNode before = this.byId.get(id);
                if (closure.add(id) && before != null) {
                    walk.addAll(before.requires().mentionedNodes());
                }
            }
            closures.put(node.id(), Set.copyOf(closure));
        }
        return Map.copyOf(closures);
    }

    /** For each node behind exactly one member of a cluster, the member of the lowest such cluster (the fork first). */
    private Map<String, String> branchMembers() {
        List<UpgradeCluster> byTier = this.allClusters().stream()
                .sorted((a, b) -> Integer.compare(this.tierOf(a), this.tierOf(b))).toList();
        Map<String, String> members = new HashMap<>();
        for (UpgradeNode node : this.nodes) {
            for (UpgradeCluster cluster : byTier) {
                Optional<String> member = this.memberBehind(node, cluster);
                if (member.isPresent()) {
                    members.put(node.id(), member.get());
                    break;
                }
            }
        }
        return Map.copyOf(members);
    }

    /** The one member of {@code cluster} {@code node} comes after; empty for a member, or for none or several. */
    private Optional<String> memberBehind(UpgradeNode node, UpgradeCluster cluster) {
        if (cluster.contains(node.id())) {
            return Optional.empty();
        }
        Set<String> before = this.mentionedBefore.get(node.id());
        List<String> behind = cluster.memberIds().stream().filter(before::contains).toList();
        return behind.size() == 1 ? Optional.of(behind.getFirst()) : Optional.empty();
    }

    private int tierOf(UpgradeCluster cluster) {
        return this.byId.get(cluster.memberIds().getFirst()).tier();
    }
}
