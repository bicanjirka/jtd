package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
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

    protected GameWorld context;
    protected final List<TowerUpgrade> upgTowers;
    protected int boardX;
    protected int boardY;
    protected int centerX;
    protected int centerY;
    protected float rangeBase;
    protected int damageBase;
    protected int damageCurrent;
    protected int coolDownMax;
    protected float rangeReal = 0;
    protected float rangeReal2 = 0;
    protected boolean passive = false;
    protected boolean selected = false;
    protected long damageDealt = 0;
    protected int killCount = 0;
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
     * Recomputes damage and range from the current set of upgrade towers, folded through
     * {@link TowerBuff}'s additive algebra so upgrades of unequal strength stack correctly.
     * Must be called on every change to that set - {@link #registerTower}/
     * {@link #unregisterTower} already do.
     */
    protected void calcDamageRange() {
        TowerBuff buff = this.upgTowers.stream()
                .map(TowerUpgrade::buff)
                .reduce(TowerBuff.none(), TowerBuff::combine);
        this.damageCurrent = buff.damageFor(this.damageBase);
        this.rangeCurrent = buff.rangeFor(this.rangeBase);

        this.rangeReal = this.rangeCurrent * this.context.getBoard().scale();
        this.rangeReal2 = rangeReal * rangeReal;
    }

    /**
     * Routes every hit a tower lands through one place so damageDealt/killCount stay accurate
     * regardless of which subclass fires: a shot into an enemy another tower already killed
     * this tick is a no-op in EnemyMob.doDamage() and must not be counted as a kill twice.
     */
    protected void dealDamage(EnemyMob enemy, Damage damage) {
        boolean wasAlive = !enemy.isDead();
        enemy.doDamage(damage);
        if (wasAlive) {
            this.damageDealt += damage.amount();
            if (enemy.isDead()) {
                this.killCount++;
            }
        }
    }

    public long getDamageDealt() {
        return this.damageDealt;
    }

    public int getKillCount() {
        return this.killCount;
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

    public String getInfoString() {
        String s = "Price: " + this.price + "\n" +
                "Range: " + this.rangeBase + "\n";
        if (this.passive) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageBase / 100f + "\n" +
                    "Fire rate: " + 20f / (this.coolDownMax + 1) + "/s\n\n";
        }
        return s;
    }

    public String getStatusString() {
        String s = "Range: " + this.rangeCurrent + "\n";
        if (this.passive) {
            s += "\n";
        } else {
            s += "Damage: " + this.damageCurrent / 100f + "\n" +
                    "Fire rate: " + 20f / (this.coolDownMax + 1) + "/s\n" +
                    "Kills: " + this.killCount + "\n" +
                    "Damage dealt: " + this.damageDealt / 100f + "\n\n";
        }
        return s;
    }

    public void registerTower(Tower t) {
        if (t != this) {
            switch (t.getType()) {
                case upgrade -> {
                    if (this.type != TowerFactory.type.upgrade && !this.upgTowers.contains(t)) {
                        TowerUpgrade tupg = (TowerUpgrade) t;
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
            case upgrade -> {
                TowerUpgrade tupg = (TowerUpgrade) t;
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
            TowerUpgrade tupg = this.upgTowers.remove(i);
            tupg.removeClient(this);
        }
    }
}
