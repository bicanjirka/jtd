package td.balance;

import td.damage.DamageUnits;
import td.enemy.EnemyDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.ThreadConfined;
import td.wave.Point;
import td.wave.Vec2;
import td.wave.Wave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A player who knows what the towers do and reads the wave preview. For every enemy of the next rounds it
 * estimates how much damage the army lands on it along its path, from what the {@link TowerLab} measured for each
 * tower on its cell against that kind of enemy, and how likely that is to kill it. A purchase is worth the enemy
 * health it would turn from likely leaking to likely dying, so it spends where enemies are getting through and
 * not where they already die twice over; once the coming rounds are safe, it builds for the ones after. It buys
 * the most worth per credit, a tower on a good free cell or an upgrade line on a tower it owns, saves only for
 * something much better that is near, and never sells.
 * <p>
 * It counts what a tower does for the others the way the lab measured it: the Beacon's buff on the towers in its
 * reach, and the share more a damage tower lands beside a tower that slows, marks or weakens. It knows an enemy
 * that hides is only hit once something reveals it: on a path a revealer reaches, it counts its towers' damage on
 * a hiding kind between what they land on it hidden and revealed, by how much the revealer shows. It reads the
 * lab's numbers, so what the lab cannot see it cannot either. The gap to a searching player measures that.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class CompetentPlayer implements Player {

    /** Cells tried per tower type when looking for where to build. */
    private static final int CELLS_PER_TYPE = 6;
    /** How much each round counts against the one before it: it reads every round left, the nearest most. */
    private static final double LOOK_AHEAD = 0.5;
    /**
     * The most it believes one node multiplies a tower by. Against probes that never die a node can measure far
     * past what it does to enemies that do; the report shows those outliers, the player does not chase them.
     * Swept against the strong player's margins, this cap held the most on every built-in level.
     */
    private static final double NODE_FACTOR_CAP = 1.5;
    /** It wants this much more damage on an enemy than its health before it calls the enemy dead. */
    private static final double SAFETY = 1.3;
    /**
     * How soft the line between leaking and dying is, as a share of the enemy's health. A hard line gives up on
     * an enemy it cannot surely kill yet and spends on easy ones instead; this one held the most enemy health on
     * every built-in level.
     */
    private static final double SOFTNESS = 0.4;
    /**
     * How much of the help the lab measured for a neighbour it counts. The lab measures it on probes that never
     * die, where a slow keeps every one of them in reach longer; swept against the strong player, half held as
     * much as ignoring it, and all of it less.
     */
    private static final double SUPPORT_TRUST = 0.5;

    private final TowerLab lab;
    private final Style style;
    private final int probesPerPath;
    /** The lab's factor for each tower type against each of the level's kinds at rank, read once. */
    private final double[][] kindFactors;
    /** The same with every enemy kept revealed: different only for a kind that hides. */
    private final double[][] revealedFactors;

    private int creditsLastActed = -1;
    private int roundLastActed = -1;
    private int towersLastActed = -1;
    /** For each path and kind, how much of that kind the revealers reaching the path show: 0 to 1. */
    private double[][] seen = new double[0][0];

    CompetentPlayer(TowerLab lab, Style style) {
        this.lab = lab;
        this.style = style;
        this.probesPerPath = TowerLab.probesPerPath();
        List<String> kinds = lab.levelRanks();
        this.kindFactors = new double[TowerFactory.Type.values().length][kinds.size()];
        this.revealedFactors = new double[TowerFactory.Type.values().length][kinds.size()];
        for (TowerFactory.Type type : TowerLab.damageDealers()) {
            for (int k = 0; k < kinds.size(); k++) {
                this.kindFactors[type.ordinal()][k] = lab.kindFactor(type, kinds.get(k));
                this.revealedFactors[type.ordinal()][k] = lab.revealedFactor(type, kinds.get(k));
            }
        }
    }

    /**
     * How a competent player is held back or helped, for the questions that compare players.
     *
     * @param types       the towers it may build
     * @param bannedNodes upgrade nodes it never buys, nor any line through them
     * @param upgrades    whether it buys upgrades
     * @param smartCells  whether it builds on the best cell, or on any cell that reaches a path
     * @param saveFactor  it waits while something this many times better per credit is within reach; 0 never waits
     */
    record Style(Set<TowerFactory.Type> types, Set<String> bannedNodes, boolean upgrades, boolean smartCells,
                 double saveFactor) {

        Style {
            // An enum set, not Set.copyOf: options are weighed in type order, and a play must repeat.
            types = types.isEmpty() ? EnumSet.noneOf(TowerFactory.Type.class) : EnumSet.copyOf(types);
            bannedNodes = Set.copyOf(bannedNodes);
        }

        static Style standard() {
            return new Style(EnumSet.allOf(TowerFactory.Type.class), Set.of(), true, true, 2.0);
        }

        /** The standard player denied {@code role}: its towers, and the nodes that do it on the others. */
        static Style without(Role role) {
            return standard().withTypes(role.allowedTypes()).withBannedNodes(role.bannedNodes());
        }

        Style withTypes(Set<TowerFactory.Type> allowed) {
            return new Style(allowed, this.bannedNodes, this.upgrades, this.smartCells, this.saveFactor);
        }

        Style withBannedNodes(Set<String> banned) {
            return new Style(this.types, banned, this.upgrades, this.smartCells, this.saveFactor);
        }

        Style withoutUpgrades() {
            return new Style(this.types, this.bannedNodes, false, this.smartCells, this.saveFactor);
        }

        Style withRandomCells() {
            return new Style(this.types, this.bannedNodes, this.upgrades, false, this.saveFactor);
        }

        Style withSaveFactor(double factor) {
            return new Style(this.types, this.bannedNodes, this.upgrades, this.smartCells, factor);
        }
    }

    static PlayerSpec spec(String name, Map<String, TowerLab> labs, Style style) {
        return new PlayerSpec(name, (survey, seed) -> new CompetentPlayer(labs.get(survey.level().name()), style));
    }

    /** One thing it could buy, what it adds to the army's damage, and how to buy it. */
    private record Option(int cost, Damage added, Runnable buy) {
    }

    /**
     * One enemy of the coming rounds as the preview shows it, and how much it counts.
     *
     * @param kind which of the level's kind and rank pairs it is, as an index into {@link TowerLab#levelRanks()}
     */
    private record Foe(int path, int kind, double health, double weight) {
    }

    /** What the army lands on one enemy walking each path, per kind, in health points. */
    private record Damage(double[][] byPathAndKind) {

        static Damage none(int paths, int kinds) {
            return new Damage(new double[paths][kinds]);
        }

        Damage plus(Damage other) {
            double[][] sum = new double[this.byPathAndKind.length][];
            for (int p = 0; p < sum.length; p++) {
                sum[p] = this.byPathAndKind[p].clone();
                for (int k = 0; k < sum[p].length; k++) {
                    sum[p][k] += other.byPathAndKind[p][k];
                }
            }
            return new Damage(sum);
        }

        Damage times(double factor) {
            double[][] scaled = new double[this.byPathAndKind.length][];
            for (int p = 0; p < scaled.length; p++) {
                scaled[p] = this.byPathAndKind[p].clone();
                for (int k = 0; k < scaled[p].length; k++) {
                    scaled[p][k] *= factor;
                }
            }
            return new Damage(scaled);
        }

        double on(int path, int kind) {
            return this.byPathAndKind[path][kind];
        }
    }

    @Override
    public void act(Turn turn) {
        // With the same credits, round and towers nothing it could decide has changed.
        if (turn.credits() == this.creditsLastActed && turn.round() == this.roundLastActed
                && turn.towers().size() == this.towersLastActed) {
            return;
        }
        this.decide(turn);
        this.creditsLastActed = turn.credits();
        this.roundLastActed = turn.round();
        this.towersLastActed = turn.towers().size();
    }

    private void decide(Turn turn) {
        List<String> kinds = this.lab.levelRanks();
        List<Foe> foes = this.foes(turn, kinds);
        if (foes.isEmpty()) {
            return;
        }
        for (int guard = 0; guard < 50; guard++) {
            this.seen = this.seen(turn, kinds, Optional.empty(), List.of());
            Damage army = this.armyDamage(turn, kinds);
            double standing = this.killed(foes, army);
            Map<Option, Double> worths = new LinkedHashMap<>();
            this.options(turn, kinds).forEach(option -> worths.put(option, this.worth(option, foes, army, standing)));
            Comparator<Option> byWorth = Comparator.comparingDouble(worths::get);
            int credits = turn.credits();
            Optional<Option> affordable = worths.keySet().stream().filter(option -> option.cost() <= credits)
                    .max(byWorth);
            turn.wants(worths.keySet().stream().filter(option -> option.cost() > credits && worths.get(option) > 0)
                    .max(byWorth).map(Option::cost).orElse(0));
            if (affordable.isEmpty()) {
                return;
            }
            double best = worths.get(affordable.get());
            if (this.style.saveFactor() > 0) {
                double reach = 2.0 * credits + turn.survey().level().startingCredits();
                boolean worthWaiting = worths.entrySet().stream()
                        .filter(entry -> entry.getKey().cost() > credits && entry.getKey().cost() <= reach)
                        .anyMatch(entry -> entry.getValue() >= this.style.saveFactor() * best);
                if (worthWaiting) {
                    return;
                }
            }
            int before = turn.credits();
            affordable.get().buy().run();
            if (turn.credits() == before) {
                return;
            }
        }
    }

    /** Enemy health per credit the option turns from leaking to dying. */
    private double worth(Option option, List<Foe> foes, Damage army, double standing) {
        return option.cost() <= 0 ? 0 : (this.killed(foes, army.plus(option.added())) - standing) / option.cost();
    }

    /** The enemy health of the coming rounds the army is expected to kill, each round counting less the later. */
    private double killed(List<Foe> foes, Damage army) {
        double total = 0;
        for (Foe foe : foes) {
            double ratio = army.on(foe.path(), foe.kind()) / foe.health();
            total += foe.weight() * foe.health() / (1 + Math.exp(-(ratio - SAFETY) / SOFTNESS));
        }
        return total;
    }

    private List<Foe> foes(Turn turn, List<String> kinds) {
        List<Foe> foes = new ArrayList<>();
        for (int ahead = 0; turn.round() + ahead <= turn.roundCount(); ahead++) {
            double weight = Math.pow(LOOK_AHEAD, ahead);
            for (Wave wave : turn.wavesOf(turn.round() + ahead)) {
                for (EnemyDefinition definition : wave.enemySet()) {
                    int kind = kinds.indexOf(TowerLab.kindKey(definition.id(), wave.rankFor(definition)));
                    if (kind < 0) {
                        continue;
                    }
                    double health = definition.baseHealth() / definition.healthDivisor() * turn.enemyHealthScale();
                    foes.add(new Foe(wave.getPathIndex(), kind, Math.max(1, health),
                            weight * wave.enemyCount(definition)));
                }
            }
        }
        return foes;
    }

    private Damage armyDamage(Turn turn, List<String> kinds) {
        Damage total = Damage.none(turn.survey().pathCount(), kinds.size());
        for (Tower tower : turn.towers()) {
            total = total.plus(this.towerDamage(turn, tower, kinds));
        }
        return total;
    }

    /**
     * What a standing tower lands: its cell, its upgrades, and the Beacons and support towers beside it; a Beacon
     * its buff's share.
     */
    private Damage towerDamage(Turn turn, Tower tower, List<String> kinds) {
        Point cell = turn.cellOf(tower);
        if (tower.getType() == TowerFactory.Type.AURA) {
            return this.beaconDamage(turn, cell, kinds, this.upgradeFactor(tower, tower.upgrades()));
        }
        return this.plainDamage(turn, tower.getType(), cell, kinds)
                .times(this.upgradeFactor(tower, tower.upgrades())
                        * (1 + this.beaconBoostAt(turn, tower.getType(), cell) + this.supportBoostAt(turn, cell)));
    }

    /** What a bare {@code type} on {@code cell} lands on one enemy of each kind walking each path. */
    private Damage plainDamage(Turn turn, TowerFactory.Type type, Point cell, List<String> kinds) {
        int paths = turn.survey().pathCount();
        double[][] damage = new double[paths][kinds.size()];
        for (int p = 0; p < paths; p++) {
            double perEnemy = this.lab.cellValue(type, cell, p) / this.probesPerPath / DamageUnits.PER_POINT;
            for (int k = 0; k < kinds.size(); k++) {
                double hidden = this.kindFactors[type.ordinal()][k];
                double shown = p < this.seen.length ? this.seen[p][k] : 0;
                damage[p][k] = perEnemy * (hidden + shown * (this.revealedFactors[type.ordinal()][k] - hidden));
            }
        }
        return new Damage(damage);
    }

    private List<Option> options(Turn turn, List<String> kinds) {
        List<Option> options = new ArrayList<>();
        for (TowerFactory.Type type : this.style.types()) {
            int price = turn.priceOf(type);
            for (Point cell : this.candidateCells(turn, type, kinds)) {
                Damage added = type == TowerFactory.Type.AURA ? this.beaconDamage(turn, cell, kinds, 1.0)
                        : this.plainDamage(turn, type, cell, kinds)
                                .times(1 + this.beaconBoostAt(turn, type, cell) + this.supportBoostAt(turn, cell))
                                .plus(this.supportDamage(turn, type, cell, turn.survey().reach(type),
                                        new Gain(0, this.supportGain(type, List.of())), kinds));
                options.add(new Option(price, added, () -> turn.build(type, cell)));
            }
        }
        if (this.style.upgrades()) {
            for (Tower tower : turn.towers()) {
                options.addAll(this.upgradeOptions(turn, tower, kinds));
            }
        }
        return options;
    }

    private List<Point> candidateCells(Turn turn, TowerFactory.Type type, List<String> kinds) {
        List<Point> free = turn.survey().buildable().stream().filter(turn::isFree)
                .filter(cell -> type == TowerFactory.Type.AURA || this.lab.cellValue(type, cell) > 0)
                .toList();
        if (free.isEmpty()) {
            return List.of();
        }
        if (!this.style.smartCells()) {
            return List.of(free.get(turn.random().nextIndex(free.size())));
        }
        if (type == TowerFactory.Type.AURA) {
            return free.stream().sorted(Comparator.comparingDouble(
                    (Point cell) -> total(this.beaconDamage(turn, cell, kinds, 1.0))).reversed())
                    .limit(CELLS_PER_TYPE).toList();
        }
        // The best cells of each path, so a lane the next round crowds is never left without a candidate.
        List<Point> cells = new ArrayList<>();
        for (int p = 0; p < turn.survey().pathCount(); p++) {
            int path = p;
            free.stream().sorted(Comparator.comparingDouble((Point cell) -> this.lab.cellValue(type, cell, path))
                            .reversed())
                    .limit(CELLS_PER_TYPE).filter(cell -> !cells.contains(cell)).forEach(cells::add);
        }
        return cells;
    }

    private static double total(Damage damage) {
        double sum = 0;
        for (double[] byKind : damage.byPathAndKind()) {
            for (double value : byKind) {
                sum += value;
            }
        }
        return sum;
    }

    private double upgradeFactor(Tower tower, UpgradeState owned) {
        return owned.owned().stream().mapToDouble(node -> this.nodeFactor(tower.getType(), node.id()))
                .reduce(1.0, (a, b) -> a * b);
    }

    private double nodeFactor(TowerFactory.Type type, String nodeId) {
        return Math.min(NODE_FACTOR_CAP, this.lab.nodeFactor(type, nodeId));
    }

    /** The share more a {@code type} on {@code cell} lands from the Beacons standing over it. */
    private double beaconBoostAt(Turn turn, TowerFactory.Type type, Point cell) {
        Vec2 centre = turn.survey().centre(cell);
        return turn.towers().stream()
                .filter(tower -> tower.getType() == TowerFactory.Type.AURA)
                .filter(beacon -> Math.hypot(beacon.getX() - centre.x(), beacon.getY() - centre.y())
                        <= beacon.getRangeReal())
                .mapToDouble(beacon -> this.lab.beaconGain(type) * this.upgradeFactor(beacon, beacon.upgrades()))
                .sum();
    }

    /**
     * The share more a damage tower on {@code cell} lands from the other towers whose reach covers it and that
     * slow, mark or weaken what they hit. Two of a type put the same effect on the same enemies, so only the
     * strongest of each type counts; different types add up.
     */
    private double supportBoostAt(Turn turn, Point cell) {
        return TowerLab.damageDealers().stream()
                .mapToDouble(type -> this.strongestSupport(turn, type, cell, cell))
                .sum();
    }

    /** The most a {@code type} whose reach covers {@code cell} gives it, leaving out the tower on {@code skip}. */
    private double strongestSupport(Turn turn, TowerFactory.Type type, Point cell, Point skip) {
        Vec2 centre = turn.survey().centre(cell);
        return turn.towers().stream()
                .filter(other -> other.getType() == type && !turn.cellOf(other).equals(skip))
                .filter(other -> Math.hypot(other.getX() - centre.x(), other.getY() - centre.y())
                        <= other.getRangeReal())
                .mapToDouble(this::supportGain)
                .max().orElse(0);
    }

    private double supportGain(Tower tower) {
        return this.supportGain(tower.getType(), ownedIds(tower.upgrades()));
    }

    private double supportGain(TowerFactory.Type type, List<String> owned) {
        return SUPPORT_TRUST * this.lab.supportGain(type, owned);
    }

    private static List<String> ownedIds(UpgradeState owned) {
        return owned.owned().stream().map(UpgradeNode::id).toList();
    }

    /**
     * What a {@code type} on {@code cell} adds to the damage towers within {@code reach} of it when what it gives
     * them goes from {@code before} to {@code after}: what a slow, a mark or a debuff is worth to the army. A
     * tower already given as much by another of the type gains nothing.
     */
    private Damage supportDamage(Turn turn, TowerFactory.Type type, Point cell, double reach, Gain gain,
                                 List<String> kinds) {
        Damage total = Damage.none(turn.survey().pathCount(), kinds.size());
        if (gain.after() == gain.before()) {
            return total;
        }
        Vec2 centre = turn.survey().centre(cell);
        for (Tower tower : turn.towers()) {
            Point at = turn.cellOf(tower);
            if (tower.getType() != TowerFactory.Type.AURA && !at.equals(cell)
                    && Math.hypot(tower.getX() - centre.x(), tower.getY() - centre.y()) <= reach) {
                double given = this.strongestSupport(turn, type, at, cell);
                double added = Math.max(0, gain.after() - given) - Math.max(0, gain.before() - given);
                total = total.plus(this.plainDamage(turn, tower.getType(), at, kinds)
                        .times(this.upgradeFactor(tower, tower.upgrades()) * added));
            }
        }
        return total;
    }

    /** What a support tower gives the towers beside it before and after a purchase. */
    private record Gain(double before, double after) {
    }

    /** What a Beacon on {@code cell} adds: its gain on every damage tower in its reach. */
    private Damage beaconDamage(Turn turn, Point cell, List<String> kinds, double factor) {
        Vec2 centre = turn.survey().centre(cell);
        double reach = turn.survey().reach(TowerFactory.Type.AURA);
        Damage total = Damage.none(turn.survey().pathCount(), kinds.size());
        for (Tower tower : turn.towers()) {
            if (tower.getType() != TowerFactory.Type.AURA
                    && Math.hypot(tower.getX() - centre.x(), tower.getY() - centre.y()) <= reach) {
                total = total.plus(this.plainDamage(turn, tower.getType(), turn.cellOf(tower), kinds)
                        .times(this.upgradeFactor(tower, tower.upgrades()) * this.lab.beaconGain(tower.getType())
                                * factor));
            }
        }
        return total;
    }

    /** Every upgrade line {@code tower} could buy now or as soon as it has the credits, whole. */
    private List<Option> upgradeOptions(Turn turn, Tower tower, List<String> kinds) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        List<UpgradeNode> buyableNow = turn.buyable(tower);
        if (buyableNow.isEmpty()) {
            return List.of();
        }
        Damage now = this.towerDamage(turn, tower, kinds);
        List<Option> options = new ArrayList<>();
        for (UpgradeNode target : tree.nodes()) {
            if (owned.owns(target.id())) {
                continue;
            }
            List<UpgradeNode> line = tree.pathTo(target).stream().filter(step -> !owned.owns(step.id())).toList();
            if (line.isEmpty() || !buyableNow.contains(line.getFirst())
                    || !line.stream().allMatch(step -> turn.gateMet(tower, step) && !spent(tree, step, owned)
                            && !this.style.bannedNodes().contains(step.id()))) {
                continue;
            }
            double factor = line.stream().mapToDouble(step -> this.nodeFactor(tower.getType(), step.id()))
                    .reduce(1.0, (a, b) -> a * b);
            int cost = line.stream().mapToInt(UpgradeNode::price).sum();
            Damage added = now.times(factor - 1);
            if (tower.getType() != TowerFactory.Type.AURA) {
                List<String> after = new ArrayList<>(ownedIds(owned));
                line.forEach(step -> after.add(step.id()));
                Gain gain = new Gain(this.supportGain(tower), this.supportGain(tower.getType(), after));
                added = added.plus(this.supportDamage(turn, tower.getType(), turn.cellOf(tower), tower.getRangeReal(),
                        gain, kinds)).plus(this.revealGain(turn, kinds, tower, after));
            }
            options.add(new Option(cost, added, () -> {
                for (UpgradeNode step : line) {
                    if (!turn.upgrade(tower, step)) {
                        return;
                    }
                }
            }));
        }
        return options;
    }

    /**
     * For each path and kind, the most any tower reaching the path shows of the kind, {@code changed} counted as
     * owning {@code changedOwned}.
     */
    private double[][] seen(Turn turn, List<String> kinds, Optional<Tower> changed, List<String> changedOwned) {
        double[][] shown = new double[turn.survey().pathCount()][kinds.size()];
        for (Tower tower : turn.towers()) {
            List<String> owned = changed.filter(tower::equals).isPresent() ? changedOwned : ownedIds(tower.upgrades());
            Point cell = turn.cellOf(tower);
            for (int k = 0; k < kinds.size(); k++) {
                double share = this.lab.revealShare(tower.getType(), owned, kinds.get(k));
                for (int p = 0; share > 0 && p < shown.length; p++) {
                    if (turn.survey().pathWithin(cell, tower.getRangeReal(), p) > 0) {
                        shown[p][k] = Math.max(shown[p][k], share);
                    }
                }
            }
        }
        return shown;
    }

    /** What the army gains on hiding kinds once {@code tower} owns {@code after} and shows more of them. */
    private Damage revealGain(Turn turn, List<String> kinds, Tower tower, List<String> after) {
        double[][] before = this.seen;
        double[][] widened = this.seen(turn, kinds, Optional.of(tower), after);
        if (Arrays.deepEquals(before, widened)) {
            return Damage.none(turn.survey().pathCount(), kinds.size());
        }
        Damage now = this.armyDamage(turn, kinds);
        this.seen = widened;
        Damage then = this.armyDamage(turn, kinds);
        this.seen = before;
        return then.plus(now.times(-1));
    }

    /** Whether owning {@code step} is already ruled out by a choice the tower made. */
    private static boolean spent(UpgradeTree tree, UpgradeNode step, UpgradeState owned) {
        return tree.choiceOf(step).map(choice -> picksUsed(choice, owned) >= choice.picks()).orElse(false);
    }

    private static long picksUsed(ExclusiveChoice choice, UpgradeState owned) {
        return choice.nodeIds().stream().filter(owned::owns).count();
    }
}
