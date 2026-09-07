package td.tower;

import td.util.Context;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTower implements Tower {

    protected Context context;
    protected String name;
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

    protected void doInit(Context context, int x, int y) {
        this.context = context;
        int scale = this.context.scale;
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

    public int getSellPrice() {
        return (int) Math.round(0.75 * this.price);
    }

    protected void calcDamageRange() {
        float upg = (1f + TowerUpgrade.power * this.upgTowers.size());
        this.damageCurrent = (int) (this.damageBase * upg);
        this.rangeCurrent = this.rangeBase * upg;

        this.rangeReal = this.rangeCurrent * this.context.scale;
        this.rangeReal2 = rangeReal * rangeReal;
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

    public String getName() {
        return this.name;
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
                    "Fire rate: " + 20f / (this.coolDownMax + 1) + "/s\n\n";
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
