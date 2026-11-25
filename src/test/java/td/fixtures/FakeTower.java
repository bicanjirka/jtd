package td.fixtures;

import td.enemy.EnemyMob;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.AbstractTower;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.TowerBaseStats;
import td.tower.TowerFactory;
import td.tower.TowerStat;
import td.tower.TowerVisitor;
import td.tower.ability.AbilityUnits;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.proc.Proc;
import td.tower.upgrade.Mechanic;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** A tower with test-supplied upgrades, for testing the upgrade mechanism apart from real content. */
public final class FakeTower extends AbstractTower {

    private final GameWorld world;
    private final int cellX;
    private final int cellY;
    private final UpgradeTree tree;
    private final Optional<UpgradeState> ownedUpgrades;
    private final OwnedMechanics<Mechanic> mechanics;
    private StatModifiers<TowerStat> given = StatModifiers.none();
    private int recalculations;
    private List<Proc> baseProcs = List.of();
    private List<TermText> mechanicSentences = List.of();

    private FakeTower(GameWorld context, int x, int y, UpgradeTree tree, Optional<UpgradeState> ownedUpgrades,
                      MechanicCatalogue<Mechanic> mechanics) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(10, 3f, 20), context, x, y);
        this.world = context;
        this.cellX = x;
        this.cellY = y;
        this.tree = tree;
        this.ownedUpgrades = ownedUpgrades;
        this.mechanics = new OwnedMechanics<>(mechanics);
    }

    /** Buys from {@code tree} through the real upgrade mechanism. */
    public static FakeTower offering(GameWorld context, int x, int y, UpgradeTree tree) {
        return new FakeTower(context, x, y, tree, Optional.empty(), MechanicCatalogue.empty());
    }

    /** Buys from {@code tree}, each node bringing what {@code mechanics} says it brings. */
    public static FakeTower offering(GameWorld context, int x, int y, UpgradeTree tree,
                                     MechanicCatalogue<Mechanic> mechanics) {
        return new FakeTower(context, x, y, tree, Optional.empty(), mechanics);
    }

    /** Reports {@code upgrades} as owned without buying them, and offers none. */
    public static FakeTower owning(GameWorld context, int x, int y, UpgradeState upgrades) {
        return new FakeTower(context, x, y, UpgradeTree.none(), Optional.of(upgrades), MechanicCatalogue.empty());
    }

    @Override
    protected OwnedMechanics<Mechanic> ownedMechanics() {
        return this.mechanics;
    }

    /** Earns {@code units} of its currency, as its attack would. */
    public void earn(AbilityUnits units) {
        this.earnAbilities(units);
    }

    /** Tries every ability whose cost is in, as its tick would; {@code cast} says whether one found a target. */
    public void castReady(Predicate<Mechanic> cast) {
        this.castReadyAbilities(this.mechanics.all(), cast);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return this.tree;
    }

    @Override
    public UpgradeState upgrades() {
        return this.ownedUpgrades.orElseGet(super::upgrades);
    }

    /** This tower itself, now giving every other tower {@code buff}, as an Aura would. */
    public FakeTower giving(StatModifiers<TowerStat> buff) {
        this.given = buff;
        return this;
    }

    @Override
    public StatModifiers<TowerStat> buffFor(Tower other) {
        return other == this ? StatModifiers.none() : this.given;
    }

    @Override
    public boolean emitsBuffs() {
        return !this.given.isNone();
    }

    /** This tower, now with {@code procs} of its own, as if born with them. Set it before it attacks. */
    public FakeTower withBaseProcs(List<Proc> procs) {
        this.baseProcs = List.copyOf(procs);
        return this;
    }

    @Override
    protected List<Proc> baseProcs() {
        return this.baseProcs;
    }

    /** This tower, every node of which now ends its words with {@code sentences}, as a mechanic's would. */
    public FakeTower describing(List<TermText> sentences) {
        this.mechanicSentences = List.copyOf(sentences);
        return this;
    }

    @Override
    public NodeWords wordsOf(UpgradeNode node) {
        NodeWords words = super.wordsOf(node);
        List<TermText> sentences = new ArrayList<>(words.sentences());
        sentences.addAll(this.mechanicSentences);
        return NodeWords.of(words.name(), words.stats(), sentences);
    }

    /** How many times {@link #recalculateStats()} has run. */
    public int recalculations() {
        return this.recalculations;
    }

    @Override
    public void recalculateStats() {
        this.recalculations++;
        super.recalculateStats();
    }

    /** Sets the kill count directly, skipping real damage. */
    public void setKillCount(int killCount) {
        this.killCount = killCount;
    }

    /** Its focus now stands at {@code steps}, as its attacks would have built it. */
    public void focusOn(int steps) {
        this.holdFocus(steps);
    }

    /** What its hit bonuses add to a plain hit on {@code target} fired under {@code focus}. */
    public HitAdjustment hitOn(EnemyMob target, int focus) {
        return this.hitAdjustment(target, HitKind.DIRECT, 1f, this.stats(), focus, false);
    }

    /** Tells its procs it landed a plain hit on {@code target}, as an attack would. */
    public void reportHit(EnemyMob target) {
        this.report(target, HitKind.DIRECT, false, false);
    }

    @Override
    protected void act(int gameTime) {
    }

    /** Visited as a plain tower of the type it reports, standing in its cell. */
    @Override
    public <R> R accept(TowerVisitor<R> visitor) {
        return TowerFactory.createTower(this.getType(), this.world, this.cellX, this.cellY).accept(visitor);
    }
}
