package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.economy.EconomyDelta;
import td.enemy.HitReceiver;
import td.stat.DisruptionPenalty;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;

/**
 * What every tower shares: position, price, current stats, upgrades, and damage and kill
 * accounting. Subclasses supply targeting and {@code doTick}.
 * <p>
 * Everything derived from the board is computed in the constructor and {@code final}. Range checks
 * compare squared distances ({@link #rangeReal2()}) to avoid a square root per scan.
 */
public abstract class AbstractTower implements Tower {

    /** Ticks per second at normal speed, from the one place the tick rate is defined. */
    protected static final float TICKS_PER_SECOND = TickRate.TICKS_PER_SECOND;

    protected final GameWorld context;
    protected final int boardX;
    protected final int boardY;
    protected final int centerX;
    protected final int centerY;
    protected final float rangeBase;
    protected final int damageBase;
    protected final int coolDownMax;
    protected final float critChanceBase;
    private final TowerBaseStats baseStats;
    private final TowerFactory.Type type;
    private final int price;
    // damageDealt is a long read on the EDT; a non-volatile long read may tear.
    protected volatile boolean selected = false;
    protected volatile long damageDealt = 0;
    protected volatile int killCount = 0;
    private volatile UpgradeState upgrades = UpgradeState.none();
    private volatile TowerStats stats;
    private volatile boolean removed = false;
    // Sampled by the tick thread; also read when the EDT republishes stats after a purchase.
    private volatile DisruptionPenalty disruption = DisruptionPenalty.none();

    /**
     * Converts the cell coordinates to the pixel centre and range. A tower with no cooldown passes
     * {@code 0} and overrides {@link #rateLine(int)}.
     */
    protected AbstractTower(TowerFactory.Type t, int price, TowerBaseStats base,
                            GameWorld context, int cellX, int cellY) {
        this.price = price;
        this.type = t;
        this.damageBase = base.damage();
        this.rangeBase = base.range();
        this.coolDownMax = base.coolDownMax();
        this.critChanceBase = base.critChanceBase();
        this.baseStats = base;
        this.context = context;
        int scale = context.getBoard().scale();
        this.boardX = cellX * scale;
        this.boardY = cellY * scale;
        this.centerX = this.boardX + scale / 2;
        this.centerY = this.boardY + scale / 2;
        this.stats = TowerStats.of(base, TowerBuff.none(), DisruptionPenalty.none(), scale);
    }

    /** Current stats as one snapshot. Never null. */
    protected TowerStats stats() {
        return this.stats;
    }

    /** Damage per hit, in hundredths. */
    protected int damageCurrent() {
        return this.stats.damage();
    }

    /** Ticks between shots. */
    protected int coolDownCurrent() {
        return this.stats.coolDown();
    }

    /** Range in pixels. */
    protected float rangeReal() {
        return this.stats.rangeReal();
    }

    /** Range in pixels, squared. */
    protected float rangeReal2() {
        return this.stats.rangeReal2();
    }

    /** Chance in {@code [0, 1]} that the next hit is critical. */
    protected float critChance() {
        return this.stats.critChance();
    }

    /** Whether this tower never attacks and only buffs its neighbours. */
    protected boolean isPassive() {
        return false;
    }

    public float getRange() {
        return this.rangeBase;
    }

    public float getRangeReal() {
        return this.stats.rangeReal();
    }

    /** Three quarters of the price paid; upgrades bought since don't raise it. */
    public int getSellPrice() {
        return (int) Math.round(0.75 * this.price);
    }

    /**
     * Recomputes stats from the buffs other towers contribute and this tower's own upgrades, and
     * publishes them as one {@link TowerStats} snapshot.
     * <p>
     * Buffs are asked of every tower on each call instead of being tracked, so there is no index
     * that can drift out of sync. Called on every tower whenever the tower set or any tower's
     * upgrades change.
     */
    public void recalculateStats() {
        this.publishStats(this.upgrades);
    }

    /**
     * Publishes stats for {@code upgrades}. Separate so that buying publishes the new stats before
     * the new upgrade state: a tick between the two writes then never pays a node's bounty bonus on
     * a kill made at the old damage.
     */
    private void publishStats(UpgradeState upgrades) {
        TowerBuff externalBuff = this.context.towers().all().stream()
                .map(t -> t.buffFor(this))
                .reduce(TowerBuff.none(), TowerBuff::combine);
        TowerBuff totalBuff = externalBuff.combine(upgrades.totalBuff());
        this.stats = TowerStats.of(this.baseStats, totalBuff, this.disruption, this.context.getBoard().scale());
    }

    /**
     * Every hit goes through here, so accounting is right whichever subclass fires. The hit carries
     * this tower's current {@link AttackProfile}; the target rolls the crit.
     * <p>
     * Counts the damage that actually landed, not the damage fired. A kill adds the upgrades'
     * bounty bonus in credits, not score. A hit on a mob already killed this tick is not a second
     * kill. A no-op once the tower is sold, so a lingering burn stops crediting it.
     *
     * @return whether the hit landed as a critical hit
     */
    protected boolean dealDamage(HitReceiver enemy, Damage damage) {
        if (this.removed) {
            return false;
        }
        boolean wasAlive = !enemy.isDead();
        Damage landed = enemy.doDamage(damage, this.stats.attack());
        if (wasAlive) {
            this.damageDealt += landed.amount();
            if (enemy.isDead()) {
                this.killCount++;
                float bountyBonus = this.upgrades.totalBuff().bountyBonus();
                if (bountyBonus != 0f) {
                    this.context.economy().apply(EconomyDelta.credits(
                            Math.round(enemy.getBounty() * bountyBonus)));
                }
            }
        }
        return landed.critical();
    }

    /**
     * Compared against the published stats rather than the last sample, so a republish from the EDT
     * racing this one is corrected on the next tick.
     */
    public void refreshDisruption() {
        DisruptionPenalty sampled = this.context.disruptions().penaltyAt(this.centerX, this.centerY);
        this.disruption = sampled;
        if (!sampled.equals(this.stats.disruption())) {
            this.publishStats(this.upgrades);
        }
    }

    public boolean isDisrupted() {
        return !this.stats.disruption().isNone();
    }

    public long getDamageDealt() {
        return this.damageDealt;
    }

    public int getKillCount() {
        return this.killCount;
    }

    /** No upgrades by default. */
    public UpgradeTree upgradeTree() {
        return UpgradeTree.none();
    }

    public UpgradeState upgrades() {
        return this.upgrades;
    }

    public boolean buyUpgrade(UpgradeNode node) {
        if (this.upgrades.owns(node.id()) || !this.upgradeTree().nodes().contains(node)) {
            return false;
        }
        if (!node.requires().isSatisfied(this, this.context) || !node.gate().isSatisfied(this, this.context)) {
            return false;
        }
        if (!this.context.economy().doPay(node.price())) {
            return false;
        }
        this.onUpgradeBought(node);
        UpgradeState nextState = this.upgrades.with(node);
        this.publishStats(nextState);
        this.upgrades = nextState;
        // This tower's contribution to its neighbours may have changed, so every tower recomputes.
        for (Tower t : this.context.towers().all()) {
            t.recalculateStats();
        }
        return true;
    }

    /**
     * Hook for an upgrade effect that no {@link TowerBuff} axis expresses; a no-op by default.
     * Compare {@code node} by equality, not reference.
     */
    protected void onUpgradeBought(UpgradeNode node) {
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getX() {
        return centerX;
    }

    public int getY() {
        return centerY;
    }

    public int getBoardX() {
        return this.boardX;
    }

    public int getBoardY() {
        return this.boardY;
    }

    public TowerFactory.Type getType() {
        return this.type;
    }

    /**
     * Describes how often the tower attacks at {@code coolDown}. A tower whose cadence is not a
     * cooldown overrides this and ignores the argument.
     */
    protected String rateLine(int coolDown) {
        return "Fire rate: " + TICKS_PER_SECOND / (coolDown + 1) + "/s\n";
    }

    /**
     * The pre-purchase text: base stats and price. Upgrades appear once the tower is built and
     * selected.
     */
    public String getInfoString() {
        String s = "Price: " + this.price + "\n" +
                "Range: " + this.rangeBase + "\n";
        if (this.isPassive()) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageBase / 100f + "\n" +
                    this.rateLine(this.coolDownMax) + "\n";
        }
        return s;
    }

    public String getStatusString() {
        String s = "Range: " + this.stats.range() + "\n";
        if (this.isPassive()) {
            s += "\n";
        } else {
            s += "Damage: " + this.stats.damage() / 100f + "\n" +
                    this.rateLine(this.stats.coolDown());
            if (this.stats.critChance() > 0f) {
                s += "Crit chance: " + Math.round(this.stats.critChance() * 100) + "%\n";
            }
            s += "Kills: " + this.killCount + "\n" +
                    "Damage dealt: " + this.damageDealt / 100f + "\n\n";
        }
        s += this.ownedNodesBlock();
        return s + this.upgradeNodesBlock();
    }

    /** One line per slot holding a bought node; empty if none. */
    private String ownedNodesBlock() {
        StringBuilder s = new StringBuilder();
        for (UpgradeSlot slot : UpgradeSlot.values()) {
            this.upgrades.tip(slot).ifPresent(node -> s.append(slot).append(": ").append(node.displayName()).append('\n'));
        }
        return s.toString();
    }

    /**
     * One line per offered node with a ✔/✘ and its gate progress; empty when nothing is offered.
     */
    private String upgradeNodesBlock() {
        List<UpgradeNode> offered = this.upgradeTree().offered(this, this.context);
        if (offered.isEmpty()) {
            return "";
        }
        StringBuilder s = new StringBuilder("\nUpgrades:\n");
        for (UpgradeNode node : offered) {
            boolean satisfied = node.gate().isSatisfied(this, this.context);
            String mark = satisfied ? "✔" : "✘";
            s.append("- ").append(mark).append(' ').append(node.displayName())
                    .append(" (").append(node.gate().progress(this, this.context)).append(")\n");
        }
        return s.toString();
    }

    /** Contributes nothing by default. */
    public TowerBuff buffFor(Tower other) {
        return TowerBuff.none();
    }

    /** Marks this tower gone, so effects it applied stop crediting it after the refund. */
    public void doCleanup() {
        this.removed = true;
    }
}
