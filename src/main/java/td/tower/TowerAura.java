package td.tower;

import td.tower.buff.TowerBuff;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * "Aura tower" - passive. Never attacks; instead it contributes a {@link TowerBuff} to every
 * non-aura tower whose centre falls within its range, and several stack additively. It
 * listens for towers being built and removed so a tower placed after it still picks the buff
 * up, and it unregisters its clients in {@link #doCleanup()} so selling it takes the buff away.
 */
public final class TowerAura extends AbstractTower implements TowerListener {

    public static final int price = 20;
    public static final int damage = 0;
    public static final float range = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    private final List<Tower> clients;
    private final float power;

    public TowerAura(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /**
     * Lets an aura tower contribute a buff stronger or weaker than the default, so two
     * aura towers can stack unequal amounts via TowerBuff's additive combine.
     */
    public TowerAura(GameWorld context, int x, int y, float power) {
        super(TowerFactory.type.aura, price, damage, range, 0, context, x, y);
        this.power = power;
        this.clients = new ArrayList<>();

        this.context.addTowerListener(this);
        this.scanTowers();
    }

    @Override
    protected boolean isPassive() {
        return true;
    }

    public TowerBuff buff() {
        return TowerBuff.amplifying(this.power);
    }

    private void scanTowers() {
        int dx, dy;
        for (Tower t : this.context.getTowers()) {
            if (!this.clients.contains(t)) {
                switch (t.getType()) {
                    case aura -> {
                    }
                    default -> {
                        dx = this.centerX - t.getX();
                        dy = this.centerY - t.getY();
                        if ((dx * dx + dy * dy) < this.rangeReal2()) {
                            t.registerTower(this);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void recalculateStats() {
        this.scanTowers();
        super.recalculateStats();
    }

    public void doTick(int gameTime) {
    }

    public void towerBuild(Tower t) {
        if (t != this && !this.clients.contains(t)) {
            switch (t.getType()) {
                case aura -> {
                }
                default -> {
                    int dx = this.centerX - t.getX();
                    int dy = this.centerY - t.getY();
                    if ((dx * dx + dy * dy) < this.rangeReal2()) {
                        t.registerTower(this);
                    }
                }
            }
        }
    }

    public void towerRemoved(Tower t) {
        if (this.clients.contains(t)) {
            t.unregisterTower(this);
        }
    }

    public void addClient(Tower t) {
        if (!this.clients.contains(t)) {
            this.clients.add(t);
        }
    }

    public void removeClient(Tower t) {
        this.clients.remove(t);
    }

    public void doCleanup() {
        super.doCleanup();
        for (int i = this.clients.size() - 1; i >= 0; i--) {
            Tower t = this.clients.get(i);
            t.unregisterTower(this);
        }
        this.context.removeTowerListener(this);
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerAura(this);
    }

    public String getInfoString() {
        return "Aura tower\n\n" +
                super.getInfoString() +
                "Increases damage and range of nearby towers by " + (this.power * 100) + "%";
    }

    public String getStatusString() {
        return "Aura tower\n\n" +
                super.getStatusString() +
                "Increases damage and range of nearby towers by " + (this.power * 100) + "%\n\n" +
                "Affects towers: " + this.clients.size();
    }
}
