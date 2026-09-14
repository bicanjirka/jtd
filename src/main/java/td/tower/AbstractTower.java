package td.tower;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything every tower shares: board position, price, base and buffed damage/range, the
 * list of upgrade towers buffing it, and the damage/kill accounting. Subclasses supply only
 * a targeting strategy and a {@code doTick}.
 * <p>
 * {@code rangeReal2} is the squared range, and every range check compares squared distances -
 * a per-tick scan has no business calling {@code Math.sqrt}.
 */
public abstract class AbstractTower implements Tower {

    /**
     * Simulation ticks per second at {@code TickSpeed.NORMAL}, i.e. the reciprocal of
     * {@code GameLoop.BASE_TICK_NANOS}. Towers express their cadence in seconds for the
     * player's benefit, and this is what converts it.
     */
    protected static final float TICKS_PER_SECOND = 20f;

    protected GameWorld context;
    protected final List<TowerAura> upgTowers;
    protected int boardX;
    protected int boardY;
    protected int centerX;
    protected int centerY;
    protected float rangeBase;
    protected int damageBase;
    protected int damageCurrent;
    protected int coolDownMax;
    protected int coolDownCurrent;
    protected float rangeReal = 0;
    protected float rangeReal2 = 0;
    protected boolean passive = false;
    protected boolean selected = false;
    protected long damageDealt = 0;
    protected int killCount = 0;
    protected UpgradePath chosenPath;
    private final TowerFactory.type type;
    private float rangeCurrent;
    private final int price;


    public AbstractTower(TowerFactory.type t, int price, int damage, float range) {
        this.price = price;
        this.type = t;
        this.damageBase = this.damageCurrent = damage;
        this.rangeBase = this.rangeCurrent = range;
        this.upgTowers = new ArrayList<>();
    }

    /**
     * Binds this tower to a world and converts its cell coordinates {@code (x, y)} into the
     * pixel centre and pixel range everything else works in. Must be the last thing a leaf
     * constructor does: anything derived from the board scale has to be set before it, and
     * anything reading {@code centerX}/{@code centerY} (a proximity scan, a listener
     * registration) has to run after it.
     */
    protected void doInit(GameWorld context, int x, int y) {
        this.context = context;
        int scale = this.context.getBoard().scale();
        this.boardX = x * scale;
        this.boardY = y * scale;
        this.centerX = this.boardX + scale / 2;
        this.centerY = this.boardY + scale / 2;
        this.rangeReal = this.rangeBase * scale;
        this.rangeReal2 = rangeReal * rangeReal;
    }

    public float getRange() {
        return this.rangeBase;
    }

    public float getRangeReal() {
        return this.rangeReal;
    }

    /** Three quarters of what was paid - selling is always a loss, buffs bought since don't raise it. */
    public int getSellPrice() {
        return (int) Math.round(0.75 * this.price);
    }

    /**
     * Recomputes damage, range and fire rate from the current set of nearby Aura towers
     * <em>and</em> this tower's own chosen upgrade path (if any), folded through
     * {@link TowerBuff}'s additive algebra so bonuses of unequal strength stack correctly -
     * a specialization composes with an Aura tower's buff for free. Must be called on every
     * change to either: {@link #registerTower}/{@link #unregisterTower} already do for the
     * former, {@link #chooseUpgradePath} for the latter.
     */
    protected void calcDamageRange() {
        TowerBuff externalBuff = this.upgTowers.stream()
                .map(TowerAura::buff)
                .reduce(TowerBuff.none(), TowerBuff::combine);
        TowerBuff pathBuff = this.chosenPath == null ? TowerBuff.none() : this.chosenPath.statBonus();
        TowerBuff totalBuff = externalBuff.combine(pathBuff);
        this.damageCurrent = totalBuff.damageFor(this.damageBase);
        this.rangeCurrent = totalBuff.rangeFor(this.rangeBase);
        this.coolDownCurrent = totalBuff.fireRateFor(this.coolDownMax);

        this.rangeReal = this.rangeCurrent * this.context.getBoard().scale();
        this.rangeReal2 = rangeReal * rangeReal;
    }

    /**
     * Routes every hit a tower lands through one place so damageDealt/killCount stay accurate
     * regardless of which subclass fires: a shot into an enemy another tower already killed
     * this tick is a no-op in EnemyMob.doDamage() and must not be counted as a kill twice.
     * <p>
     * {@code damageDealt} accumulates what {@code doDamage} reports actually landed, not the
     * {@code damage} argument: a mob that resists part of a hit (see {@code EnemyMobSquare})
     * takes less than was fired at it, and a tower claiming the full amount would over-report
     * against exactly the enemies it performs worst on.
     * <p>
     * A kill that lands here tops up credits by {@code chosenPath}'s {@code bountyBonus}
     * (if any) on top of the flat {@code EconomyDelta.kill} bounty {@code enemy.doDamage}
     * already granted - extra <em>credits</em> only, no extra score, so a tower's bounty
     * specialization is a cash bonus rather than a scoring one.
     */
    protected void dealDamage(EnemyMob enemy, Damage damage) {
        boolean wasAlive = !enemy.isDead();
        Damage landed = enemy.doDamage(damage);
        if (wasAlive) {
            this.damageDealt += landed.amount();
            if (enemy.isDead()) {
                this.killCount++;
                if (this.chosenPath != null && this.chosenPath.statBonus().bountyBonus() != 0f) {
                    int bonus = Math.round(enemy.getBounty() * this.chosenPath.statBonus().bountyBonus());
                    this.context.apply(EconomyDelta.credits(bonus));
                }
            }
        }
    }

    public long getDamageDealt() {
        return this.damageDealt;
    }

    public int getKillCount() {
        return this.killCount;
    }

    /** No paths by default - only the four attack towers override this with real content. */
    public List<UpgradePath> availablePaths() {
        return List.of();
    }

    public UpgradePath getChosenPath() {
        return this.chosenPath;
    }

    public boolean chooseUpgradePath(UpgradePath path) {
        if (this.chosenPath != null || !this.availablePaths().contains(path)) {
            return false;
        }
        if (!path.condition().isSatisfied(this, this.context)) {
            return false;
        }
        if (!this.context.doPay(path.price())) {
            return false;
        }
        this.chosenPath = path;
        this.onUpgradePathChosen(path);
        this.calcDamageRange();
        return true;
    }

    /**
     * Hook for a leaf tower whose chosen path bumps a stat {@link TowerBuff} can't express
     * (e.g. {@code TowerTwo}'s splash radius, {@code TowerThree}'s sweep speed) - a no-op by
     * default. Called once, right when {@link #chooseUpgradePath} commits the choice.
     */
    protected void onUpgradePathChosen(UpgradePath path) {
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
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

    public TowerFactory.type getType() {
        return this.type;
    }

    /**
     * The line describing how often this tower attacks at the given cooldown - callers pass
     * {@code coolDownMax} for the pre-purchase base rate and {@code coolDownCurrent} for the
     * live, possibly-buffed one. Overridden by a tower whose cadence is not a cooldown at all
     * - see {@link TowerThree}, which sweeps continuously and has a rotation speed rather
     * than a fire rate, and so ignores the argument.
     */
    protected String rateLine(int coolDown) {
        return "Fire rate: " + TICKS_PER_SECOND / (coolDown + 1) + "/s\n";
    }

    public String getInfoString() {
        String s = "Price: " + this.price + "\n" +
                "Range: " + this.rangeBase + "\n";
        if (this.passive) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageBase / 100f + "\n" +
                    this.rateLine(this.coolDownMax) + "\n";
        }
        return s;
    }

    public String getStatusString() {
        String s = "Range: " + this.rangeCurrent + "\n";
        if (this.passive) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageCurrent / 100f + "\n" +
                    this.rateLine(this.coolDownCurrent) +
                    "Kills: " + this.killCount + "\n" +
                    "Damage dealt: " + this.damageDealt / 100f + "\n\n";
        }
        if (this.chosenPath != null) {
            s += "Specialized: " + this.chosenPath.displayName() + "\n";
        }
        return s;
    }

    public void registerTower(Tower t) {
        if (t != this) {
            switch (t.getType()) {
                case aura -> {
                    if (this.type != TowerFactory.type.aura && !this.upgTowers.contains(t)) {
                        TowerAura tupg = (TowerAura) t;
                        this.upgTowers.add(tupg);
                        tupg.addClient(this);
                        this.calcDamageRange();
                    }
                }
                default -> {
                }
            }
        }
    }

    public void unregisterTower(Tower t) {
        switch (t.getType()) {
            case aura -> {
                TowerAura tupg = (TowerAura) t;
                this.upgTowers.remove(t);
                tupg.removeClient(this);
                this.calcDamageRange();
            }
            default -> {
            }
        }
    }

    public void doCleanup() {
        for (int i = this.upgTowers.size() - 1; i >= 0; i--) {
            TowerAura tupg = this.upgTowers.remove(i);
            tupg.removeClient(this);
        }
    }
}
