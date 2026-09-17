package td.tower;

import td.tower.buff.TowerBuff;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Aura tower" - passive. Never attacks; instead it contributes a {@link TowerBuff} to every
 * non-aura tower whose centre falls within its range, and several stack additively. It
 * listens for towers being built and removed so a tower placed after it still picks the buff
 * up, and it unregisters its clients in {@link #doCleanup()} so selling it takes the buff away.
 */
public final class TowerAura extends AbstractTower implements TowerListener {

    public static final int PRICE = 20;
    public static final int DAMAGE = 0;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    // A LinkedHashSet, not a List: every path here asks "is this tower already a client",
    // and scanTowers asks it once per tower on the board on every towerBuild notification -
    // quadratic with a List. Insertion-ordered so doCleanup and the info string stay stable.
    private final Set<Tower> clients;
    private final float power;

    public TowerAura(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /**
     * Lets an aura tower contribute a buff stronger or weaker than the default, so two
     * aura towers can stack unequal amounts via TowerBuff's additive combine.
     */
    public TowerAura(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.aura, PRICE, DAMAGE, RANGE, 0, context, x, y);
        this.power = power;
        this.clients = new LinkedHashSet<>();

        this.context.towers().addListener(this);
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
        for (Tower t : this.context.towers().all()) {
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
        this.clients.add(t);
    }

    public void removeClient(Tower t) {
        this.clients.remove(t);
    }

    public void doCleanup() {
        super.doCleanup();
        // Over a copy: unregisterTower calls back into removeClient, so iterating the live
        // set would be a concurrent modification.
        for (Tower t : new ArrayList<>(this.clients)) {
            t.unregisterTower(this);
        }
        this.context.towers().removeListener(this);
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
