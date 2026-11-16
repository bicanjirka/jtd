package td.balance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.wave.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A player that tries before it buys, and buys only what a round needs. Before each round it plays the level again
 * as far as that round with what it has; while that leaks it tries every purchase it can afford, each a replay of
 * its own play with that one purchase added, and makes the cheapest that holds the round, or with none the one that
 * takes the most off the leak for its price. What it does not need it keeps for a later round.
 * <p>
 * It knows nothing of prices' worth or of what a tower is for: a purchase is judged by what it does in the real
 * engine on this board against this round. So it is the same player for a new tower, tier or level, and what it may
 * buy is all that sets one such player apart from another.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class SearchingPlayer implements Player {

    /**
     * What a player may buy and how far it looks.
     *
     * @param types    the towers it may build
     * @param upgrades whether it may buy upgrade nodes
     * @param ahead    rounds after the coming one a purchase is also held to
     */
    record Limits(Set<TowerFactory.Type> types, boolean upgrades, int ahead) {

        Limits {
            types = Set.copyOf(types);
        }
    }

    /** One fewer life lost is worth as much as taking all the health of an enemy that leaked. */
    private static final double PER_LEAK = 1.0;
    /** Damage counts for little beside the leak itself: it only tells apart purchases the leak does not. */
    private static final double PER_DAMAGE = 0.05;
    /** Sprees tried at once when no purchase helps, how often in a round, and how long one may go on. */
    private static final int SPREES = 60;
    private static final int MOST_SPREES = 4;
    private static final int MOST_SPREE_STEPS = 200;
    /** The shares of the credits the sprees spend: a third, two thirds and all. */
    private static final int SPREE_SHARES = 3;
    /** A purchase that gains less than this does nothing. */
    private static final double LEAST_GAIN = 0.0001;

    private static final Map<TowerFactory.Type, UpgradeTree> TREES = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(SearchingPlayer.class);

    private final Limits limits;
    private final Policy policy;
    private int roundDone;

    SearchingPlayer(Limits limits, Policy policy) {
        this.limits = limits;
        this.policy = policy;
    }

    static PlayerSpec spec(String name, Limits limits, Policy policy) {
        return new PlayerSpec(name, (survey, seed) -> new SearchingPlayer(limits, policy));
    }

    /** A purchase it could make now, one action or a node with the nodes before it, and what it costs. */
    record Option(List<PlayerAction> actions, int price) {

        Option {
            actions = List.copyOf(actions);
        }

        static Option of(PlayerAction action, int price) {
            return new Option(List.of(action), price);
        }

        String toLine() {
            return this.actions.stream().map(PlayerAction::toLine).collect(Collectors.joining(" + "));
        }
    }

    /**
     * How the rounds rehearsed went.
     *
     * @param leaks lives they took
     * @param left  the health the enemies that leaked got away with, each as a share of its own: lower is a defence
     *              nearer to holding them
     * @param dealt the damage the towers landed: more is a defence that does more, whomever it hit
     */
    record Outcome(int leaks, double left, double dealt) {

        /** How much better {@code other} is; above zero is better. */
        double gainOf(Outcome other) {
            double more = this.dealt <= 0 ? 0 : (other.dealt - this.dealt) / this.dealt;
            return (this.leaks - other.leaks) * PER_LEAK + (this.left - other.left) + more * PER_DAMAGE;
        }
    }

    /** Which purchase is made when none holds the round alone. */
    enum Policy {
        /** The one that gains the most for a credit. */
        BEST_RATE,
        /** The one that gains the most, whatever it costs. */
        BIGGEST_STEP
    }

    @Override
    public void act(Turn turn) {
        if (!turn.betweenRounds() || turn.round() == this.roundDone) {
            return;
        }
        this.roundDone = turn.round();
        Outcome now = this.rehearse(turn, List.of());
        int sprees = 0;
        while (now.leaks() > 0) {
            List<Option> options = this.options(turn);
            List<Outcome> outcomes = this.rehearseAll(turn, options);
            int best = pick(now, options, outcomes, this.policy);
            if (best < 0) {
                // What no one purchase helps against may need a node that others stand before.
                options = this.deeperOptions(turn);
                outcomes = this.rehearseAll(turn, options);
                best = pick(now, options, outcomes, this.policy);
            }
            if (best < 0 && sprees++ < MOST_SPREES) {
                // Or many purchases at once: a boss that only breaks under a whole defence shows no one step to take.
                List<Spree> tried = IntStream.range(0, SPREES).parallel().mapToObj(i -> this.spree(turn, i)).toList();
                List<Option> bought = tried.stream().map(Spree::bought).toList();
                options = bought;
                outcomes = tried.stream().map(Spree::outcome).toList();
                best = pick(now, options, outcomes, Policy.BIGGEST_STEP);
            }
            if (best < 0 || !options.get(best).actions().stream().allMatch(turn::perform)) {
                LOG.info("round {}: {} leaks left with {} credits, {} purchases tried, {}", turn.round(), now.leaks(),
                        turn.credits(), options.size(), best < 0 ? "none helps" : "the best did not go through");
                turn.wants(options.stream().mapToInt(Option::price).min().orElse(0));
                return;
            }
            LOG.info("round {}: {} for {} takes {} leaks to {}", turn.round(), options.get(best).toLine(),
                    options.get(best).price(), now.leaks(), outcomes.get(best).leaks());
            now = outcomes.get(best);
        }
    }

    /** What a spending spree bought and how the rounds rehearsed went with it. */
    private record Spree(Option bought, Outcome outcome) {
    }

    /**
     * Spree {@code index} of the coming round: the play so far played again with purchases drawn by lot, up to a
     * share of the credits that grows with the index's place among the sprees.
     */
    private Spree spree(Turn turn, int index) {
        int round = turn.round();
        List<PlayRecord.TimedAction> soFar = turn.actionsSoFar();
        ScriptedPlayer script = new ScriptedPlayer(soFar);
        double share = (index % SPREE_SHARES + 1) / (double) SPREE_SHARES;
        RandomSource lot = RandomSource.seeded(round * 1000L + index);
        int before = turn.credits();
        int[] spent = new int[1];
        boolean[] done = new boolean[1];
        Player player = now -> {
            script.act(now);
            if (now.round() != round || !now.betweenRounds() || done[0]) {
                return;
            }
            done[0] = true;
            int floor = (int) Math.round(now.credits() * (1 - share));
            for (int i = 0; i < MOST_SPREE_STEPS; i++) {
                int budget = now.credits() - floor;
                List<Option> options = new ArrayList<>(this.options(now));
                options.addAll(this.deeperOptions(now));
                options.removeIf(option -> option.price() > budget);
                if (options.isEmpty()) {
                    break;
                }
                options.get(lot.nextIndex(options.size())).actions().forEach(now::perform);
            }
            spent[0] = before - now.credits();
        };
        PlayRecord played = turn.rehearse(player, Math.min(turn.roundCount(), round + this.limits.ahead()));
        List<PlayerAction> actions = played.actions().stream().skip(soFar.size())
                .map(PlayRecord.TimedAction::action).toList();
        return new Spree(new Option(actions, spent[0]), outcomeOf(played, round));
    }

    /**
     * The index of the purchase to make: the cheapest that leaves no leak, else the one {@code policy} takes of
     * those that gain on {@code now}; -1 when none helps.
     */
    static int pick(Outcome now, List<Option> options, List<Outcome> outcomes, Policy policy) {
        int best = -1;
        for (int i = 0; i < options.size(); i++) {
            if (outcomes.get(i).leaks() == 0 && (best < 0 || options.get(i).price() < options.get(best).price()
                    || (options.get(i).price() == options.get(best).price()
                    && outcomes.get(i).dealt() > outcomes.get(best).dealt()))) {
                best = i;
            }
        }
        if (best >= 0) {
            return best;
        }
        double bestRate = 0;
        for (int i = 0; i < options.size(); i++) {
            double gain = now.gainOf(outcomes.get(i));
            double rate = policy == Policy.BEST_RATE ? gain / Math.max(1, options.get(i).price()) : gain;
            if (gain > LEAST_GAIN && rate > bestRate) {
                bestRate = rate;
                best = i;
            }
        }
        return best;
    }

    private List<Outcome> rehearseAll(Turn turn, List<Option> options) {
        return options.parallelStream().map(option -> this.rehearse(turn, option.actions())).toList();
    }

    /** The play so far played again with {@code extra} bought before the coming round, as far as it looks. */
    private Outcome rehearse(Turn turn, List<PlayerAction> extra) {
        int round = turn.round();
        List<PlayRecord.TimedAction> script = new ArrayList<>(turn.actionsSoFar());
        extra.forEach(action -> script.add(new PlayRecord.TimedAction(round, PlayRecord.TimedAction.BEFORE_ROUND,
                action)));
        int through = Math.min(turn.roundCount(), round + this.limits.ahead());
        return outcomeOf(turn.rehearse(new ScriptedPlayer(script), through), round);
    }

    private static Outcome outcomeOf(PlayRecord played, int round) {
        int leaks = played.rounds().stream().filter(one -> one.number() >= round)
                .mapToInt(one -> one.pressure().leaked()).sum();
        double left = played.ends().stream().filter(end -> end.leaked() && end.round() >= round)
                .mapToDouble(end -> end.spot().healthLeft()).sum();
        double dealt = played.rounds().stream().filter(one -> one.number() >= round)
                .mapToLong(one -> one.pressure().damage().values().stream().mapToLong(Long::longValue).sum()).sum();
        return new Outcome(leaks, left, dealt);
    }

    /** Every purchase it may make and can afford: each tower on its best cells, and each node a tower is offered. */
    private List<Option> options(Turn turn) {
        List<Option> options = new ArrayList<>();
        int credits = turn.credits();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            int price = turn.priceOf(type);
            if (this.limits.types().contains(type) && price <= credits) {
                bestCells(turn, type).forEach(cell -> options.add(Option.of(new PlayerAction.Build(type, cell), price)));
            }
        }
        if (this.limits.upgrades()) {
            // Towers alike in type, lane and nodes are one choice: the node goes to the one that has done the most.
            Map<String, Option> byKind = new LinkedHashMap<>();
            Map<String, Long> busiest = new LinkedHashMap<>();
            for (Tower tower : turn.towers()) {
                Point cell = turn.cellOf(tower);
                String kind = tower.getType() + "@" + laneOf(turn.survey(), tower.getType(), cell) + " "
                        + tower.upgrades().owned().stream().map(UpgradeNode::id).sorted().collect(Collectors.joining(","));
                for (UpgradeNode node : turn.buyable(tower)) {
                    String key = kind + " + " + node.id();
                    if (node.price() <= credits && turn.gateMet(tower, node)
                            && tower.getDamageDealt() >= busiest.getOrDefault(key, -1L)) {
                        busiest.put(key, tower.getDamageDealt());
                        byKind.put(key, Option.of(new PlayerAction.Upgrade(cell, node.id()), node.price()));
                    }
                }
            }
            options.addAll(byKind.values());
        }
        return options;
    }

    /**
     * Purchases of more than one step: for each tower standing, each node it is not yet offered with the nodes
     * before it, and each new tower with each node of its first tier.
     */
    private List<Option> deeperOptions(Turn turn) {
        List<Option> options = new ArrayList<>();
        if (!this.limits.upgrades()) {
            return options;
        }
        int credits = turn.credits();
        Map<String, Option> byKind = new LinkedHashMap<>();
        for (Tower tower : turn.towers()) {
            Point cell = turn.cellOf(tower);
            List<UpgradeNode> owned = tower.upgrades().owned();
            String kind = tower.getType() + "@" + laneOf(turn.survey(), tower.getType(), cell) + " "
                    + owned.stream().map(UpgradeNode::id).sorted().collect(Collectors.joining(","));
            List<UpgradeNode> offered = turn.buyable(tower);
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                List<UpgradeNode> path = tower.upgradeTree().pathTo(node).stream()
                        .filter(step -> !owned.contains(step)).toList();
                int price = path.stream().mapToInt(UpgradeNode::price).sum();
                if (path.size() > 1 && !owned.contains(node) && !offered.contains(node) && price <= credits
                        && path.stream().allMatch(step -> turn.gateMet(tower, step))) {
                    byKind.putIfAbsent(kind + " + " + node.id(), new Option(path.stream()
                            .<PlayerAction>map(step -> new PlayerAction.Upgrade(cell, step.id())).toList(), price));
                }
            }
        }
        options.addAll(byKind.values());
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            if (!this.limits.types().contains(type)) {
                continue;
            }
            Optional<Point> cell = bestCells(turn, type).stream().findFirst();
            for (UpgradeNode node : TREES.computeIfAbsent(type, TowerBench::treeOf).nodes()) {
                int price = turn.priceOf(type) + node.price();
                if (cell.isPresent() && node.tier() == 0 && price <= credits
                        && TREES.get(type).pathTo(node).size() == 1) {
                    options.add(new Option(List.of(new PlayerAction.Build(type, cell.get()),
                            new PlayerAction.Upgrade(cell.get(), node.id())), price));
                }
            }
        }
        return options;
    }

    /** The free cells {@code type} reaches the most of each lane from, and the most of all lanes together. */
    static List<Point> bestCells(Turn turn, TowerFactory.Type type) {
        BoardSurvey survey = turn.survey();
        List<Point> free = survey.buildable().stream().filter(turn::isFree).toList();
        Set<Point> cells = new LinkedHashSet<>();
        free.stream().max(Comparator.comparingDouble((Point cell) -> survey.coverage(type, cell))).ifPresent(cells::add);
        for (int lane = 0; lane < survey.pathCount(); lane++) {
            int index = lane;
            free.stream().max(Comparator.comparingDouble(
                    (Point cell) -> survey.pathWithin(cell, survey.reach(type), index))).ifPresent(cells::add);
        }
        return List.copyOf(cells);
    }

    /** The lane {@code type} reaches the most of from {@code cell}. */
    private static int laneOf(BoardSurvey survey, TowerFactory.Type type, Point cell) {
        int best = 0;
        for (int lane = 1; lane < survey.pathCount(); lane++) {
            if (survey.pathWithin(cell, survey.reach(type), lane) > survey.pathWithin(cell, survey.reach(type), best)) {
                best = lane;
            }
        }
        return best;
    }
}
