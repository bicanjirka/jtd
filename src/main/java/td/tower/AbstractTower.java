package td.tower;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;
import java.util.Optional;

/**
 * Everything every tower shares: board position, price, base and buffed damage/range/fire
 * rate, the list of Aura towers buffing it, this tower's own chosen upgrade path (if any),
 * and the damage/kill accounting. Subclasses supply only a targeting strategy and a
 * {@code doTick}.
 * <p>
 * Everything derived from the board - the pixel centre, the pixel range - is computed in this
 * constructor and is {@code final}. A leaf therefore cannot read a half-built tower, and the
 * ordering rule that used to be documented prose ("{@code doInit} must be the last thing a
 * leaf constructor does") is now enforced by the compiler instead.
 * <p>
 * {@link #rangeReal2()} is the squared range, and every range check compares squared
 * distances - a per-tick scan has no business calling {@code Math.sqrt}.
 */
public abstract class AbstractTower implements Tower {

    /**
     * Simulation ticks per second at {@code TickSpeed.NORMAL}. Derived from the one place the
     * tick rate is defined, so a change to the loop's timestep reaches every tower's displayed
     * fire rate instead of leaving it quietly wrong.
     */
    protected static final float TICKS_PER_SECOND = TickRate.TICKS_PER_SECOND;
    /**
     * A burning target is this much more likely to take a critical hit - universal, applied to
     * every tower's roll against every burning enemy, not a perk any one tower or path owns.
     * One constant rather than restated inline, the same discipline {@link Damage#CRITICAL_MULTIPLIER}
     * already follows.
     */
    private static final float BURN_CRIT_CHANCE_MULTIPLIER = 2f;

    protected final GameWorld context;
    protected final int boardX;
    protected final int boardY;
    protected final int centerX;
    protected final int centerY;
    protected final float rangeBase;
    protected final int damageBase;
    protected final int coolDownMax;
    protected final float critChanceBase;
    private final TowerFactory.Type type;
    private final int price;
    // Independent readouts rather than a correlated set: damageDealt/killCount are written by
    // tick code and read by the info panel on the EDT (damageDealt is a long, whose
    // non-volatile reads may tear), and the rest are single flags.
    protected volatile boolean selected = false;
    protected volatile long damageDealt = 0;
    protected volatile int killCount = 0;
    protected volatile Optional<UpgradePath> chosenPath = Optional.empty();
    private volatile TowerStats stats;
    private volatile boolean removed = false;

    /**
     * Binds this tower to a world and converts its cell coordinates into the pixel centre and
     * pixel range everything else works in. A leaf passes its own constants straight through in
     * {@code base}; a leaf with no cooldown (a continuous or swept weapon) passes {@code 0} for
     * {@code coolDownMax} and overrides {@link #rateLine(int)} to describe its cadence some
     * other way. {@code base.critChanceBase()} is {@code 0} for every leaf except
     * {@code SniperTower}, whose marksman aim starts with some crit chance of its own before
     * any upgrade path adds more.
     */
    protected AbstractTower(TowerFactory.Type t, int price, TowerBaseStats base,
                            GameWorld context, int cellX, int cellY) {
        this.price = price;
        this.type = t;
        this.damageBase = base.damage();
        this.rangeBase = base.range();
        this.coolDownMax = base.coolDownMax();
        this.critChanceBase = base.critChanceBase();
        this.context = context;
        int scale = context.getBoard().scale();
        this.boardX = cellX * scale;
        this.boardY = cellY * scale;
        this.centerX = this.boardX + scale / 2;
        this.centerY = this.boardY + scale / 2;
        this.stats = TowerStats.of(this.damageBase, this.rangeBase, this.coolDownMax,
                this.critChanceBase, TowerBuff.none(), scale);
    }

    /**
     * This tower's current buffed stats, as one coherent snapshot. Never null.
     */
    protected TowerStats stats() {
        return this.stats;
    }

    /**
     * Current damage per hit, in hundredths - shorthand for {@code stats().damage()}.
     */
    protected int damageCurrent() {
        return this.stats.damage();
    }

    /**
     * Current ticks between shots - shorthand for {@code stats().coolDown()}.
     */
    protected int coolDownCurrent() {
        return this.stats.coolDown();
    }

    /**
     * Current range in pixels - shorthand for {@code stats().rangeReal()}.
     */
    protected float rangeReal() {
        return this.stats.rangeReal();
    }

    /**
     * Current range in pixels, squared - shorthand for {@code stats().rangeReal2()}.
     */
    protected float rangeReal2() {
        return this.stats.rangeReal2();
    }

    /**
     * Current chance, in {@code [0, 1]}, that this tower's next hit rolls critical -
     * shorthand for {@code stats().critChance()}. This tower's own {@link #critChanceBase}
     * (0 for most towers) plus whatever an upgrade path has granted on top (see
     * {@code td.tower.buff.TowerBuff.critChanceBonus}).
     */
    protected float critChance() {
        return this.stats.critChance();
    }

    /**
     * Whether this tower never attacks and exists only to buff its neighbours. A fixed
     * property of the tower type rather than mutable state, so only {@code AuraTower}
     * overrides it.
     */
    protected boolean isPassive() {
        return false;
    }

    public float getRange() {
        return this.rangeBase;
    }

    public float getRangeReal() {
        return this.stats.rangeReal();
    }

    /**
     * Three quarters of what was paid - selling is always a loss, buffs bought since don't raise it.
     */
    public int getSellPrice() {
        return (int) Math.round(0.75 * this.price);
    }

    /**
     * Recomputes damage, range and fire rate from the Aura towers currently on the board
     * <em>and</em> this tower's own chosen upgrade path (if any), folded through
     * {@link TowerBuff}'s additive algebra so bonuses of unequal strength stack correctly -
     * a specialization composes with an Aura tower's buff for free.
     * <p>
     * The external buff is <em>computed</em> from the roster on each call rather than read
     * from a list this tower maintains. Asking every tower what it contributes
     * ({@link Tower#buffFor}) costs one pass over a board of tens of towers on a user action,
     * and in exchange there is no index to keep in agreement with anything: no client set on
     * the Aura side, no aura list on this side, and no way for the two to drift apart.
     * {@code TowerRoster} calls this on every tower when the set changes;
     * {@link #chooseUpgradePath} calls it when the path side changes.
     * <p>
     * Publishes the result as one new {@link TowerStats}, so tick code reading concurrently
     * sees either the whole old set or the whole new one.
     */
    public void recalculateStats() {
        this.publishStats(this.chosenPath);
    }

    /**
     * Recomputes and publishes {@link TowerStats} treating {@code path} as this tower's chosen
     * specialization. Separate from {@link #recalculateStats()} so that
     * {@link #chooseUpgradePath} can publish the new stats <em>before</em> it publishes the
     * path itself: a tick landing between the two writes then sees the upgraded stats without
     * the path, rather than the path without its stats - which would have paid the path's
     * bounty bonus on a kill dealt at un-upgraded damage.
     */
    private void publishStats(Optional<UpgradePath> path) {
        TowerBuff externalBuff = this.context.towers().all().stream()
                .map(t -> t.buffFor(this))
                .reduce(TowerBuff.none(), TowerBuff::combine);
        TowerBuff totalBuff = externalBuff.combine(
                path.map(UpgradePath::statBonus).orElseGet(TowerBuff::none));
        this.stats = TowerStats.of(this.damageBase, this.rangeBase, this.coolDownMax, this.critChanceBase,
                totalBuff, this.context.getBoard().scale());
    }

    /**
     * Routes every hit a tower lands through one place so damageDealt/killCount stay accurate
     * regardless of which subclass fires: a shot into an enemy another tower already killed
     * this tick is a no-op in EnemyMob.doDamage() and must not be counted as a kill twice.
     * <p>
     * Rolls this tower's crit chance first (see {@link #rollCritical}), so every subclass's
     * call site gets a critical hit for free the moment its stats carry one, with no per-leaf
     * change needed.
     * <p>
     * {@code damageDealt} accumulates what {@code doDamage} reports actually landed, not the
     * {@code damage} argument: a mob that resists part of a hit (see
     * {@code td.enemy.PercentResistTrait}) takes less than was fired at it, and a tower
     * claiming the full amount would over-report against exactly the enemies it performs
     * worst on.
     * <p>
     * A kill that lands here tops up credits by {@code chosenPath}'s {@code bountyBonus}
     * (if any) on top of the flat {@code EconomyDelta.kill} bounty {@code enemy.doDamage}
     * already granted - extra <em>credits</em> only, no extra score, so a tower's bounty
     * specialization is a cash bonus rather than a scoring one.
     * <p>
     * A no-op once this tower has been sold or cleared (see {@link #doCleanup}). A
     * damage-over-time effect this tower applied can still be ticking on an enemy several
     * ticks after the tower itself is gone - without this guard, its lingering burn would
     * keep inflating {@code damageDealt}/{@code killCount} and paying bounty bonuses on an
     * object the player has already been refunded for.
     */
    protected void dealDamage(EnemyMob enemy, Damage damage) {
        if (this.removed) {
            return;
        }
        boolean wasAlive = !enemy.isDead();
        Damage landed = enemy.doDamage(this.rollCritical(enemy, damage));
        if (wasAlive) {
            this.damageDealt += landed.amount();
            if (enemy.isDead()) {
                this.killCount++;
                float bountyBonus = this.chosenPath
                        .map(p -> p.statBonus().bountyBonus()).orElse(0f);
                if (bountyBonus != 0f) {
                    this.context.economy().apply(EconomyDelta.credits(
                            Math.round(enemy.getBounty() * bountyBonus)));
                }
            }
        }
    }

    /**
     * Rolls this tower's current {@link #critChance()} against {@code context.random()} (per
     * this project's "randomness is injected" rule - never {@code Math.random()}) and, on
     * success, returns {@code damage.asCritical()} - otherwise {@code damage} unchanged. A
     * tower with no crit chance (every tower not carrying an upgrade path that grants some)
     * takes the same branch it always has, at the cost of one comparison.
     * <p>
     * A burning {@code enemy} doubles the effective chance (clamped at 100%) before the roll -
     * universal, not owned by whichever tower happens to apply burn, since
     * {@code enemy.activeEffectKinds()} is a plain, public query any tower can already read
     * (the status-marker UI already does). A tower with no crit chance still rolls nothing
     * against a burning target; the doubling only ever helps a roll that was already possible.
     */
    private Damage rollCritical(EnemyMob enemy, Damage damage) {
        float chance = this.critChance();
        if (chance <= 0f) {
            return damage;
        }
        if (enemy.activeEffectKinds().contains(EffectKind.BURN)) {
            chance = Math.min(1f, chance * BURN_CRIT_CHANCE_MULTIPLIER);
        }
        if (this.context.random().nextDouble() < chance) {
            return damage.asCritical();
        }
        return damage;
    }

    public long getDamageDealt() {
        return this.damageDealt;
    }

    public int getKillCount() {
        return this.killCount;
    }

    /**
     * No paths by default - only the four attack towers override this with real content.
     */
    public List<UpgradePath> availablePaths() {
        return List.of();
    }

    public Optional<UpgradePath> getChosenPath() {
        return this.chosenPath;
    }

    public boolean chooseUpgradePath(UpgradePath path) {
        if (this.chosenPath.isPresent() || !this.availablePaths().contains(path)) {
            return false;
        }
        if (!path.condition().isSatisfied(this, this.context)) {
            return false;
        }
        if (!this.context.economy().doPay(path.price())) {
            return false;
        }
        this.onUpgradePathChosen(path);
        // Stats first, then the path - see publishStats for why the order matters.
        this.publishStats(Optional.of(path));
        this.chosenPath = Optional.of(path);
        return true;
    }

    /**
     * Hook for a leaf tower whose chosen path bumps a stat {@link TowerBuff} can't express
     * (e.g. {@code SplashTower}'s splash radius, {@code SonarTower}'s sweep speed) - a no-op by
     * default. Called once, right when {@link #chooseUpgradePath} commits the choice.
     */
    protected void onUpgradePathChosen(UpgradePath path) {
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
     * The line describing how often this tower attacks at the given cooldown - callers pass
     * {@code coolDownMax} for the pre-purchase base rate and {@code coolDownCurrent} for the
     * live, possibly-buffed one. Overridden by a tower whose cadence is not a cooldown at all
     * - see {@link SonarTower}, which sweeps continuously and has a rotation speed rather
     * than a fire rate, and so ignores the argument.
     */
    protected String rateLine(int coolDown) {
        return "Fire rate: " + TICKS_PER_SECOND / (coolDown + 1) + "/s\n";
    }

    public String getInfoString() {
        String s = "Price: " + this.price + "\n" +
                "Range: " + this.rangeBase + "\n";
        if (this.isPassive()) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageBase / 100f + "\n" +
                    this.rateLine(this.coolDownMax) + "\n";
        }
        return s + this.upgradePathsBlock();
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
        s += this.chosenPath.map(p -> "Specialized: " + p.displayName() + "\n").orElse("");
        return s + this.upgradePathsBlock();
    }

    /**
     * Describes every upgrade path this tower still offers, one per line - "" once this tower
     * has already chosen one (its {@code getStatusString()}'s own "Specialized: ..." line
     * already covers that, and {@code availablePaths()} keeps returning the full static list
     * regardless of what's been chosen) or for a tower with none (the empty list
     * {@code availablePaths()} defaults to). Shown in both {@link #getInfoString()} (so a
     * player can see what a tower will offer before ever buying it) and
     * {@link #getStatusString()}.
     */
    private String upgradePathsBlock() {
        if (this.chosenPath.isPresent()) {
            return "";
        }
        List<UpgradePath> paths = this.availablePaths();
        if (paths.isEmpty()) {
            return "";
        }
        StringBuilder s = new StringBuilder("\nUpgrade paths:\n");
        for (UpgradePath path : paths) {
            s.append("- ").append(path.describe()).append('\n');
        }
        return s.toString();
    }

    /**
     * Contributes nothing - only {@code AuraTower} overrides this.
     */
    public TowerBuff buffFor(Tower other) {
        return TowerBuff.none();
    }

    /**
     * Marks this tower gone, so a damage-over-time effect it applied stops crediting it once
     * the player has been refunded for it (see {@link #dealDamage}). There is no buff
     * bookkeeping left to undo here: the towers this one was buffing recompute from the roster
     * the moment {@code TowerRoster} removes it.
     */
    public void doCleanup() {
        this.removed = true;
    }
}
