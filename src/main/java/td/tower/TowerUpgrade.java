package td.tower;

import td.util.Context;
import td.util.TowerListener;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class TowerUpgrade extends AbstractTower implements TowerListener {

    public static final int price = 20;
    public static final int damage = 0;
    public static final float range = 1.5f;
    public static final float power = 0.2f;

    private final List<Tower> clients;

    public TowerUpgrade(Context context, int x, int y) {
        super(TowerFactory.type.upgrade, price, damage, range);
        this.name = "upg";
        this.lineColor = Color.WHITE;
        this.passive = true;
        this.clients = new ArrayList<>();
        this.doInit(context, x, y);

        this.context.addTowerListener(this);
        this.scanTowers();
    }

    private void scanTowers() {
        int dx, dy;
        for (Tower t : this.context.towers) {
            if (!this.clients.contains(t)) {
                switch (t.getType()) {
                    case upgrade -> {
                    }
                    default -> {
                        dx = this.centerX - t.getX();
                        dy = this.centerY - t.getY();
                        if ((dx * dx + dy * dy) < this.rangeReal2) {
                            t.registerTower(this);
                        }
                    }
                }
            }
        }
    }

    protected void calcDamageRange() {
        this.scanTowers();
        super.calcDamageRange();
    }

    public void doTick(int gameTime) {
    }

    public void towerBuild(Tower t) {
        if (t != this && !this.clients.contains(t)) {
            switch (t.getType()) {
                case upgrade -> {
                }
                default -> {
                    int dx = this.centerX - t.getX();
                    int dy = this.centerY - t.getY();
                    if ((dx * dx + dy * dy) < this.rangeReal2) {
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

    public void paintEffect(Graphics2D g2, int gameTime) {
    }

    public String getInfoString() {
        return "Power tower\n\n" +
                super.getInfoString() +
                "Increases damage and range of nearby towers by " + (TowerUpgrade.power * 100) + "%";
    }

    public String getStatusString() {
        return "Power tower\n\n" +
                super.getStatusString() +
                "Increases damage and range of nearby towers by " + (TowerUpgrade.power * 100) + "%\n\n" +
                "Affects towers: " + this.clients.size();
    }
}
